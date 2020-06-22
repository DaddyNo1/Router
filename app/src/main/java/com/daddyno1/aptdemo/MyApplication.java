package com.daddyno1.aptdemo;

import android.app.Application;

import com.daddyno1.router.Router;
import com.daddyno1.router_annotation.Route;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Router.init(this);
    }
}
