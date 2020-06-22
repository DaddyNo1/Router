package com.daddyno1.router;

public class RouteMeta {
    public Class destination;
    public String path;
    public String group;

    public static RouteMeta build(Class destination, String path, String group) {
        RouteMeta routeMeta = new RouteMeta();
        routeMeta.destination = destination;
        routeMeta.path = path;
        routeMeta.group = group;
        return routeMeta;
    }
}
