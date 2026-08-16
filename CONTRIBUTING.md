# Contributing

Thank you for helping improve Create: Meowchanics.

## Choosing a branch

- Use `develop` for new features, balance changes, models, animations, recipes,
  refactors, and ports.
- Use `main` only for small, verified release fixes and documentation changes.
- Keep loader-specific work inside `forge-1.20.1/` or `neoforge-1.21.1/`.

## Active development target

Unless a task explicitly requests a port, new features are implemented and
tested only in `forge-1.20.1/` on the `develop` branch. The NeoForge source tree
is updated later as a separate, deliberate porting task; feature commits must
not make incidental NeoForge changes.

Create a short-lived branch from the appropriate base, for example
`feature/cat-logistics` or `fix/dedicated-server`, and open a pull request back
to that base branch.

## Before submitting

1. Build the affected project with its bundled Gradle wrapper.
2. Test both single-player and a dedicated server when common code changed.
3. Do not commit `run/`, `.gradle/`, `build/`, logs, crash reports, downloaded
   dependency jars, worlds, credentials, or launcher files.
4. Keep client-only Minecraft classes out of common/server class-loading paths.
5. Explain user-visible changes and list the versions used for testing.

By contributing, you agree that your contribution may be distributed under the
repository's project license.
