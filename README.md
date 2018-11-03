## Asura

> ⛩`( ⊙ o ⊙ )`

这个是 [Indigo](https://github.com/asura-pro/indigo) 项目的后端

[![Build Status](https://travis-ci.org/asura-pro/indigo-api.svg?branch=master)](https://travis-ci.org/asura-pro/indigo-api)
[![codecov](https://codecov.io/gh/asura-pro/indigo-api/branch/master/graph/badge.svg)](https://codecov.io/gh/asura-pro/indigo-api)
![GitHub release](https://img.shields.io/github/release/asura-pro/indigo-api.svg)
![Maven Central](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/cc/akkaha/asura-core_2.12/maven-metadata.xml.svg)
![GitHub](https://img.shields.io/github/license/asura-pro/indigo-api.svg)

### 特性
- LDAP 认证 和 JWT 签名
- Swagger UI 接口文档
- 底层基于 [Akka](https://github.com/akka/akka) 框架，提供了高并发，分布式，消息驱动这些应用特性和 [Actor](https://en.wikipedia.org/wiki/Actor_model) 抽象模型
- RESTful 及 Websocket 的接口 基于 [Playframework](https://github.com/playframework/playframework) 实现, 所有接口都是非阻塞的
- 集成 [Quartz](https://github.com/quartz-scheduler/quartz) 提供定时触发的机制
- 提供操作 [linkerd](https://github.com/linkerd/linkerd) 代理表的 api，以实现动态请求路由的功能

### 依赖
- java8+
- mysql
- es

### 构建项目

1. 首先需要确保系统已经安装了 [sbt](https://www.scala-sbt.org/1.x/docs/Setup.html) 构建工具

2. 执行 `sbt dist` 即可打包应用

3. 执行 `sbt run` 运行应用
