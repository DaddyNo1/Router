package com.daddyno1.buildsrc

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils

/**
 * 这里对 input为Class、scope为Full_Project的 Transform 公共的行为进行了
 * 封装。继承自该类的Transform 理论上之只要实现  getName  transform 即可。
 * 当然，其它方法也可以修改。
 */
abstract class ClassFullProjectTransform extends Transform {

    def outputJars = new ArrayList() //jar文件所有输出   File:Jar
    def outputDirs = new ArrayList() //dir文件所有输出   File:Directory

    TransformOutputProvider outputProvider
    Collection<TransformInput> inputs

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        //Scope.PROJECT, Scope.SUB_PROJECTS, Scope.EXTERNAL_LIBRARIES
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        //输入
        inputs = transformInvocation.inputs;
        //输出管理类
        outputProvider = transformInvocation.getOutputProvider()

        //遍历输入
        inputs.each { TransformInput transformInput ->
            transformInput.directoryInputs.each { DirectoryInput directoryInput ->
                handleDirectory(directoryInput)
            }
            transformInput.jarInputs.each { JarInput jarInput ->
                handleJar(jarInput)
            }
        }
    }

    /**
     * 处理Dir的输入
     * 每一个任务都会会有 输入、输出。此时是默认输入是：/......../app/build/intermediates/javac/debug/classes
     * 这里有一个问题就是，如果我们的任务依赖了其它的任务，也许输入就不是这个路径了。如何从其中解析出 classPath 和 className，这是一个问题，
     * 解决方法：直接使用输出的内容即可。
     *
     * 另外一个问题是：我们对字节码的处理应该在上一个任务的输出上直接修改字节码，处理之后拷贝一份作为自己的输出；还是应该先从上一个任务
     * 的输出拷贝一份 作为自己的输出，然后修改自己的输出内容（通常是字节码处理）。按理说后一种方式是对的，这样不会影响上一个任务的输出，
     * 否则会不会出现这种问题：很多任务都依赖同一个任务进行执行，假如某一个任务改了 那个任务的输出，那么其它任务有可能会被污染。所以根据以上
     * 思路修改自己处理逻辑。
     */
    final def  handleDirectory(DirectoryInput directoryInput) {
        //打印 ClickTraceTransform 这个任务的输入
        println "ClickTraceTransform-DirInput: ${directoryInput.file.path}"
        //定义输出
        def dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
        FileUtils.copyDirectory(directoryInput.file, dest)
        //打印 ClickTraceTransform 这个任务最终的输出
        println "ClickTraceTransform-DirOutput: ${dest.path}"

        //添加输出
        outputDirs.add(dest)
    }

    // 处理 jar
    final def handleJar(JarInput jarInput) {
        //打印 ClickTraceTransform 这个任务的输入
        println "ClickTraceTransform-JarInput: ${jarInput.file.path}"

        def jarName = jarInput.name
        def md5Name = DigestUtils.md5(jarInput.file.absolutePath)
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4)
        }
        //定义 output  重命名输出文件（同目录copyFile会冲突）
        def dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        // 将内容 复制到 输出
        FileUtils.copyFile(jarInput.file, dest)

        //打印 ClickTraceTransform 这个任务的输入
        println "ClickTraceTransform-JarOutput: ${dest.path}"

        //添加输出
        outputJars.add(dest)
    }

}