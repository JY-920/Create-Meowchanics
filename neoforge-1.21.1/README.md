# Create: Meowchanics

“机械动力：老吴学”的 Minecraft 1.21.1 NeoForge 独立源码工程。

## 运行环境

- Minecraft 1.21.1
- NeoForge 21.1.219 或更高的 21.1.x 版本
- Create 6.0.10
- Java 21
- JEI 19.x（可选，用于配方展示）

## 构建

在项目根目录执行：

```powershell
.\gradlew.bat clean build
```

构建产物位于 `build/libs/`，文件名格式为：

```text
create-meowchanics-<版本>-neoforge-1.21.1.jar
```

## 开发运行

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
```

本工程从 Forge 1.20.1 版本独立移植；不要与旧工程共用源码目录或构建产物。
