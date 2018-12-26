package com.zxy.android.plugin.api.inspect

import com.android.utils.FileUtils
import com.google.common.base.Strings
import javassist.ClassPool
import javassist.CtClass
import org.gradle.api.Project

/**
 * Created by zhengxiaoyong on 2018/12/23.
 */
class ApiInspectTools {

    static void exportApiInspectInfo(Project project, String variant, Set<String> inspectedPackages, Set<String> inspectedClasses) {
        File target = new File(project.buildDir, "api-inspect" + File.separator + variant + File.separator + "inspect-info.txt")
        target.parentFile.mkdirs()
        FileUtils.deleteIfExists(target)
        target.createNewFile()
        BufferedWriter writer = target.newWriter("UTF-8", true)

        writer.write("==================================>Inspect Packages<==================================\n")
        if (!inspectedPackages.isEmpty()) {
            inspectedPackages.each {
                writer.write("> $it\n")
            }
        } else {
            writer.write("> NONE.\n")
        }
        writer.write("======================================================================================\n\n")

        writer.write("==================================>Inspect Classes<==================================\n")
        if (!inspectedClasses.isEmpty()) {
            inspectedClasses.each {
                writer.write("> $it\n")
            }
        } else {
            writer.write("> NONE.\n")
        }
        writer.write("======================================================================================\n\n")
        writer.flush()
        writer.close()
    }

    static void exportApiInspectResult(Project project, String variant, Set<IncompatibleClassInfo> incompatibleClassInfoSet, Set<IncompatibleMethodInfo> incompatibleMethodInfoSet) {
        File target = new File(project.buildDir, "api-inspect" + File.separator + variant + File.separator + "inspect-result.txt")
        target.parentFile.mkdirs()
        FileUtils.deleteIfExists(target)
        target.createNewFile()
        BufferedWriter writer = target.newWriter("UTF-8", true)

        writer.write("==================================>Inspect Results<==================================\n")
        if (!incompatibleClassInfoSet.isEmpty()) {
            incompatibleClassInfoSet.each {
                writer.write("Incompatible Api -> [Class: ${it.incompatibleClassName}]\n")
                writer.write("                 └> [Occur In Class : ${it.className}]\n")
            }
        }

        if (!incompatibleMethodInfoSet.isEmpty()) {
            incompatibleMethodInfoSet.each {
                writer.write("Incompatible Api -> [Class: ${it.incompatibleClassName}]\n")
                writer.write("                 └> [Method: ${it.methodName}]\n")
                writer.write("                 └> [Occur In Class: ${it.className}, Line: ${it.lineNumber}]\n")
            }
        }

        if (incompatibleClassInfoSet.isEmpty() && incompatibleMethodInfoSet.isEmpty()) {
            writer.write("> NONE.\n")
        }
        writer.write("======================================================================================\n\n")
        writer.flush()
        writer.close()
    }

    static String getPackageFromBuildConfig(ClassPool classPool, Project project, File jarFile) {
        def jarPath = jarFile.absolutePath
        if (jarPath == null)
            return null
        def buildConfigs = project.zipTree(jarPath).filter {
            it.name == "BuildConfig.class"
        }

        def packageName

        if (buildConfigs != null && buildConfigs.size() > 0) {
            def classStream = null
            try {
                def file = buildConfigs.getSingleFile()
                classStream = new FileInputStream(file)
                CtClass ctClass = classPool.makeClass(classStream)
                packageName = ctClass.getDeclaredField("APPLICATION_ID").constantValue

                if (Strings.isNullOrEmpty(packageName)) {
                    packageName = ctClass.getPackageName()
                }

                return packageName
            } catch (Exception e) {
                if (classStream != null)
                    classStream.close()
            }
        }
        return null
    }

}
