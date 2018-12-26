# **ApiInspect**
An api compatibility inspect gradle plugin.（一个用于检测Api兼容性的Gradle插件）

----

[ ![Download](https://api.bintray.com/packages/sunzxyong/maven/ApiInspect/images/download.svg) ](https://bintray.com/sunzxyong/maven/ApiInspect/_latestVersion)[![Travis](https://img.shields.io/travis/rust-lang/rust.svg)]() [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)]() ![](https://img.shields.io/badge/architecture-clean-yellow.svg)

## **Introduce**


## **Usage**
### **Installation**
Add dependencies in **`build.gradle`** of the **`root project`**：

```
    dependencies {
        // ...
        classpath('com.zxy.android.plugin:api-inspect:1.0.0') {
            exclude module: 'gradle'
        }
    }
```

and add the **`apply plugin`** to build.gradle in the module：

```
apply plugin: 'api.inspect'
```

### **Configuration**
By default, **`ApiInspect`** will inspects all apis but does not contain the system api. Of course, you can also customize **`exclude`** or **`include`** Settings：

```
apiInspect {

    enable true //Whether api inspect is enabled.

    inspectSystemApi false //Whether to inspect the system api.

    //Specify the library to inspect.
//    include {
//        //Value is the package name.
//        api "com.zxy.tiny"
//    }

    //Specify the library not to inspect.
//    exclude {
//        //Value is the package name.
//        api 'com.zxy.tiny'
//        api 'com.google.zxing'
//    }

}
```

## **Inspect Result**
When the Apk build is completed. The results of the inspection will be printed on the console：

<img src="https://raw.githubusercontent.com/Sunzxyong/ImageRepository/master/apiinspect.png" width="500"/>

Of course, The results of the inspection will also be stored in the **`api-inspect`** directory：

<img src="https://raw.githubusercontent.com/Sunzxyong/ImageRepository/master/apiinspect_result.jpg" width="500"/>

## **Support**

> **Support Gradle Plugin Version： >=2.3.3**

## **Version**
Version control supports the semantic 2.0 protocol

* **1.0.0：First version, support api compatibility inspect.**

## **License**

>
>     Apache License
>
>     Version 2.0, January 2004
>     http://www.apache.org/licenses/
>
>     Copyright 2018 郑晓勇
>
>  Licensed under the Apache License, Version 2.0 (the "License");
>  you may not use this file except in compliance with the License.
>  You may obtain a copy of the License at
>
>      http://www.apache.org/licenses/LICENSE-2.0
>
>  Unless required by applicable law or agreed to in writing, software
>  distributed under the License is distributed on an "AS IS" BASIS,
>  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
>  See the License for the specific language governing permissions and
>  limitations under the License.


