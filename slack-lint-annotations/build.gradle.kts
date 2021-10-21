plugins {
  kotlin("jvm")
}

if (hasProperty("slack.repositoryUrl"))  {
  apply(plugin = "com.vanniktech.maven.publish")
}