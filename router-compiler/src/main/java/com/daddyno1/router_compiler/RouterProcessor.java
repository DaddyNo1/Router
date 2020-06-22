package com.daddyno1.router_compiler;

import com.daddyno1.router_annotation.Route;
import com.daddyno1.router_compiler.utils.Utils;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import static com.daddyno1.router_compiler.utils.Consts.*;
import static com.daddyno1.router_compiler.utils.Utils.*;


/**
 * 定义注解处理器，注解处理器是以 module 为单位进行处理。
 */
@AutoService(Processor.class)
public class RouterProcessor extends AbstractProcessor {

    // Map<group, Map<path, Element>>
    private Map<String, Map<String, Element>> groups = new HashMap<>();
    // module 名字
    private String moduleName = null;


    private Filer filer;            //文件工具类
    private Elements elementUtils;  // Element工具类
    private Types typeUtils;        //Type 工具类. 如：判断是否同一个类型，是否子类型等等

    //设定注解处理器支持的 源代码版本
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    //设定 注解处理器 支持的注解类型
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
        annotations.add(Route.class);
        return annotations;
    }

    // 设定 注解处理器 接收的配置参数集合
    @Override
    public Set<String> getSupportedOptions() {
        Set<String> ops = new LinkedHashSet<>();
        ops.add(KEY_MODULE_NAME);
        return ops;
    }


    //初始化方法，初始化一些工具类等相关行为
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        Utils.messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();

        //尝试获取用户参数  moduleName
        Map<String, String> options = processingEnvironment.getOptions();
        moduleName = options.get(KEY_MODULE_NAME);

        //不为空打印一下module名字
        if (!StringUtils.isEmpty(moduleName)) {
            logger("module --> " + moduleName);
        } else {
            //抛出异常让用户去处理
            throw new RuntimeException("Router::Compiler>> 没有配置 moduleName，请在 build.gradle中进行配置");
        }
    }

    /**
     * 进行注解处理
     *
     * @param set              一个集合，包含了所有需要处理的注解的集合。
     * @param roundEnvironment  本回合的上下文环境
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        messager.printMessage(Diagnostic.Kind.NOTE, "---start---");

        //测试 有哪些数据
//        for (TypeElement typeElement : set) {
//            logger(typeElement.getQualifiedName());
//        }
//        Set<? extends Element> eles = roundEnvironment.getElementsAnnotatedWith(Route.class);
//        for (Element element : eles) {
//            Route routeAnn = element.getAnnotation(Route.class);
//            logger(element.getSimpleName());
//            logger(element.asType().toString());
//            logger(routeAnn.path());
//            logger(routeAnn.group());
//        }

        // 需要处理的注解结合不为空才进行解析
        if (!CollectionUtils.isEmpty(set)) {
            //获取所有被 @Route 注解的Element
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Route.class);
            //解析 Element 集合
            parseRoute(elements);
        }

        return false;
    }

    /**
     * 解析所有注解的节点
     */
    private void parseRoute(Set<? extends Element> elements) {
        if (CollectionUtils.isNotEmpty(elements)) {
            logger(moduleName + "模块发现路由，个数为：" + elements.size());

            //获取 RouteMeta、IRouteGroup、IRouteRoot 类型
            TypeElement type_RouteMeta = elementUtils.getTypeElement("com.daddyno1.router.RouteMeta");
            TypeElement type_IRouteGroup = elementUtils.getTypeElement("com.daddyno1.router.IRouteGroup");
            TypeElement type_IRouteRoot = elementUtils.getTypeElement("com.daddyno1.router.IRouteRoot");

            /**
             * 扫描节点，获取有用信息，构建相关集合
             */
            for (Element element : elements) {
                Route annotation = element.getAnnotation(Route.class);
                String path = annotation.path();
                String group = getGroupFromAnnotation(annotation);
                logger("path: " + path);
                logger("group: " + group);

                Map<String, Element> routes = groups.get(group);
                if (routes == null) {
                    routes = new HashMap<>();
                    groups.put(group, routes);
                }
                routes.put(path, element);
            }


            logger(groups.toString());


            /**
             * public class Router$$Group$$group2 implements IRouteGroup {
             *     @Override
             *     public void loadInto(Map<String, RouteMeta> routes) {
             *         routes.put("/group2/second", RouteMeta.build(SecondActivity.class, "/group2/second", "group2"));
             *     }
             * }
             */
            //////////////////创建 Router$$Group$$ + group  类
            //创建参数类型  Map<String, RouteMeta>
            ParameterizedTypeName inputMapTypeOfGroup = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(type_RouteMeta)
            );

            //创建参数 routes
            ParameterSpec groupParamSpec = ParameterSpec.builder(inputMapTypeOfGroup,
                    "routes").build();

            // 循环每个组，分别创建不同的 路由组类
            for (Map.Entry<String, Map<String, Element>> mapEntry : groups.entrySet()) {
                String group = mapEntry.getKey();
                Map<String, Element> routes = mapEntry.getValue();

                //创建方法 loadInto
                MethodSpec.Builder loadIntoMethodOfGroup = MethodSpec.methodBuilder("loadInto")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(groupParamSpec);

                //循环所有路由表插入
                for (Map.Entry<String, Element> stringElementEntry : routes.entrySet()) {
                    String path = stringElementEntry.getKey();
                    Element element = stringElementEntry.getValue();


                    /**
                     * $T ：  TypeName/ClassName 、TypeMirror、 Element/TypeElement、Type
                     *
                     *       if (o instanceof TypeName) return (TypeName) o;
                     *       if (o instanceof TypeMirror) return TypeName.get((TypeMirror) o);
                     *       if (o instanceof Element) return TypeName.get(((Element) o).asType());
                     *       if (o instanceof Type) return TypeName.get((Type) o);
                     *
                     */

                    /**
                     * $N:   Object、ParameterSpec、FieldSpec、MethodSpec、TypeSpec
                     *
                     *       if (o instanceof CharSequence) return o.toString();
                     *       if (o instanceof ParameterSpec) return ((ParameterSpec) o).name;
                     *       if (o instanceof FieldSpec) return ((FieldSpec) o).name;
                     *       if (o instanceof MethodSpec) return ((MethodSpec) o).name;
                     *       if (o instanceof TypeSpec) return ((TypeSpec) o).name;
                     */
                    loadIntoMethodOfGroup.addStatement("routes.put($S, $T.build( $T.class, $S, $S))",
                            path,
                            type_RouteMeta,
                            element,
                            path,
                            group);

                    /**
                     * Element 是所有被注解注释的节点类。(apt api) 包含：
                     *     PACKAGE,
                     *     ENUM,
                     *     CLASS,
                     *     ANNOTATION_TYPE,
                     *     INTERFACE,
                     *     ENUM_CONSTANT,
                     *     FIELD,
                     *     PARAMETER,
                     *     LOCAL_VARIABLE,
                     *     EXCEPTION_PARAMETER,
                     *     METHOD,
                     *     CONSTRUCTOR,
                     *     STATIC_INIT,
                     *     INSTANCE_INIT,
                     *     TYPE_PARAMETER,
                     *     OTHER,
                     *     RESOURCE_VARIABLE;
                     *
                     * TypeElement 是 Element 的子类，类型的节点类。(apt api) 如：CLASS、ENUM、INTERFACE
                     *
                     * TypeMirror (apt api) 是 Element 这个节点的一种类型表达方式。  可通过 Element#asType 获取。因为TypeMirror 可以从Element 中获取
                     * 所以一般可以 传 Element 的地方，也有 TypeMirror 参数的方法重载。
                     *
                     * Type 是一个接口。（java reflect api） Class 类实现了 Type。如果参数是Type类型的，代表可以传入任何类的Class对象。
                     *
                     * TypeName/ClassName (javaPoet api) 是Java 类型的表达方式。如：BOOLEAN 、INT 等等，当然也可以是任何类型。
                     * ClassName 继承自 TypeName。可以通过 （package + name） 、 Type 、 TypeElement 获得。
                     *
                     */
                }

                //Router$$Group$$ + group
                TypeSpec typeOfGroup = TypeSpec.classBuilder(CLASS_OF_ROUTE_GROUP + group)
                        .addModifiers(Modifier.PUBLIC)
                        .addSuperinterface(type_IRouteGroup.asType())
                        .addMethod(loadIntoMethodOfGroup.build())
                        .build();

                JavaFile javaFileOfGroup = JavaFile.builder("com.route", typeOfGroup)
                        .build();

                try {
                    javaFileOfGroup.writeTo(filer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            /**
             * public class Router$$Root$$app implements IRouteRoot {
             *   @Override
             *   public void loadInto(Map<String, Class<? extends IRouteGroup>> routeGroups) {
             *     routeGroups.put("group2", Router$$Group$$group2.class);
             *     routeGroups.put("group1", Router$$Group$$group1.class);
             *   }
             * }
             * }
             */
            //////////////////创建 Router$$Root$$ + module  类
            //创建参数类型 Map<String, Class<? extends IRouteGroup>>
            ParameterizedTypeName inputMapTypeOfRoot = ParameterizedTypeName.get(ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(ClassName.get(type_IRouteGroup))
                    ));

            //创建参数
            ParameterSpec rootParamSpec = ParameterSpec.builder(inputMapTypeOfRoot,
                    "routeGroups")
                    .build();

            MethodSpec.Builder builder = MethodSpec.methodBuilder("loadInto")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(rootParamSpec);

            for (Map.Entry<String, Map<String, Element>> mapEntry : groups.entrySet()) {
                String group = mapEntry.getKey();
                builder.addStatement("routeGroups.put($S, $L.class)", group, CLASS_OF_ROUTE_GROUP + group);
            }

            TypeSpec typeOfRoot = TypeSpec.classBuilder(CLASS_OF_ROOT_GROUP + moduleName)
                    .addSuperinterface(type_IRouteRoot.asType())
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(builder.build())
                    .build();

            JavaFile javaFileOfRoot = JavaFile.builder("com.route", typeOfRoot)
                    .build();

            try {
                javaFileOfRoot.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从注解中获取 group 信息
     */
    private String getGroupFromAnnotation(Route annotation) {
        String group = annotation.group();
        if (StringUtils.isNotEmpty(group)) {
            return group;
        }
        group = getGroupFromPath(annotation.path());
        return group;
    }

    //从 path 中获取 group信息
    private String getGroupFromPath(String path) {
        String group;
        if (StringUtils.isEmpty(path)) {
            throw new RuntimeException("module: " + moduleName + ".   route path : [" + path + "] is empty;");
        }
        if (!path.startsWith("/")) {
            throw new RuntimeException("module: " + moduleName + ".   route path : [" + path + "] is must start with /");
        }
        int index = path.indexOf("/", 1);
        if (index == -1) {
            throw new RuntimeException("module: " + moduleName + ".   route path : [" + path + "]  must contains at lease two /");
        }
        group = path.substring(1, path.indexOf("/", index));
        return group;
    }
}
