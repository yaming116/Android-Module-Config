package com.github.yaming116.module.config.api;

import android.app.Application;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sun on 2017/2/15.
 */
public abstract class AutoInitializer {
    private static final String TAG = "AutoInitializer";

    protected static final List<Class> autoConfig = new ArrayList();
    private static AutoInitializer autoConfigIml = null;

    static {
        try {
            Class clazz = Class.forName("com.github.yaming116.module.config.AutoInitializerIml");
            autoConfigIml = (AutoInitializer) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    protected void init(Application appContext) {

        for (Class clazz : autoConfig) {
            try {
                AutoConfigObject instance = (AutoConfigObject) clazz.newInstance();
                instance.init(appContext);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }


    public static void install(Application appContext){
        if (autoConfigIml != null){
            autoConfigIml.init(appContext);
        }else {
            Log.e(TAG, "autoConfigIml is null");
        }
    }
}
