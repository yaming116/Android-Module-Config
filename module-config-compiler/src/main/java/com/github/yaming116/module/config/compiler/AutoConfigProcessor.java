package com.github.yaming116.module.config.compiler;

import com.github.yaming116.module.config.annotation.AutoConfig;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;

import jdk.internal.dynalink.support.ClassMap;

/**
 * Created by Sun on 2017/1/18.
 */
@AutoService(Processor.class)
public class AutoConfigProcessor extends AbstractProcessor {
    private static final ClassName CONTEXT = ClassName.get("android.content", "Context");

    public static final String KEY_MODULE_NAME = "moduleName";

    static final String AUTO_CONFIG_OBJECT_TYPE = "com.github.yaming116.module.config.api.AutoConfigObject";
    static final String AUTO_CONFIG_TYPE = "com.github.yaming116.module.config.api.AutoInitializer";

    private Filer mFiler;
    private Elements elementUtils;
    private String moduleName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mFiler = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();

        // Attempt to get user configuration [moduleName]
        Map<String, String> options = processingEnv.getOptions();
        if (MapUtils.isNotEmpty(options)) {
            moduleName = options.get(KEY_MODULE_NAME);
        }

        if (StringUtils.isNotEmpty(moduleName)) {
            moduleName = moduleName.replaceAll("[^0-9a-zA-Z_]+", "");

            note("The user has configuration the module name, it was [" + moduleName + "]");
        } else {
            error("These no module name, at 'build.gradle', like :\n" +
                    "apt {\n" +
                    "    arguments {\n" +
                    "        moduleName project.getName();\n" +
                    "    }\n" +
                    "}\n");
            throw new RuntimeException("AutoConfig::Compiler >>> No module name, for more information, look at gradle log.");
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(AutoConfig.class.getCanonicalName());
    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        try {
            List<ClassName> classNames = findAndParseTargets(roundEnv);
            if (!classNames.isEmpty()){
                TypeSpec type = buildSource(classNames);

                if(type != null) {
                    JavaFile.builder("com.github.yaming116.module.config", type).build().writeTo(mFiler);
                }
            }

            note( "AutoConfig build success");
        }catch (Exception e) {
            error(e.getMessage());
        }

        return true;
    }

    private List<ClassName> findAndParseTargets(RoundEnvironment roundEnv){
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(AutoConfig.class);

        List<ClassName> clazz = new ArrayList<>();

        for (Element classElement : elements) {
            if (classElement.getKind() != ElementKind.CLASS) {
                error(classElement, "The AutoConfig annotation can only be applied to classes");
                return null;
            }
            TypeElement autoConfigType = ((TypeElement) classElement);
            if (!isSubtypeOfType(autoConfigType.getSuperclass(), AUTO_CONFIG_OBJECT_TYPE)) {
                error(classElement, "@%s must extends AutoConfigObject.", classElement.getSimpleName());
                return null;
            }
            clazz.add(ClassName.get(autoConfigType));
        }

        return clazz;
    }

    private TypeSpec buildSource(List<ClassName> clazz){

        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();

        for (ClassName name : clazz) {
            codeBlockBuilder.addStatement("autoConfig.add($T.class)", name);
        }

        return TypeSpec.classBuilder("AutoInitializerIml$$" + moduleName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.get(elementUtils.getTypeElement(AUTO_CONFIG_TYPE)))
                .addStaticBlock(codeBlockBuilder.build())
                .build();
    }

    static boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (isTypeEqual(typeMirror, otherType)) {
            return true;
        }
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTypeEqual(TypeMirror typeMirror, String otherType) {
        return otherType.equals(typeMirror.toString());
    }

    private void error(String message, Object... args) {
        printMessage(Kind.ERROR, null, message, args);
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Kind.ERROR, element, message, args);
    }

    private void note(String message, Object... args) {
        printMessage(Kind.NOTE, null, message, args);
    }

    private void note(Element element, String message, Object... args) {
        printMessage(Kind.NOTE, element, message, args);
    }

    private void printMessage(Kind kind, Element element, String message, Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        if (element == null){
            processingEnv.getMessager().printMessage(kind, message);
        }else {
            processingEnv.getMessager().printMessage(kind, message, element);
        }


    }
}
