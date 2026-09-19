// NeoForge 1.21.1 only. Use with BOTH common files; do not copy the Forge adapter too.
ServerEvents.generateData('after_mods', event => {
  laowuExamples36.register((path, definition) => event.json(path, definition))
})
