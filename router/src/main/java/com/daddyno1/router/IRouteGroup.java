package com.daddyno1.router;

import java.util.Map;

public interface IRouteGroup {
    void loadInto(Map<String, RouteMeta> map);
}
