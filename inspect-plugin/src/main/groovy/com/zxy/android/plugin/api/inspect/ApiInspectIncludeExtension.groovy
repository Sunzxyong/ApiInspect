package com.zxy.android.plugin.api.inspect

import com.google.common.base.Strings

/**
 * Created by zhengxiaoyong on 2018/12/22.
 */
class ApiInspectIncludeExtension {

    Set<String> apis = new HashSet<>()

    void api(String api) {
        if (Strings.isNullOrEmpty(api))
            return
        apis.add(api)
    }

}
