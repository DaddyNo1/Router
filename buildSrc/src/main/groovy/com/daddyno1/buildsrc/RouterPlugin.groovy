
package com.daddyno1.buildsrc

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project


class RouterPlugin implements Plugin<Project>{
    @Override
    void apply(Project project) {
        println("======== RouterPlugin ========")

        def appExtension = project.extensions.findByType(AppExtension.class)
        // 创建Transform
        appExtension.registerTransform(new RouterTransform(project))

    }
}