// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  // Run lint on the lints! https://groups.google.com/g/lint-dev/c/q_TVEe85dgc
  alias(libs.plugins.lint)
  alias(libs.plugins.ksp)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.buildConfig)
}

val lintKotlinVersion = KotlinVersion(2, 2, 10)

buildConfig {
  packageName("slack.lint")
  useKotlinOutput { internalVisibility = true }
  sourceSets.getByName("test") {
    buildConfigField(
      "kotlin.KotlinVersion",
      "LINT_KOTLIN_VERSION",
      "KotlinVersion(${lintKotlinVersion.major}, ${lintKotlinVersion.minor}, ${lintKotlinVersion.patch})",
    )
  }
}

lint {
  htmlReport = true
  xmlReport = true
  textReport = true
  absolutePaths = false
  checkTestSources = true
  baseline = file("lint-baseline.xml")
  disable += setOf("GradleDependency")
  fatal += setOf("LintDocExample", "LintImplPsiEquals", "UastImplementation")
}

tasks.test {
  // Disable noisy java applications launching during tests
  jvmArgs("-Djava.awt.headless=true")
  maxParallelForks = Runtime.getRuntime().availableProcessors() * 2
}

dependencies {
  compileOnly(libs.bundles.lintApi)
  ksp(libs.autoService.ksp)
  implementation(libs.autoService.annotations)
  testImplementation(libs.bundles.lintTest)
  testImplementation(libs.junit)

  // For IDE linking of APIs
  testImplementation(libs.retrofit)
  testImplementation(libs.eithernet) { exclude(group = "org.jetbrains.kotlin") }
}

val kgpKotlinVersion =
  KotlinVersion.fromVersion(lintKotlinVersion.toString().substringBeforeLast('.'))

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    // Lint forces Kotlin (regardless of what version the project uses), so this
    // forces a matching language level for now. Similar to `targetCompatibility` for Java.
    // This should match the value in LintKotlinVersionCheckTest.kt
    apiVersion.set(kgpKotlinVersion)
    languageVersion.set(kgpKotlinVersion)
  }
}
