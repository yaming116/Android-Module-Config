package com.github.yaming116.android.config.sample;

import android.app.Application;
import android.util.Log;

import com.github.yaming116.module.config.api.AutoConfigObject;

/**
 * Created by Sun on 2017/1/18.
 */
public class Test extends AutoConfigObject{

    @Override
    public  void init(Application appContext) {
        Log.d("init", "Test is init");
    }
}
