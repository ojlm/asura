
# 开发指南

## 前端开发环境准备

- 安装 `node` 最新版 [地址](https://nodejs.org/en/)
- 执行 `yarn install` 安装依赖

## 后端开发环境准备

- 安装 `sbt`

> Mac 执行 `brew install sbt`, 其他系统参考[官方文档](https://www.scala-sbt.org/documentation.html)

> 配置阿里云依赖镜像源, 编辑(没有则创建) `$HOME/.sbt/repositories`, 内容如下

```
[repositories]
local
aliyun: http://maven.aliyun.com/nexus/content/groups/public/
```

- IDE 推荐使用 IntellJ IDEA, 并安装上 Scala 插件

- 第一次打开 IDE 或 进入项目目录 会下载 sbt 相关的依赖 和 项目本身的依赖这个过程有些慢...可能要花好几个分钟,只有第一次会这样.

> 如果用 IDE 看不到详细进度
> 如果用 sbt, 进入目录执行 `sbt`, 看不到下载进度, 如果想看可查看日志 `tail -f $HOME/.sbt/boot/update.log`

## 编码参考规范

Scala: [https://docs.scala-lang.org/style/](https://docs.scala-lang.org/style/)

Typescript: [https://github.com/Microsoft/TypeScript/wiki/Coding-guidelines](https://github.com/Microsoft/TypeScript/wiki/Coding-guidelines)

Angular: [https://angular.io/guide/styleguide](https://angular.io/guide/styleguide)
