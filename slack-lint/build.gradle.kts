import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  // Run lint on the lints! https://groups.google.com/g/lint-dev/c/q_TVEe85dgc
  id("com.android.lint")
}

if (hasProperty("slack.repositoryUrl"))  {
  apply(plugin = "com.vanniktech.maven.publish")
}

lintOptions {
  htmlReport = true
  xmlReport = true
  textReport = true
  isAbsolutePaths = false
  isCheckTestSources = true
  baselineFile = file("lint-baseline.xml")
}

val lintVersion = if (hasProperty("lintVersion")) property("lintVersion")!!.toString() else "30.0.2"
dependencies {
  compileOnly("com.android.tools.lint:lint-api:$lintVersion")
  compileOnly("com.android.tools.lint:lint-checks:$lintVersion")
  testImplementation("com.android.tools.lint:lint:$lintVersion")
  testImplementation("com.android.tools.lint:lint-tests:$lintVersion")
  testImplementation("com.android.tools:testutils:$lintVersion")
  testImplementation("junit:junit:4.13.2")

  // For IDE linking of APIs
  testCompileOnly("com.squareup.retrofit2:retrofit:2.9.0")
  testCompileOnly("com.slack.eithernet:eithernet:1.0.0")
}

tasks.withType<Jar>().configureEach {
  manifest {
    attributes("Lint-Registry-v2" to "slack.lint.SlackIssueRegistry")
  }
}

tasks.withType<KotlinCompile>()
  .matching { !it.name.contains("test", ignoreCase = true) }
  .configureEach {
    kotlinOptions {
      // Lint still requires 1.4 (regardless of what version the project uses), so this forces a lower
      // language level for now. Similar to `targetCompatibility` for Java.
      apiVersion = "1.4"
      languageVersion = "1.4"
    }
  }
