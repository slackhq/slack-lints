// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  // Run lint on the lints! https://groups.google.com/g/lint-dev/c/q_TVEe85dgc
  alias(libs.plugins.lint)
  alias(libs.plugins.ksp)
}

if (hasProperty("SlackRepositoryUrl")) {
  apply(plugin = "com.vanniktech.maven.publish")
}

lint {
  htmlReport = true
  xmlReport = true
  textReport = true
  absolutePaths = false
  checkTestSources = true
  baseline = file("lint-baseline.xml")
}

dependencies {
  compileOnly(libs.bundles.lintApi)
  ksp(libs.autoService.ksp)
  implementation(libs.autoService.annotations)
  testImplementation(libs.bundles.lintTest)
  testImplementation(libs.junit)

  // For IDE linking of APIs
  testImplementation(libs.retrofit)
  testImplementation(libs.eithernet)
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    // Lint still requires 1.4 (regardless of what version the project uses), so this forces a lower
    // language level for now. Similar to `targetCompatibility` for Java.
    apiVersion = "1.4"
    languageVersion = "1.4"
  }
}
