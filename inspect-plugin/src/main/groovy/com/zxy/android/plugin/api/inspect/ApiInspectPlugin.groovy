package com.zxy.android.plugin.api.inspect

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by zhengxiaoyong on 2018/12/22.
 */
class ApiInspectPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def android = null
        if (project.plugins.hasPlugin("com.android.application")) {
            android = project.extensions.getByType(AppExtension)
        } else if (project.plugins.hasPlugin("com.android.library")) {
            android = project.extensions.getByType(LibraryExtension)
        }

        if (android == null)
            return
        project.extensions.create("apiInspect", ApiInspectExtension.class, project)
        android.registerTransform(new ApiInspectTransform(project))
    }
}
