# nes4j

![Build status](https://github.com/GZYangKui/nes4j/actions/workflows/maven.yml/badge.svg)
![Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue)
<br/>

![nes4j](https://github.com/GZYangKui/nes4j-example/blob/master/video/32328_1673422550.gif?raw=true)

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
    <version>1.0.1</version>
</dependency>
```

+ Gradle(groovy)

```groovy
implementation group: 'cn.navclub', name: 'nes4j-bin', version: '1.0.1'
```

or

```groovy
implementation 'cn.navclub:nes4j-bin:1.0.1'
```

+ Gradle(Kotlin)

```kotlin
implementation("cn.navclub:nes4j-bin:1.0.1")
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
package cn.navclub.nes4j.app.audio;

import cn.navclub.nes4j.bin.apu.Player;

import javax.sound.sampled.*;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;

@SuppressWarnings("all")
public class JavaXAudio implements Player {
    private final byte[] sample;
    private final Line.Info info;
    private final AudioFormat format;
    private final SourceDataLine line;

    private int index;


    public JavaXAudio() throws LineUnavailableException {
        this.sample = new byte[735 * 2];
        this.format = new AudioFormat(44100, 8, 1, false, false);
        this.info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(format);
        line.start();
    }

    //当APU生成一个音频样本时回调该函数
    @Override
    public void output(float sample) {
        var value = int8(Math.round(sample * 0xff));
        this.sample[this.index++] = value;
        if (this.index == this.sample.length) {
            this.index = 0;
            this.line.write(this.sample, 0, this.sample.length);
        }
    }

    //When Nes instance stop or error occurred call this function
    @Override
    public void stop() {
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

## 特便感谢

| 名称                                      | 描述           |
|-----------------------------------------|--------------|
| [Jetbrain](https://www.jetbrains.com/)  | 免费提供全套集成开发环境 |
| [NES forum](https://forums.nesdev.org/) | 提供技术支持       |
