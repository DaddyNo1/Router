# Router
#### 项目介绍：

本项目是一个模仿`ARouter`的页面路由框架。目的是为了对`ARouter`有一个更好的理解。

手动实现一遍能够更好的理解其中的原理。通过这个项目可以获得：

1. 对ARouter的源码更好的理解。
2. 对 **APT** 进行相关实践，以及使用 **javapoet** 辅助生成相关 `.java` 文件
3. 对 **Gradle-plugin** 开发进行实践，学习使用 **Transform** 和 **javassist** 对 `.class` 文件进行处理。

#### 项目结构：

app、test ：用于测试的业务Moudle

router ：核心逻辑

router-annotatior ：定义相关注解

router-compiler ：定义注解处理器`APT`

buildSrc：定义gradle-plugin，通过Transform影响构建过程。

