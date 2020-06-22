package com.daddyno1.aptdemo;

import android.app.Application;

import com.daddyno1.router.Router;
import com.daddyno1.router_annotation.Route;

/**
 * <p>Description  : MyApplication</p>
 * <p>Author       : jxf</p>
 * <p>Date         : 2020/6/18</p>
 * <p>Time         : 9:50 PM</p>
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Router.init(this);
    }
}
