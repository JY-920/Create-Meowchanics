// Forge 1.20.1 only. Use with BOTH common files; do not copy the NeoForge adapter too.
ServerEvents.highPriorityData(event => {
  laowuExamples36.register((path, definition) => event.addJson(path, definition))
})
