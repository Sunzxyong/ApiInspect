package com.zxy.android.plugin.api.inspect

/**
 * Created by zhengxiaoyong on 2018/12/24.
 */
class IncompatibleClassInfo {

    String className

    String incompatibleClassName

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        IncompatibleClassInfo that = (IncompatibleClassInfo) o

        if (className != that.className) return false
        if (incompatibleClassName != that.incompatibleClassName) return false

        return true
    }

    int hashCode() {
        int result
        result = (className != null ? className.hashCode() : 0)
        result = 31 * result + (incompatibleClassName != null ? incompatibleClassName.hashCode() : 0)
        return result
    }
}
