package com.daddyno1.router;

import com.daddyno1.router_annotation.Route;

import java.util.HashMap;
import java.util.Map;

public class Warehouse {
    // Map<group, Class>
    public final static Map<String, Class<? extends IRouteGroup>> groups = new HashMap<>();
    // Map<path, RouteMeta>
    public final static Map<String, RouteMeta> routes = new HashMap<>();
}
