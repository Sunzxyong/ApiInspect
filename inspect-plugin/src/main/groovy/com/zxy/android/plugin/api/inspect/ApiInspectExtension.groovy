package com.zxy.android.plugin.api.inspect

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

/**
 * Created by zhengxiaoyong on 2018/12/22.
 */
class ApiInspectExtension {

    boolean enable = true

    boolean inspectSystemApi = false

    ApiInspectIncludeExtension include

    ApiInspectExcludeExtension exclude

    ApiInspectExtension(Project project) {
        ObjectFactory objectFactory = project.getObjects()
        include = objectFactory.newInstance(ApiInspectIncludeExtension.class)
        exclude = objectFactory.newInstance(ApiInspectExcludeExtension.class)
    }

    void include(Action<ApiInspectIncludeExtension> action) {
        action.execute(include)
    }

    void exclude(Action<ApiInspectExcludeExtension> action) {
        action.execute(exclude)
    }

}
