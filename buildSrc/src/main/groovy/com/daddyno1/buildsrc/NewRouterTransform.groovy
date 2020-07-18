package com.daddyno1.buildsrc

import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import groovy.io.FileType
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

class NewRouterTransform extends ClassFullProjectTransform {

    //存储了所有类名 以Router$$Root$$为前缀的类的全类名列表 ：com/route/Router$$Group$$group1
    List<String> routerRootList = new ArrayList<>()
    //LogisticCenter.class
    File file_LogisticCenter
    // Class 文件注入的工具类
    RouterInject routerInject
    //Project
    Project project

    @Override
    String getName() {
        return "RouterTransform"
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        println "--- RouterTransform ---  transform  start"
        super.transform(transformInvocation)

        def startTime = System.currentTimeMillis()
        this.routerInject = new RouterInject(project)


        outputDirs.each { File file ->
            file.traverse(type: FileType.FILES, nameFilter: ~/.*\.class/) { File classFile ->
                def name = classFile.name
                def absPath = classFile.absolutePath

                //如果是MainActivity.class， onCreate最后添加一个 toast
                if(absPath.contains(Consts.HOOK_POINT_MAIN_ACTIVITY)){
                    routerInject.handleActivity(file.path, Consts.HOOK_POINT_MAIN_ACTIVITY_NAME)
                }

                // Router$$Root$$
                if (isStartWithPrefix(name)){
                    String clsName = getClassFullName(file.path, classFile)
                    routerRootList.add(clsName)
                }
            }
        }

        outputJars.each { File file ->
            if(file.absolutePath.endsWith(Consts.JAR)){
                JarFile jarFile = new JarFile(file)
                Enumeration enumeration = jarFile.entries();
                //遍历jar文件中的文件
                while (enumeration.hasMoreElements()){
                    JarEntry jarEntry = enumeration.nextElement()
                    String entryName = jarEntry.name

                    //如果是 com/daddyno1/router/LogisticCenter.class
                    if(entryName.contains(Consts.HOOK_CLASS_FILE)){
                        file_LogisticCenter = file
                    }

                    // Router$$Root$$
                    if(containPrefix(entryName)){
                        String className = getFullClassNameFromJarEntry(entryName)
                        routerRootList.add(className)
                    }
                }
            }
        }


        //根据收集到的信息，进行代码插庄
        if (!routerRootList.isEmpty()) {
            routerInject.handleLogisticCenterClass(file_LogisticCenter, routerRootList)
        }

        def cost = (System.currentTimeMillis() - startTime) / 1000
        println "--- RouterTransform ---   transform   end"
        println "RouterTransform cost: $cost s"
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
     * 逻辑更新以后，目前这个方法有点问题。我们不应该从 /classes/ 中截取 全类名，因为我么不确定输入路径一定包含 /classes/
     * 使用下面的逻辑  def getClassFullName(String classPath, File file) {
     */
    @Deprecated
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

    /**
     * 获取class  File 的全类名
     * @param file
     * @param classPath file对应的path
     */
    def getClassFullName(String classPath, File file) {
        if (file == null || classPath == null) return
        def classNameTmp = file.path - (classPath + "/") - Consts.FILE_END_SUFIX
        return classNameTmp.replaceAll(Consts.FILE_SEPARATOR, Consts.DOT)
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