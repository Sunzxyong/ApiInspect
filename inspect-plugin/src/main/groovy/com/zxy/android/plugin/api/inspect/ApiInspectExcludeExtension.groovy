package com.zxy.android.plugin.api.inspect

import com.google.common.base.Strings

/**
 * Created by zhengxiaoyong on 2018/12/22.
 */
class ApiInspectExcludeExtension {

    Set<String> apis = new HashSet<>()

    void api(String api) {
        if (Strings.isNullOrEmpty(api))
            return
        apis.add(api)
    }

}
