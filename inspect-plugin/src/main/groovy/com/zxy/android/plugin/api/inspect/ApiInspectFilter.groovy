package com.zxy.android.plugin.api.inspect

import javassist.CtClass
import javassist.CtMethod

/**
 * Created by zhengxiaoyong on 2018/12/24.
 */
interface ApiInspectFilter {

    boolean filter(CtClass clazz)

    boolean filter(String className)

    boolean filter(CtMethod method)

}
