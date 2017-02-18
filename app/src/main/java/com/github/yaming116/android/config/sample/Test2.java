package com.github.yaming116.android.config.sample;

import android.app.Application;
import android.util.Log;

import com.github.yaming116.module.config.annotation.AutoConfig;
import com.github.yaming116.module.config.api.AutoConfigObject;

/**
 * Created by Sun on 2017/1/18.
 */
public class Test2 extends AutoConfigObject {
    @Override
    public <T extends Application> void init(T appContext) {
        Log.d("init", "Test2 is init");
    }
}
