package com.daddyno1.router;

import android.accounts.AbstractAccountAuthenticator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;

import java.lang.reflect.InvocationTargetException;

public class Router {

    private static Context mContext;

    public static void init(Context context){
        mContext = context;
        LogisticCenter.init();
    }

    public synchronized static void navigation(String path){
        RouteMeta routeMeta = Warehouse.routes.get(path);
        if (routeMeta == null){
            String group = getGroupFromPath(path);
            Class<? extends IRouteGroup> clsRootGroup = Warehouse.groups.get(group);
            if (clsRootGroup == null){
                throw new RuntimeException("route : not find the path : [" + path + "] i");
            }else{
                try {
                    IRouteGroup routeGroup = clsRootGroup.getConstructor().newInstance();
                    routeGroup.loadInto(Warehouse.routes);
                    Warehouse.groups.remove(group);

                    navigation(path); //reload

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else{
            // found route
            if (mContext == null){
                throw new RuntimeException("Router must be init before use");
            }

            Intent intent = new Intent(mContext, routeMeta.destination);
            if (!(mContext instanceof Activity)){
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            ActivityCompat.startActivity(mContext, intent, null);
        }
    }

    //从 path 中获取 group信息
    private static String getGroupFromPath(String path) {
        String group;
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("route path : [" + path + "] is empty;");
        }
        if (!path.startsWith("/")) {
            throw new RuntimeException("route path : [" + path + "] is must start with /");
        }
        int index = path.indexOf("/", 1);
        if (index == -1) {
            throw new RuntimeException("route path : [" + path + "]  must contains at lease two /");
        }
        group = path.substring(1, path.indexOf("/", index));
        return group;
    }
}
