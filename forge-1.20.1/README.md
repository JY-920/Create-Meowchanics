# Create: Meowchanics — Forge 1.20.1

Stable Forge source tree for **机械动力：老吴学**.

## Environment

- Minecraft 1.20.1
- Forge 47.4.22
- Create 6.0.8
- Java 17
- JEI 15.x (optional)

## Build

```powershell
.\gradlew.bat clean build
```

The release jar is generated in `build/libs/` as
`create-meowchanics-<version>-forge-1.20.1.jar`.

Dependencies are resolved from their public Maven repositories. Do not add
downloaded mod jars to this source tree.
