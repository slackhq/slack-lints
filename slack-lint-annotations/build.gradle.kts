plugins {
  kotlin("jvm")
}

if (hasProperty("SlackRepositoryUrl"))  {
  apply(plugin = "com.vanniktech.maven.publish")
}