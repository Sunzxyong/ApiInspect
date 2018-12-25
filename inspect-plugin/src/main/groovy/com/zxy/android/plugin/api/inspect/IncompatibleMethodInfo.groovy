package com.zxy.android.plugin.api.inspect

/**
 * Created by zhengxiaoyong on 2018/12/24.
 */
class IncompatibleMethodInfo {

    String className

    String incompatibleClassName

    String methodName

    String signature

    String lineNumber

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        IncompatibleMethodInfo that = (IncompatibleMethodInfo) o

        if (className != that.className) return false
        if (incompatibleClassName != that.incompatibleClassName) return false
        if (lineNumber != that.lineNumber) return false
        if (methodName != that.methodName) return false
        if (signature != that.signature) return false

        return true
    }

    int hashCode() {
        int result
        result = (className != null ? className.hashCode() : 0)
        result = 31 * result + (incompatibleClassName != null ? incompatibleClassName.hashCode() : 0)
        result = 31 * result + (methodName != null ? methodName.hashCode() : 0)
        result = 31 * result + (signature != null ? signature.hashCode() : 0)
        result = 31 * result + (lineNumber != null ? lineNumber.hashCode() : 0)
        return result
    }
}
