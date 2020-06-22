package com.daddyno1.router;

import android.content.Context;
import android.text.TextUtils;

import java.lang.reflect.InvocationTargetException;

public class LogisticCenter {

    private static boolean hasInit = false;


    // init 仓库
    public static void init(){
        if (!hasInit){
            loadRouterMap();
            hasInit = true;
        }
    }

    /**
     * 这个方法会在编译时获取所有 module 的 IRouteRoot，然后初始化路由组
     */
    private static void loadRouterMap() {
        // 这里的代码会被 gradle-plugin 填充，生成的代码类似下面这种：
//        registerRouteRoots(register("com.route.Router$$Root$$app"));
//        registerRouteRoots(register("com.route.Router$$Root$$test"));

    }

    private static void registerRouteRoots(IRouteRoot routeRoot){
        if (routeRoot != null){
            routeRoot.loadInto(Warehouse.groups);
        }
    }

    public static IRouteRoot register(String className){
        if(!TextUtils.isEmpty(className)){
            try {
                Class cls = Class.forName(className);
                IRouteRoot routeRoot = (IRouteRoot) cls.getConstructor().newInstance();
                return routeRoot;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
