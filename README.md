<div align="center">
  <img src="build/icon/nes4j.png" alt="Nes4j logo" width="200" height="auto" />
  <h1>Nes4j</h1>
  <p>Nintendo red and white simulator</p>
</div>

<div align="center">
  <h3>
  <a href="README_zh.md">中文文档</a>
  </h3>
  <h3>
  <a href="https://gitee.com/navigatorcode/nes4j">gitee repository</a>
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

## Project introduction

**nes4j**The Nintendo Red and White Machine Simulator is implemented in Java language, mainly
including [CPU](https://www.nesdev.org/wiki/CPU)、
[PPU](https://www.nesdev.org/wiki/PPU_programmer_reference) and [APU](https://www.nesdev.org/wiki/APU) Three parts.PPU
is a red and white machine
It is difficult to understand the most difficult module.

## Project Structure

```
nes4j
├── app UI module(javafx)
├── bin Simulator core module (CPU/PPU/APU)
└── document Development Documentation
```

## Quick start

### Download project

``` shell
git clone https://gitee.com/navigatorCode/nes4j.git
```

### Start project

```shell
 mvn run
```

## Support cartridge mapper

+ [NROM](https://www.nesdev.org/wiki/NROM)
+ [UxROM](https://www.nesdev.org/wiki/UxROM)
+ [CNROM](https://www.nesdev.org/wiki/INES_Mapper_003)

> More cartridge mapper are being implemented, please wait.

## The core module is introduced separately

> If you feel that the current game output program cannot meet your needs, you can provide PR to us, and we will try
> our best to meet your needs. Another way is to introduce nes4j bin module yourself to achieve video and audio output
> of
> the game.

### First introduce dependency

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

### Then create an NES instance and initialize

+ GameWorld.java

```java

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.Frame;

public class GameWorld {
    public NES create() {
        NES instance = NES.NESBuilder
                .newBuilder()
                //nes game rom
                .file(file)
                //Audio Handler 
                .player(JavaXAudio.class)
                //Game loop callback
                .gameLoopCallback(GameWorld.this::gameLoopCallback)
                .build();
        try {
            //Current method was called current will block current thread until game stop or exception occurred
            instance.execute();
        } catch (Exception e) {
            //todo An error occurred during the game.Once error occurred game immediate stop 
        }
    }

    //This function was callback when a game frame generate
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
    //Currnet fill index
    private int index;
    private Thread thread;
    private volatile boolean stop;
    private final static int SAMPLE_SIZE = 735 * 2;
    //Audio buffer size default 32kb
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

## Participatory contributions

We strongly welcome interested developers to participate in the project construction, and welcome everyone to put
forward valuable suggestions and functional requirements for the project. The project is being actively developed, and
welcome PR 👏。

## Copyright Description

At present, most of the game copyrights in the market are owned by [Nintendo]((https://www.nintendo.com/)).
Do not distribute the game without permission from Nintendo If any infringement is caused thereby, it has nothing
to do with the software. If any infringing material is designed in the software, please send an email to
GZYangKui@126.com Notify me to delete the corresponding infringing materials.

## Development

### document

If you want to write your own simulator or understand the internal structure of the simulator, the following resources
can provide you with some basic knowledge of the simulator:

* [NES Documentation (PDF)](http://nesdev.com/NESDoc.pdf)
* [NES Reference Guide (Wiki)](http://wiki.nesdev.com/w/index.php/NES_reference_guide)
* [6502 CPU Reference](http://www.obelisk.me.uk/6502/reference.html)

### Assembly debugging(Experimental)

> Main interface -> Tool -> Debug

![Assembler](SNAPSHOTS/assemblera.png)

> Snapshot memory view (Memory)
>
![Assembler](SNAPSHOTS/MemoryView.png)

## Special thanks

| name                                    | describe                                                     |
|-----------------------------------------|--------------------------------------------------------------|
| [Jetbrain](https://www.jetbrains.com/)  | Provide a complete set of integrated development environment |
| [NES forum](https://forums.nesdev.org/) | Provide technical support                                    |
