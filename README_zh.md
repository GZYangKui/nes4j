<div align="center">
  <img src="build/icon/nes4j.png" alt="Nes4j logo" width="200" height="auto" />
  <h1>Nes4j</h1>
  <p>任天堂红白机模拟器</p>
</div>

<div align="center">
  <h3>
  <a href="README.md">English Document</a>
  </h3>
  <h3>
  <a href="https://github.com/GZYangKui/nes4j">github仓库</a>
  </h3>
  <img src="https://github.com/GZYangKui/nes4j/actions/workflows/maven.yml/badge.svg" alt="Build status"/>
  <img src="https://img.shields.io/badge/license-Apache%202.0-blue" alt="Apache 2.0"/>
  <img src="https://img.shields.io/badge/I18n-Support-orange.svg" alt="I18n support"/>
  <img src="https://badgen.net/github/stars/GZYangKui/nes4j?icon=github&color=4ab8a1" alt="stars">
  <img src="https://badgen.net/github/forks/GZYangKui/nes4j?icon=github&color=4ab8a1" alt="forks">
<br/>
</div>



![nes4j](SNAPSHOTS/Main.png)
![DuckTables](SNAPSHOTS/DuckTables.png)
![Super Mario](SNAPSHOTS/Super%20Mario.png)

## 项目介绍

**nes4j**是使用java语言实现任天堂红白机模拟器,主要包括[CPU](https://www.nesdev.org/wiki/CPU)、
[PPU](https://www.nesdev.org/wiki/PPU_programmer_reference)和[APU](https://www.nesdev.org/wiki/APU)三部分组成.其中PPU是红白机
实现难度最大的一个模块,理解起来有点困难.

## 项目结构

```
nes4j
├── app UI模块(javafx)
├── bin 模拟器核心模块(CPU/PPU/APU)
└── document 开发文档
```

## 快速启动

### 下载项目

``` shell
git clone https://gitee.com/navigatorCode/nes4j.git
```

### 启动项目

```shell
 mvn run
```

## 已实现卡带Mapper

+ [NROM](https://www.nesdev.org/wiki/NROM)
+ [UxROM](https://www.nesdev.org/wiki/UxROM)
+ [CNROM](https://www.nesdev.org/wiki/INES_Mapper_003)

> 更多卡带Mapper正在实现中,敬请期待。
>
>

## 自定义音视频输出

> 如果你觉得当前游戏输出程序无法满足你的需求,你可以给我们提PR,我们会尽可能满足你的需求,另外一种方法就是你自己引入nes4j-bin模块自己实现
> 游戏视屏和音频输出

### 首先引入依赖

+ Apache Maven

```xml

<dependency>
    <groupId>cn.navclub</groupId>
    <artifactId>nes4j-bin</artifactId>
    <version>1.0.2</version>
</dependency>
```

+ Gradle(groovy)

```groovy
implementation group: 'cn.navclub', name: 'nes4j-bin', version: '1.0.2'
```

or

```groovy
implementation 'cn.navclub:nes4j-bin:1.0.2'
```

+ Gradle(Kotlin)

```kotlin
implementation("cn.navclub:nes4j-bin:1.0.2")
```

### 创建NES实例并初始化

+ GameWorld.java

```java

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.Frame;

public class GameWorld {
    public NES create() {
        NES instance = NES.NESBuilder
                .newBuilder()
                //nes游戏rom
                .file(file)
                //音频输出程序 
                .player(JavaXAudio.class)
                //Game loop 回调
                .gameLoopCallback(GameWorld.this::gameLoopCallback)
                .build();
        try {
            //一旦当前方法被调用将会阻塞当前线程直到游戏结束或者异常发生
            instance.execute();
        } catch (Exception e) {
            //todo 当异常发生当前游戏立即停止
        }
    }

    //当PPU输出一帧视屏时回调该函数
    private void gameLoopCallback(Frame frame, JoyPad joyPad, JoyPad joyPad1) {

    }
}

```

+ JavaXAudio.java

```java

@SuppressWarnings("all")
public class JavaXAudio implements Player {
    private final byte[] sample;
    private final Line.Info info;
    private final AudioFormat format;
    private final SourceDataLine line;
    private int ldx;
    //当前填充下标
    private int index;
    private Thread thread;
    private volatile boolean stop;
    private final static int SAMPLE_SIZE = 735 * 2;
    //音频默认缓存区大小为32kb
    private final static int DEF_BUF_SIZE = 32 * 1024;

    private static final LoggerDelegate log = LoggerFactory.logger(JavaXAudio.class);


    public JavaXAudio() throws LineUnavailableException {
        this.sample = new byte[DEF_BUF_SIZE];
        this.format = new AudioFormat(44100, 8, 1, false, false);
        this.info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(format);
        line.start();

        CompletableFuture.runAsync((this::exec));
    }

    @Override
    public synchronized void output(byte sample) {
        this.sample[this.index++] = sample;
        if (this.lcalculate() > SAMPLE_SIZE && thread != null) {
            LockSupport.unpark(this.thread);
        }
        index = index % DEF_BUF_SIZE;
    }


    private void exec() {
        var arr = new byte[DEF_BUF_SIZE];
        this.thread = Thread.currentThread();
        while (!this.stop) {
            LockSupport.park();
            final int length;
            synchronized (this) {
                length = lcalculate();
                if ((length + ldx > DEF_BUF_SIZE)) {
                    var tmp = DEF_BUF_SIZE - this.ldx;
                    System.arraycopy(this.sample, this.ldx, arr, 0, tmp);
                    System.arraycopy(this.sample, 0, arr, tmp, this.index);
                } else {
                    System.arraycopy(this.sample, this.ldx, arr, 0, length);
                }
                this.ldx = this.index;
            }
            this.line.write(arr, 0, length);
        }
    }

    private int lcalculate() {
        var len = this.index - this.ldx;
        if (len > 0) {
            return len;
        }
        return DEF_BUF_SIZE - ldx + index;
    }


    @Override
    public void stop() {
        this.stop = true;
        LockSupport.unpark(this.thread);
        this.line.close();
    }
}
```

## 参与贡献

我们强烈欢迎有兴趣的开发者参与到项目建设中来，同时欢迎大家对项目提出宝贵意见建议和功能需求，项目正在积极开发，欢迎 PR 👏。

## 版权说明

目前市场上绝大部分游戏版权为[任天堂](https://www.nintendo.com/)所有,请勿在未取得任天堂授权的情况下私自分发游戏.
如果因此引发的任何侵权行为均与本软件无关.如果本软件中设计任何侵权素材请发送邮件到GZYangKui@126.com通知我删除对应侵权素材.

## 开发文档

如果你也想编写自己的模拟器或想了解模拟器内部结构,以下资源可以给你提供一些模拟器基础知识:

* [NES Documentation (PDF)](http://nesdev.com/NESDoc.pdf)
* [NES Reference Guide (Wiki)](http://wiki.nesdev.com/w/index.php/NES_reference_guide)
* [6502 CPU Reference](http://www.obelisk.me.uk/6502/reference.html)

## 技术交流学习

![qq](document/im_qq.jpg)

## 特别感谢

| 名称                                      | 描述           |
|-----------------------------------------|--------------|
| [Jetbrain](https://www.jetbrains.com/)  | 免费提供全套集成开发环境 |
| [NES forum](https://forums.nesdev.org/) | 提供技术支持       |
