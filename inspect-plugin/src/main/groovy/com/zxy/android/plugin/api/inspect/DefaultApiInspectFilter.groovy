package com.zxy.android.plugin.api.inspect

import com.android.SdkConstants
import javassist.CtClass
import javassist.CtMethod
import org.gradle.api.Project

/**
 * Created by zhengxiaoyong on 2018/12/24.
 */
class DefaultApiInspectFilter implements ApiInspectFilter {

    Project mProject

    ApiInspectExtension mApiInspectExtension

    DefaultApiInspectFilter(Project project) {
        this.mProject = project
        mApiInspectExtension = mProject.extensions.findByType(ApiInspectExtension.class)
    }

    @Override
    boolean filter(CtClass clazz) {
        boolean inspectSystemApi = mApiInspectExtension.inspectSystemApi
        return isSystemGenerateClass(clazz.getSimpleName()) || isJavaSystemClass(clazz.getName()) || (inspectSystemApi ? false : isAndroidSystemClass(clazz.getName()))
    }

    @Override
    boolean filter(String className) {
        boolean inspectSystemApi = mApiInspectExtension.inspectSystemApi
        int index = className.lastIndexOf('.')
        String simpleName = className
        if (index >= 0) {
            simpleName = className.substring(index + 1)
        }
        return isSystemGenerateClass(simpleName) || isJavaSystemClass(className) || (inspectSystemApi ? false : isAndroidSystemClass(className))
    }

    @Override
    boolean filter(CtMethod method) {
        return false
    }

    @Override
    boolean filterPackage(String packageName) {
        boolean inspectSystemApi = mApiInspectExtension.inspectSystemApi
        return isJavaSystemClass(packageName) || (inspectSystemApi ? false : isAndroidSystemClass(packageName))
    }

    boolean isSystemGenerateClass(String name) {
        if (!name.endsWith(SdkConstants.DOT_CLASS))
            name += SdkConstants.DOT_CLASS
        return name.startsWith('R$') || name.contentEquals('R.class') || name.contentEquals("BuildConfig.class")
    }

    boolean isJavaSystemClass(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("org.apache.")
    }

    boolean isAndroidSystemClass(String name) {
        return name.startsWith("android.") || name.startsWith("androidx.")
    }

}
