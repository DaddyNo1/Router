package com.daddyno1.buildsrc;
import com.android.build.gradle.AppExtension;
import org.apache.commons.io.IOUtils;
import org.gradle.api.Project;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class RouterInject {
    public static ClassPool classPool = new ClassPool(ClassPool.getDefault());
    Project project;
    AppExtension appExtension;
    private boolean isAndroidClassLoaded = false;

    public RouterInject(Project project){
        this.project = project;
        appExtension = this.project.getExtensions().findByType(AppExtension.class);
    }

    /**
     *  给ClassPool 添加类加载的路径。
     * @param pathName Appends a directory or a jar (or zip) file to the end of the search path.
     */
    private void appendClassPath(String pathName){
        System.out.println("class pool append classPath: " + pathName);

        try {
            classPool.appendClassPath(pathName);
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据 direction 处理 class 文件。把传入的Activity onCreate 方法后边插入一段 Toast 代码。
     * @param clsPath       class 文件的本机路径
     * @param clsName       class 文件的全类名
     */
    public void handleActivity(String clsPath, String clsName){
        loadAndroidClass();

        try {
            // 将当前类的路径加入 ClassPool
            appendClassPath(clsPath);

            CtClass ctClass = classPool.get(clsName);
            CtMethod ctMethod = ctClass.getDeclaredMethod("onCreate");

            classPool.importPackage("android.widget.Toast");
            ctMethod.insertAfter("Toast.makeText(this, \"hello\", Toast.LENGTH_SHORT).show();");

            //写回去
            ctClass.writeFile(clsPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理 LogisticCenter class文件
     */
    public void handleLogisticCenterClass(File file, List<String> routerRootList){

        try {
            appendClassPath(file.getAbsolutePath());
            CtClass ctClass = classPool.get(Consts.HOOK_CLASS_NAME);
            //解冻
            if(ctClass.isFrozen()){
                ctClass.defrost();
            }
            CtMethod ctMethod = ctClass.getDeclaredMethod(Consts.HOOK_METHOD_NAME);

            StringBuilder builder = new StringBuilder();
            builder.append("{");
            for (String root : routerRootList) {
                builder.append("registerRouteRoots(register(\"" + root + "\")); \n");
            }
            builder.append("}");
            ctMethod.setBody(builder.toString());

            // 怎么把CtClass 写回 jar
            handleJarFile(file, ctClass.toBytecode());

        } catch (NotFoundException | CannotCompileException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 涉及 Jar 文件的修改。需要临时文件过渡。
     */
    private void handleJarFile(File file, byte[] bytes){
        try {
            // 过渡文件
            File optJar = new File(file.getParent(), file.getName() + ".opt");
            //java.util.zip.ZipException: zip file is empty  为什么不能使用已经存在的File，使用的话直接就编程0字节了？
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));

            //遍历输入的 Jar File
            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()){
                JarEntry jarEntry =  enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                InputStream inputStream = jarFile.getInputStream(zipEntry);

                jarOutputStream.putNextEntry(zipEntry);

                //如果是 com/daddyno1/router/LogisticCenter.class
                if(entryName.contains(Consts.HOOK_CLASS_FILE)){
                    System.out.println(">>>>>>>> 处理 " + Consts.HOOK_CLASS_FILE + " <<<<<<<<<");
                    jarOutputStream.write(bytes);
                }else{
                    //不作处理
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }

                jarOutputStream.closeEntry();
                inputStream.close();
            }
            jarOutputStream.close();
            jarFile.close();

            //删除源文件
            if(file.exists()){
                file.delete();
            }
            //生成的临时文件 重命名成源文件的名字
            optJar.renameTo(file);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化 ClassPool，从而可以访问原始的  Android sdk
     */
    private void loadAndroidClass(){
        if (!isAndroidClassLoaded){
            // [ /Users/jxf/workspace/Android/sdk/platforms/android-29/android.jar,
            // /Users/jxf/workspace/Android/sdk/build-tools/29.0.3/core-lambda-stubs.jar ]
            List<File> bootClassPaths = appExtension.getBootClasspath();
            if(!bootClassPaths.isEmpty()){
                for (Object bootClassPath : bootClassPaths) {
                    appendClassPath(bootClassPath.toString());
                }
            }
        }
    }
}
