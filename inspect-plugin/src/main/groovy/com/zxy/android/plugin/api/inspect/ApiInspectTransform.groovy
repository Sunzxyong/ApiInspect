package com.zxy.android.plugin.api.inspect

import com.android.SdkConstants
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.utils.FileUtils
import com.google.common.base.Strings
import com.google.common.collect.ImmutableSet
import javassist.ClassPool
import javassist.CtClass
import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Created by zhengxiaoyong on 2018/12/22.
 */
class ApiInspectTransform extends Transform {

    Project mProject

    ClassPool mClassPool

    ApiInspector mApiInspector

    DefaultApiInspectFilter mApiInspectFilter

    ApiInspectExtension mApiInspectExtension

    Set<File> mClassDirectoryPaths = new HashSet<>()

    Set<File> mJarFilePaths = new HashSet<>()

    ApiInspectTransform(Project project) {
        this.mProject = project
        mApiInspectExtension = mProject.extensions.findByType(ApiInspectExtension.class)
        mApiInspectFilter = new DefaultApiInspectFilter(project)
    }

    @Override
    String getName() {
        return "apiInspect"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return ImmutableSet.of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return ImmutableSet.of(
                QualifiedContent.Scope.PROJECT,
                QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
                QualifiedContent.Scope.SUB_PROJECTS,
                QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES)
    }

    @Override
    Set<? super QualifiedContent.Scope> getReferencedScopes() {
        return ImmutableSet.of(QualifiedContent.Scope.PROVIDED_ONLY)
    }

    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider()
        if (outputProvider == null) {
            throw new IllegalArgumentException("Missing output object for transform " + getName())
        }

        if (!mApiInspectExtension.enable) {
            justNoOp(transformInvocation, outputProvider)
            return
        }

        initialize(transformInvocation)

        if (transformInvocation.isIncremental() && isIncremental()) {
            transformInvocation.inputs.each { TransformInput input ->
                input.directoryInputs.each { DirectoryInput directoryInput ->
                    File classDirectory = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)

                    mClassPool.appendClassPath(classDirectory.absolutePath)

                    Map<File, Status> changedFiles = directoryInput.getChangedFiles()
                    for (Map.Entry<File, Status> changedInput : changedFiles.entrySet()) {
                        File changeInputFile = changedInput.getKey()
                        Status status = changedInput.getValue()

                        if (!changeInputFile.getName().endsWith(SdkConstants.DOT_CLASS)) {
                            continue
                        }
                        switch (status) {
                            case Status.NOTCHANGED:
                                //do nothing.
                                break
                            case Status.ADDED:
                            case Status.CHANGED:
                                String addFileName = FileUtils.relativePossiblyNonExistingPath(changeInputFile, directoryInput.file)
                                File addFile = new File(classDirectory, addFileName)
                                copyFileCompatible(changeInputFile, addFile)
                                break
                            case Status.REMOVED:
                                String removeFileName = FileUtils.relativePossiblyNonExistingPath(changeInputFile, directoryInput.file)
                                File removeFile = new File(classDirectory, removeFileName)
                                if (removeFile.exists()) {
                                    if (removeFile.isDirectory()) {
                                        FileUtils.deletePath(removeFile)
                                    } else {
                                        FileUtils.deleteIfExists(removeFile)
                                    }
                                }
                                break
                            default:
                                break
                        }
                    }
                }

                input.jarInputs.each { JarInput jarInput ->
                    File jarFile = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                    switch (jarInput.status) {
                        case Status.NOTCHANGED:
                            //do nothing.
                            mClassPool.appendClassPath(jarFile.absolutePath)
                            break
                        case Status.ADDED:
                        case Status.CHANGED:
                            copyFileCompatible(jarInput.file, jarFile)
                            mClassPool.appendClassPath(jarFile.absolutePath)
                            mJarFilePaths.add(jarFile)
                            break
                        case Status.REMOVED:
                            FileUtils.deleteIfExists(jarFile)
                            break
                    }
                }
            }
        } else {
            outputProvider.deleteAll()

            transformInvocation.inputs.each { TransformInput input ->
                input.directoryInputs.each { DirectoryInput directoryInput ->
                    File classDirectory = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                    FileUtils.copyDirectory(directoryInput.file, classDirectory)

                    mClassPool.appendClassPath(classDirectory.absolutePath)
                    mClassDirectoryPaths.add(classDirectory)
                }

                input.jarInputs.each { JarInput jarInput ->
                    File jarFile = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    copyFileCompatible(jarInput.file, jarFile)

                    mClassPool.appendClassPath(jarFile.absolutePath)
                    mJarFilePaths.add(jarFile)
                }
            }
        }

        mJarFilePaths.each { jarFile ->

            String packageName = ApiInspectTools.getPackageFromBuildConfig(mClassPool, mProject, jarFile)

            if (shouldInspectApi(packageName)) {
                mApiInspector.addInspectedPackage(packageName)

                JarFile jar = new JarFile(jarFile)
                Enumeration<JarEntry> jarEntries = jar.entries()
                while (jarEntries.hasMoreElements()) {
                    JarEntry jarEntry = jarEntries.nextElement()
                    if (jarEntry.isDirectory())
                        continue
                    String entryName = jarEntry.getName()
                    if (entryName.endsWith(SdkConstants.DOT_CLASS)) {
                        String className = entryName.replace('\\', '.').replace(File.separator, '.')
                        className = className.substring(0, className.length() - SdkConstants.DOT_CLASS.length())
                        try {
                            CtClass clazz = mClassPool.get(className)
                            mApiInspector.inspectClass(mClassPool, clazz)
                        } catch (Exception e) {
                            //ignore.
                        }
                    }
                }
            }
        }

        Set<IncompatibleClassInfo> incompatibleClassInfoSet = mApiInspector.getIncompatibleClasses()
        Set<IncompatibleMethodInfo> incompatibleMethodInfoSet = mApiInspector.getIncompatibleMethods()

        if (!incompatibleClassInfoSet.isEmpty() || !incompatibleMethodInfoSet.isEmpty()) {
            int count = incompatibleClassInfoSet.size() + incompatibleMethodInfoSet.size()
            mProject.logger.error("\n=====================================>Api Incompatible<=====================================")
            mProject.logger.error(">>> Count: [$count]")
            mProject.logger.error("--------------------------------------------------------------------------------------------")
            incompatibleClassInfoSet.each {
                mProject.logger.error("Incompatible Api -> [Class: ${it.incompatibleClassName}]")
                mProject.logger.error("                 └> [Occur in class : ${it.className}]")
                mProject.logger.error("--------------------------------------------------------------------------------------------")
            }
            incompatibleMethodInfoSet.each {
                mProject.logger.error("Incompatible Api -> [Class: ${it.incompatibleClassName}]")
                mProject.logger.error("                 └> [Method: ${it.methodName}]")
                mProject.logger.error("                 └> [Occur in class: ${it.className}, Line: ${it.lineNumber}]")
                mProject.logger.error("--------------------------------------------------------------------------------------------")
            }
            mProject.logger.error("============================================================================================\n")
        }

        def variant = transformInvocation.context.variantName
        ApiInspectTools.exportApiInspectInfo(mProject, variant, mApiInspector.getInspectedPackages(), mApiInspector.getInspectedClasses())
        ApiInspectTools.exportApiInspectResult(mProject, variant, incompatibleClassInfoSet, incompatibleMethodInfoSet)
    }

    def justNoOp(TransformInvocation transformInvocation, TransformOutputProvider outputProvider) {
        outputProvider.deleteAll()

        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File classDirectory = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(directoryInput.file, classDirectory)
            }

            input.jarInputs.each { JarInput jarInput ->
                File jarFile = outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                copyFileCompatible(jarInput.file, jarFile)
            }
        }
    }

    def setupConfig() {
        ApiInspectIncludeExtension includeExtension = mApiInspectExtension.include
        ApiInspectExcludeExtension excludeExtension = mApiInspectExtension.exclude

        if (includeExtension != null && includeExtension.apis != null)
            mApiInspectFilter.addIncludePackages(includeExtension.apis)

        if (excludeExtension != null && excludeExtension.apis != null)
            mApiInspectFilter.addExcludePackages(excludeExtension.apis)
    }

    def shouldInspectApi(String packageName) {
        if (Strings.isNullOrEmpty(packageName))
            return false

        return !mApiInspectFilter.filterPackage(packageName)
    }

    def initialize(TransformInvocation transformInvocation) {
        mClassPool = new ClassPool()
        mApiInspector = new ApiInspector(mProject, mApiInspectFilter)

        setupConfig()

        def android
        if (mProject.plugins.hasPlugin("com.android.application")) {
            android = mProject.extensions.getByType(AppExtension.class)
        } else if (mProject.plugins.hasPlugin("com.android.library")) {
            android = mProject.extensions.getByType(LibraryExtension.class)
        } else {
            throw new GradleException("The plugin type is not supported！")
        }

        def androidJar = "${android.getSdkDirectory().getAbsolutePath()}${File.separator}platforms${File.separator}" +
                "${android.getCompileSdkVersion()}${File.separator}android.jar"
        mClassPool.appendClassPath(androidJar)

        Collection<TransformInput> referencedInputs = transformInvocation.getReferencedInputs()
        referencedInputs.each {
            it.directoryInputs.each {
                mClassPool.appendClassPath(it.file.absolutePath)
            }
            it.jarInputs.each {
                mClassPool.appendClassPath(it.file.absolutePath)
            }
        }
    }

    def copyFileCompatible(File srcFile, File destFile) {
        try {
            FileUtils.copyFile(srcFile, destFile)
        } catch (Exception e) {
            org.apache.commons.io.FileUtils.copyFile(srcFile, destFile)
        }
    }
}
