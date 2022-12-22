# nes4j

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

### 配置参数

```json
{
  /**UI控制键与任天堂手柄映射关系**/
  "mapper": [
    {
      "keyCode": "A",
      "button": "BTN_A"
    },
    {
      "keyCode": "S",
      "button": "BTN_B"
    },
    {
      "keyCode": "UP",
      "button": "BTN_UP"
    },
    {
      "keyCode": "DOWN",
      "button": "BTN_DN"
    },
    {
      "keyCode": "SPACE",
      "button": "BTN_SE"
    },
    {
      "keyCode": "ENTER",
      "button": "BTN_ST"
    },
    {
      "keyCode": "LEFT",
      "button": "BTN_LF"
    },
    {
      "keyCode": "RIGHT",
      "button": "BTN_RT"
    }
  ]
}
```

### 启动项目

```shell
 mvn run
```
或 直接点击[Launcher](app/src/main/java/cn/navclub/nes4j/app/Launcher.java)运行

## 参与贡献

我们强烈欢迎有兴趣的开发者参与到项目建设中来，同时欢迎大家对项目提出宝贵意见建议和功能需求，项目正在积极开发，欢迎 PR 👏。


## 捐赠支持

如果您觉得我们的开源项目对您有帮助，那就请项目开发者们来一杯咖啡☕️吧！当前我们接受来自于**微信**、**支付宝**或者**码云**
的捐赠，请在捐赠时备注自己的昵称或附言。