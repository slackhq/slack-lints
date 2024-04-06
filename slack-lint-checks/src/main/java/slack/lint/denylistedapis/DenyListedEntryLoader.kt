package slack.lint.denylistedapis

interface DenyListedEntryLoader {
  val entries: Set<DenyListedEntry>
}
