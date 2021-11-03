import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  // Run lint on the lints! https://groups.google.com/g/lint-dev/c/q_TVEe85dgc
  id("com.android.lint")
  id("com.google.devtools.ksp")
}

if (hasProperty("SlackRepositoryUrl"))  {
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

val lintVersion = providers.gradleProperty("lintVersion")
  .forUseAtConfigurationTime()
  .getOrElse("30.0.3")
dependencies {
  compileOnly("com.android.tools.lint:lint-api:$lintVersion")
  compileOnly("com.android.tools.lint:lint-checks:$lintVersion")
  ksp("dev.zacsweers.autoservice:auto-service-ksp:1.0.0")
  implementation("com.google.auto.service:auto-service-annotations:1.0")
  testImplementation("com.android.tools.lint:lint:$lintVersion")
  testImplementation("com.android.tools.lint:lint-tests:$lintVersion")
  testImplementation("com.android.tools:testutils:$lintVersion")
  testImplementation("junit:junit:4.13.2")

  // For IDE linking of APIs
  testCompileOnly("com.squareup.retrofit2:retrofit:2.9.0")
  testCompileOnly("com.slack.eithernet:eithernet:1.0.0")
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
