## Asura

> ⛩`( ⊙ o ⊙ )`

这个是 [Indigo](https://github.com/asura-pro/indigo) 这个项目的后端部分

[![Build Status](https://travis-ci.org/asura-pro/indigo-api.svg?branch=master)](https://travis-ci.org/asura-pro/indigo-api)

### 特性(WIP)
- 底层基于 [Akka](https://github.com/akka/akka) 框架，提供了高并发，分布式，消息驱动这些应用特性和 [Actor](https://en.wikipedia.org/wiki/Actor_model) 抽象模型。Actor 模型在简化系统设计，代码解耦，简化实现和解决一些并发问题上对本项目帮助很大
- RESTful 及 Websocket 的接口 基于 [Playframework](https://github.com/playframework/playframework) 实现, 所有接口都是非阻塞的
- 集成 [Quartz](https://github.com/quartz-scheduler/quartz) 提供定时触发的机制
- 提供操作 [linkerd](https://github.com/linkerd/linkerd) 代理表的 api，以实现动态请求路由的功能

### 构建项目

1. 首先需要确保系统已经安装了 [sbt](https://www.scala-sbt.org/1.x/docs/Setup.html) 构建工具

2. 执行 `sbt dist` 即可打包应用


### Status

Under development
