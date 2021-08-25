## Indigo

> ⛩`( ⊙ o ⊙ )`

这个代码库包含两部分内容, 一部分是 [indigo](https://github.com/ojlm/indigo) 的后端部分. 另一部分 是一个新的集成UI测试工具.

## Indigo UI

这一部分是在 [Karate UI](https://github.com/intuit/karate/tree/master/karate-core) 的基础上做了修改和扩展.

扩展特性包括:

- 支持同时操作多个`driver`, 且自由切换(适用于多人协作, 多人会议, 基于Electron的应用多Webview切换这些场景)
- 新增了一组 [操作系统原生操作指令](https://ojlm.github.io/indigo-docs/#/zh-cn/ui/system)
- 新实现的内置 [android driver](https://ojlm.github.io/indigo-docs/#/zh-cn/ui/android)
- 服务化: 后台运行, 控制多个浏览器或设备, 推送数据, 提高响应速度. 可以直接作为agent 跟浏览器打包容器部署. 本身也是个代理服务器.
- 集成 ffmpeg, opencv, tesseract, 提供图像识别和OCR的基础指令
- 内置一个 [scrcpy](https://github.com/Genymobile/scrcpy) 的 java/scala 实现, 及以此为基础的...
- Web IDE...

如何使用:
> 适用于Windows, Mac, Linux平台. 所有依赖已经打包到一个文件中, 下载即可使用.

1. 需要 java11 及以上运行环境
2. 下载最新发布版本, 链接: https://pan.baidu.com/s/1b7Z47TkaQoSpdcRCaCuUEQ 提取码: e6ym

```bash
# java -jar indigo.jar
 _         _ _
(_)_ _  __| (_)__ _ ___
| | ' \/ _` | / _` / _ \
|_|_||_\__,_|_\__, \___/
              |___/

用法:
indigo [-hVv] [-c=file] [command]

描述:
适用于 Web,Android 的自动化工具...

选项:
  -c, --config=file   通过配置文件启动, 待实现.
                      如果指定了配置文件其他子命令将忽略.
  -v, --verbose       指定多个 -v 选项以打印更多调试信息. 比如, `-v -v -v` or
                        `-vvv`.
  -h, --help          显示帮助信息并退出.
  -V, --version       显示版本信息并退出.

命令:
  karate    运行原生karate指令
  chrome    托管多个chrome来进行UI自动化
  electron  Electron应用UI自动化
  monkey    运行monkey任务
```

### Chrome

```bash
# java -jar indigo.jar chrome -h
托管多个chrome来进行UI自动化

用法:
indigo chrome [-hsVv] [--disable-proxy] [--enable-push-logs]
              [--enable-push-screen] [--enable-push-status] [--enable-server]
              [--headless] [--not-remove-user-data-dir] [--core-count=num]
              [--init-count=num] [--max-count=num] [-p=port] [--push-ip=ip]
              [--push-port=port] [--push-status-interval=secs] [--push-url=url]
              [--user-data-dir=dir] [--user-data-dir-prefix=dir]
              [--vnc-pass=***] [--vnc-ws-port=port] [--options=option[,
              option...]]... [--remote-debugging-port=port[,port...]]...

描述:
托管多个chrome来进行UI测试

选项:
  -p, --server-port=port     本地服务端口. 默认: 8080.
      --enable-server        启动本地服务. 默认启动.
  -s, --start                启动新的浏览器实例. 默认 true. 如果是 `false`, 则会
                               尝试连接`--remote-debugging-port`指定的端口号实
                               例.
      --init-count=num       初始启动的浏览器数量, 默认1个.
      --core-count=num       保持活跃的浏览器数量, 默认1个.
      --max-count=num        允许启动的最大浏览器数量, 默认1个.
      --remote-debugging-port=port[,port...]
                             浏览器远程调试端口, 默认: [9222]. 如果
                               'start=true' 并且仅有一个实例需要启动的时候这个这
                               个选项才有效. 如果 'start=false', 会直接连接到这
                               些端口列表.
      --user-data-dir=dir    浏览器用户目录, 仅启动一个实例时有效
      --not-remove-user-data-dir
                             浏览器关闭时不删除用户目录下的数据
      --user-data-dir-prefix=dir
                             浏览器用户目录前缀, 启动多个浏览器时需要. 默认是
                               `target`目录.
      --headless             启动无头浏览器.
      --options=option[,option...]
                             其他的浏览器启动选项. 例: '--options "--incognito,
                               --mute-audio,--use-fake-ui-for-media-stream,
                               --use-fake-device-for-media-stream"'.
      --disable-proxy        关闭本地代理, 浏览仅允许本地访问, 通过代理可以远程
                               访问, 默认开启.
      --vnc-pass=***         VNC密码. 如果本地启动了 VNC 服务器, 可通过本服务在
                               浏览器中访问
      --vnc-ws-port=port     本地 websockify 端口号. 默认: 5901. Websocket 代
                               理, 通过 noVnc 访问远程桌面.
      --push-ip=ip           本地IP地址, 用于推送到服务器.
      --push-port=port       本地端口号, 用于推送到服务器.
      --push-url=url         服务器连接串, 用于推送信息和接收控制指令, 消息协议
                               请查看文档. 支持四种传输协
                               议:'http','ws','tcp','unix'. 例: `unix:
                               ///var/run/indigo.sock`, `tcp://192.168.1.1:
                               8080`.
      --enable-push-status   开启推送浏览器状态信息到服务器.
      --enable-push-screen   开启推送浏览器屏幕截图到服务器.
      --push-status-interval=secs
                             推送浏览器状态信息的时间间隔, 默认: 30 秒, 屏幕截图
                               间隔为该时间5倍.
      --enable-push-logs     开启推送日志信息, 日志信息包括 Chrome DevTools
                               Protocol 的命令信息和脚本的执行信息.
  -v, --verbose              指定多个 -v 选项以打印更多调试信息. 比如, `-v -v
                               -v` or `-vvv`.
  -h, --help                 显示帮助信息并退出.
  -V, --version              显示版本信息并退出.
```

### Android

```bash
# java -jar indigo.jar android -h

Android 自动化

用法:
indigo android [-hVv] [--always-on-top] [--disable-appium-server]
               [--disable-display] [--disable-scrcpy]
               [--disable-scrcpy-control] [--enable-appium-http]
               [--enable-appium-mjpeg] [--adb-host=host] [--adb-interval=secs]
               [--adb-path=path] [--adb-port=port] [--apk=path]
               [--appium-http-port=port] [--appium-mjpeg-port=port]
               [--bit-rate=num] [--display-id=num] [--max-fps=num] [-p=port]
               [-s=s] [--socket-name=name] [--window-width=num]

描述:
android 自动化

选项:
  -p, --server-port=port     本地服务端口. 默认: 8080.
      --adb-host=host        adb server 的地址. 默认: localhost.
      --adb-port=port        adb server 的端口. 默认: 5037.
      --adb-path=path        adb程序的完整路径地址. 默认使用环境变量中的`adb`.
      --apk=path             要推送的apk文件路径. 默认使用打包好的.
  -s, --serial=s             仅链接指定的序列号设备.
      --adb-interval=secs    设备检查间隔. 默认: 5 秒.
      --disable-display      禁用设备屏幕镜像窗口.
      --always-on-top        使镜像窗口不被其他窗口覆盖.
      --window-width=num     窗口初始宽度. 默认: 280.
      --socket-name=name     Local socket 名称. 默认: asura.
      --disable-appium-server
                             禁用 appium 服务.
      --enable-appium-http   启用原生的 appium http 服务.
      --appium-http-port=port
                             appium http 服务端口号. 默认: 6790.
      --enable-appium-mjpeg  启用原生的 appium mjpeg 服务.
      --appium-mjpeg-port=port
                             appium mjpeg 服务端口号. 默认: 7810.
      --disable-scrcpy       禁用屏幕镜像.
      --disable-scrcpy-control
                             禁用远程控制.
      --bit-rate=num         指定视频码率 bits/s. 默认: 8000000.
      --max-fps=num          最大帧数, Android 10 及以上有效.
      --display-id=num       要镜像的设备显示器id. 默认: 0.
  -v, --verbose              指定多个 -v 选项以打印更多调试信息. 比如, `-v -v
                               -v` or `-vvv`.
  -h, --help                 显示帮助信息并退出.
  -V, --version              显示版本信息并退出
```
