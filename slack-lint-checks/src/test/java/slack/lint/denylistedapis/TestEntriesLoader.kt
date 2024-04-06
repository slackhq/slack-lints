package slack.lint.denylistedapis

internal class TestEntriesLoader: DenyListedEntryLoader {
  override val entries: Set<DenyListedEntry> =
    setOf(
      DenyListedEntry(
        className = "slack.lint.TestClass",
        functionName = "run",
        errorMessage = "TestClass.run() is disallowed in tests via external denylist entry."
      )
    )
}