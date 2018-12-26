package com.zxy.android.plugin.api.inspect

import com.google.common.base.Strings
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.NotFoundException
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import org.gradle.api.Project

/**
 * Created by zhengxiaoyong on 2018/12/22.
 */
class ApiInspector {

    private Project mProject

    private Set<String> mInspectedClasses = new HashSet<>()

    private Set<String> mInspectedPackages = new HashSet<>()

    private Set<IncompatibleClassInfo> mIncompatibleClasses = new HashSet<>()

    private Set<IncompatibleClassInfo> mIncompatibleMethods = new HashSet<>()

    private ApiInspectFilter mApiInspectFilter

    ApiInspector(Project project, ApiInspectFilter filter) {
        this.mProject = project
        mApiInspectFilter = filter
    }

    Set<String> getInspectedClasses() {
        return mInspectedClasses
    }

    Set<String> getInspectedPackages() {
        return mInspectedPackages
    }

    Set<IncompatibleClassInfo> getIncompatibleClasses() {
        return mIncompatibleClasses
    }

    Set<IncompatibleClassInfo> getIncompatibleMethods() {
        return mIncompatibleMethods
    }

    void addInspectedPackage(String packageName) {
        mInspectedPackages.add(packageName)
    }

    void inspectClass(ClassPool classPool, CtClass clazz) {
        if (mApiInspectFilter.filter(clazz))
            return

        if (!shouldInspect(clazz.name))
            return

        clazz.getRefClasses().each {
            if (!mApiInspectFilter.filter(String.valueOf(it))) {
                try {
                    classPool.get(it)
                } catch (NotFoundException e) {
                    IncompatibleClassInfo info = new IncompatibleClassInfo()
                    info.className = clazz.name
                    info.incompatibleClassName = it
                    mIncompatibleClasses.add(info)
                } catch (Exception e) {
                    //ignore.
                }
            }
        }

        inspectMethod(clazz)
    }

    void inspectMethod(CtClass clazz) {
        CtMethod[] methods = clazz.getDeclaredMethods()
        if (methods == null || methods.length == 0)
            return

        try {
            methods.each {
                it.instrument(new ExprEditor() {
                    @Override
                    void edit(MethodCall m) throws CannotCompileException {
                        super.edit(m)
                        try {
                            if (!mApiInspectFilter.filter(m.className))
                                m.getMethod()
                        } catch (NotFoundException e) {
                            IncompatibleMethodInfo info = new IncompatibleMethodInfo()
                            info.className = clazz.name
                            info.incompatibleClassName = m.className
                            info.methodName = m.methodName
                            info.signature = m.signature
                            info.lineNumber = m.lineNumber
                            mIncompatibleMethods.add(info)
                        } catch (Exception e) {
                            //ignore.
                        }
                    }
                })
            }
        } catch (Exception e) {
            //ignore.
        }
    }

    boolean shouldInspect(String className) {
        if (Strings.isNullOrEmpty(className))
            return false
        boolean inspect = mInspectedClasses.contains(className)
        if (!inspect) {
            mInspectedClasses.add(className)
        }
        return !inspect
    }

}
