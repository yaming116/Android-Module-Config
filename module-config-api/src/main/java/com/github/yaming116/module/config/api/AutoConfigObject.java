package com.github.yaming116.module.config.api;

import android.app.Application;
import android.content.Context;

import com.github.yaming116.module.config.annotation.AutoConfig;

/**
 * Created by Sun on 2017/2/14.
 */

@AutoConfig
public abstract class AutoConfigObject {

    public abstract <T extends Application> void init(T appContext);

}
