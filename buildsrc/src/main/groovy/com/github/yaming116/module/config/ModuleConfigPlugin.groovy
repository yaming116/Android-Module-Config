package com.github.yaming116.module.config

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.api.LibraryVariantImpl
import com.neenbedankt.gradle.androidapt.AndroidAptPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension

class ModuleConfigPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (!(project.plugins.hasPlugin(AppPlugin) || project.plugins.hasPlugin(LibraryPlugin))) {
            throw new IllegalArgumentException(
                    'AutoConfig gradle plugin can only be applied to android projects.')
        }

        // Add dependencies
        Project router = project.rootProject.findProject("module-config-api")
        Project compiler = project.rootProject.findProject("module-config-compiler")

        def usesAptPlugin = project.plugins.findPlugin('com.neenbedankt.android-apt') != null
        def hasAnnotationProcessorConfiguration = project.getConfigurations().findByName('annotationProcessor') != null

        if (!hasAnnotationProcessorConfiguration) {
            if (!usesAptPlugin) {
                usesAptPlugin = true
                project.apply(AndroidAptPlugin)
            }
        }
        if (router && compiler) {
            project.dependencies {
                compile router
            }

            if (usesAptPlugin){
                project.dependencies {
                    apt compiler
                }
            }else {
                project.dependencies {
                    annotationProcessor compiler
                }
            }

        } else {
            String autoConfigVersion = "latest.integration"
            String compilerVersion = "latest.integration"
            ExtraPropertiesExtension ext = project.rootProject.ext
            if (ext.has("autoConfigVersion")) {
                autoConfigVersion = ext.get("autoConfigVersion")
            }
            if (ext.has("autoConfigCompilerVersion")) {
                compilerVersion = ext.get("autoConfigCompilerVersion")
            }
            project.dependencies.add("compile", "com.github.yaming116:module-config-api:${autoConfigVersion}")
            if (usesAptPlugin) {
                project.dependencies.add("apt", "com.github.yaming116:module-config-compiler:${compilerVersion}")
            }else {
                project.dependencies.add("annotationProcessor", "com.github.yaming116:module-config-compiler:${compilerVersion}")
            }

        }

        // Modify build config
        String validModuleName = project.name.replace('.', '_')
        project.afterEvaluate {
            if (project.plugins.hasPlugin(AppPlugin)) {
                ((AppExtension) project.android).applicationVariants.all { ApplicationVariantImpl variant ->
                    // What the f**k, the flowing line wasted me some days.
                    // Inspired by com.android.build.gradle.tasks.factory.JavaCompileConfigAction.
                    // F**king source code!
                    variant.variantData.javacTask.options.compilerArgs.add("-AmoduleName=${validModuleName}")

                    Set<Project> libs = project.rootProject.subprojects.findAll {
                        it.plugins.hasPlugin(LibraryPlugin) && it.plugins.hasPlugin(RouterPlugin)
                    }
                    StringBuilder sb = new StringBuilder();
                    if (!libs.empty) {
                        libs.each { Project p ->
                            sb.append(p.name.replace('.', '_')).append(",")
                        }
                    }
                    sb.append(validModuleName)
                    variant.buildConfigField("String", "AUTO_CONFIG_MODULES_NAME", "\"$sb\"")
                }
            } else {
                ((LibraryExtension) project.android).libraryVariants.all { LibraryVariantImpl variant ->
                    variant.variantData.javacTask.options.compilerArgs.add("-AmoduleName=${validModuleName}")
                }
            }
        }

    }
}