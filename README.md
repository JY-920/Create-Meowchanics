# Create: Meowchanics

**机械动力：老吴学** is a playful Minecraft add-on that turns cats into an
absurd Create-powered production, logistics, and combat system.

## Source trees

| Directory | Minecraft | Loader | Create | Java | Status |
| --- | --- | --- | --- | --- | --- |
| [`forge-1.20.1`](forge-1.20.1/) | 1.20.1 | Forge 47.4.22 | 6.0.8 | 17 | Stable release line |
| [`neoforge-1.21.1`](neoforge-1.21.1/) | 1.21.1 | NeoForge 21.1.x | 6.0.10 | 21 | Port in active development |

The two projects are intentionally independent. Run Gradle from the source tree
for the Minecraft version you want to build.

## Branch policy

- `main` contains release-ready code. Changes here are limited to verified bug
  fixes, compatibility fixes, documentation, and release preparation.
- `develop` is the integration branch for new gameplay, models, recipes, and
  other feature work.
- Published builds are marked with signed or annotated version tags such as
  `v1.0.1`.

Pull requests that add features should target `develop`. A tested fix can be
promoted from `develop` to `main` when preparing a release.

## Building

Forge 1.20.1:

```powershell
cd forge-1.20.1
.\gradlew.bat clean build
```

NeoForge 1.21.1:

```powershell
cd neoforge-1.21.1
.\gradlew.bat clean build
```

Build artifacts are written to each project's `build/libs/` directory. Gradle
downloads the public development dependencies; no PCL instance or bundled
third-party mod jars are required.

## Required mods

- Minecraft Forge or NeoForge for the selected source tree
- Create
- Flywheel and Ponder versions required by Create
- JEI is optional and enables recipe displays

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. Bug
reports should include the Minecraft version, loader version, Create version,
the complete crash report, and the relevant `latest.log` section.

## Third-party notices

Attribution for upstream projects and adapted portions is documented in
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md). Additional license files
required by dependencies are retained in the corresponding source trees and
packaged mod resources.

## License

Source code, recipe/data files, build scripts, and documentation are available
under the [MIT License](LICENSE.md). Original textures, models, animations,
audio, and other artistic assets are All Rights Reserved. Third-party material
continues to use its original license; see
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for attribution.
