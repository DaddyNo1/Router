package com.daddyno1.buildsrc

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import groovy.io.FileType
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

class RouterTransform extends Transform {

    //存储了所有类名 以Router$$Root$$为前缀的类的全类名列表 ：com/route/Router$$Group$$group1
    List<String> routerRootList = new ArrayList<>()
    //LogisticCenter.class
    File file_LogisticCenter;
    // Class 文件注入的工具类
    RouterInject routerInject;
    //Project
    Project project;

    RouterTransform(Project project){
        this.project = project
        this.routerInject= new RouterInject(project);
    }

    // 返回对应的 Task名称
    @Override
    String getName() {
        return "RouterTransform"
    }

    //确定对哪些类型的结果进行转换
    //TransformManager.CONTENT_CLASS        处理 class文件
    //TransformManager.CONTENT_JARS         处理 class文件 和 资源文件
    //TransformManager.CONTENT_RESOURCES;   处理资源文件
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }


    //指定插件的适用范围
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    //是否支持增量更新。 返回true, TransformInput 会包含一份修改的文件列表；返回false,则会进行全量编译，并且会删除上一次的输出内容
    @Override
    boolean isIncremental() {
        return false
    }

    //具体转换过程。注意输入、输出的概念
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        println "--- RouterTransform ---  transform  start"

        def startTime = System.currentTimeMillis();
        def inputs = transformInvocation.inputs;
        def outProvider = transformInvocation.outputProvider;

        //删除之前输出
        if (outProvider != null) {
            outProvider.deleteAll();
        }

        // Transform 的 inputs 有两种类型， 一种是目录，一种是 jar包
        inputs.each { TransformInput input ->

            // 遍历directionInputs (本地 project 编译成的多个 class)
            input.directoryInputs.each { DirectoryInput directoryInput ->
                handleDirectory(directoryInput, outProvider)
            }

            // 遍历 jarInputs (各个依赖编译成的jar)
            input.jarInputs.each { JarInput jarInput ->
                handleJar(jarInput, outProvider)
            }
        }

//        //根据收集到的信息，进行代码插庄
        if (!routerRootList.isEmpty()){
            routerInject.handleLogisticCenterClass(file_LogisticCenter, routerRootList)
        }

        def cost = (System.currentTimeMillis() - startTime) / 1000
        println "--- RouterTransform ---   transform   end"
        println "RouterTransform cost: $cost s"
    }

    void handleDirectory(DirectoryInput directoryInput, TransformOutputProvider transformOutputProvider) {
//      app module的class文件路径：  /Users/jxf/workspace/Android/githubProject/APTDemo/app/build/intermediates/javac/debug/classes
//        println(directoryInput.file)
//        println(directoryInput.name)   // 5939ff559de81089666d902c13be89aa35451460

        if (directoryInput.file.size() == 0) {
            return;
        }

        if (directoryInput.file.isDirectory()) {
            directoryInput.file.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) { File classFile ->
                def name = classFile.name
                def absPath = classFile.absolutePath

                //如果是MainActivity.class， onCreate最后添加一个 toast
                if(absPath.contains(Consts.HOOK_POINT_MAIN_ACTIVITY)){
                    routerInject.handleActivity(directoryInput.file.absolutePath, Consts.HOOK_POINT_MAIN_ACTIVITY_NAME)
                }

                // Router$$Root$$
                if (isStartWithPrefix(name)){
                    String clsName = getFullClassName(classFile.absolutePath)
                    routerRootList.add(clsName)
                }
            }
        }

        //定义 output
        def dest = transformOutputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY);
        // 将 input目录 复制到 output 目录
        FileUtils.copyDirectory(directoryInput.file, dest);
    }

    /**
     * 处理 Jar 类型的 input
     */
    void handleJar(JarInput jarInput, TransformOutputProvider transformOutputProvider) {

//      非app module 最终会以jar的形式打包出来
//      router module的jar：  /Users/jxf/workspace/Android/githubProject/APTDemo/router/build/intermediates/runtime_library_classes/debug/classes.jar
//      test module生成的jar:  /Users/jxf/workspace/Android/githubProject/APTDemo/test/build/intermediates/runtime_library_classes/debug/classes.jar
//       println(jarInput.file.path)

        def jarName = jarInput.name
        def md5Name =  DigestUtils.md5(jarInput.file.absolutePath)
        if(jarName.endsWith(".jar")){
            jarName = jarName.substring(0, jarName.length() - 4)
        }
        //定义 output  重命名输出文件（同目录copyFile会冲突）
        def dest = transformOutputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)

        if(jarInput.file.absolutePath.endsWith(Consts.JAR)){
            JarFile jarFile = new JarFile(jarInput.file)
            Enumeration enumeration = jarFile.entries();
            //遍历jar文件中的文件
            while (enumeration.hasMoreElements()){
                JarEntry jarEntry = enumeration.nextElement()
                String entryName = jarEntry.name

                //如果是 com/daddyno1/router/LogisticCenter.class
                if(entryName.contains(Consts.HOOK_CLASS_FILE)){
                    file_LogisticCenter = dest
                }

                // Router$$Root$$
                if(containPrefix(entryName)){
                    String className = getFullClassNameFromJarEntry(entryName)
                    routerRootList.add(className)
                }
            }
        }

        // 将内容 复制到 输出
        FileUtils.copyFile(jarInput.file, dest)
    }

    /**
     * 检测文件名是以 Router$$Root$$ 作为前缀，如果是，则收集起来。
     * @param name
     */
    boolean isStartWithPrefix(String name){
        if(name != null &&name.startsWith(Consts.PREFIX)){
            return true;
        }
        return false;
    }

    boolean containPrefix(String name){
        if(name != null &&name.contains(Consts.PREFIX)){
            return true;
        }
        return false;
    }

    /**
     * 从路径中截取 类的全类名
     * @param filePath  class文件的路径
     */
    String getFullClassName(String filePath){
        String fullClassName = ""
        if (filePath != null && filePath.endsWith(Consts.FILE_END_SUFIX)){

            def start = filePath.indexOf(Consts.CLASS_PATH) + Consts.CLASS_PATH.length();
            def end = filePath.lastIndexOf(Consts.FILE_END_SUFIX)
            if (start <= end){
                fullClassName = filePath.substring(start, end)
            }
            fullClassName = fullClassName.replaceAll("/",".")
        }
        return fullClassName
    }

    String getFullClassNameFromJarEntry(String entryName){
        String fullClassName = ""
        if (entryName != null && entryName.endsWith(Consts.FILE_END_SUFIX)){
            def end = entryName.lastIndexOf(Consts.FILE_END_SUFIX)
            if (end != -1){
                fullClassName = entryName.substring(0, end)
            }
            fullClassName = fullClassName.replaceAll("/",".")
        }
        return fullClassName
    }

}