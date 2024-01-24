// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  // Run lint on the lints! https://groups.google.com/g/lint-dev/c/q_TVEe85dgc
  alias(libs.plugins.lint)
  alias(libs.plugins.ksp)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.mavenShadow)
  alias(libs.plugins.buildConfig)
}

val lintKotlinVersion = KotlinVersion(1, 9, 21)

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
}

val shade: Configuration = configurations.maybeCreate("compileShaded")

configurations.getByName("compileOnly").extendsFrom(shade)

dependencies {
  compileOnly(libs.bundles.lintApi)
  ksp(libs.autoService.ksp)
  implementation(libs.autoService.annotations)
  shade(libs.kotlin.metadata) { exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib") }

  // Dupe the dep because the shaded version is compileOnly in the eyes of the gradle configurations
  testImplementation(libs.kotlin.metadata)
  testImplementation(libs.bundles.lintTest)
  testImplementation(libs.junit)

  // For IDE linking of APIs
  testImplementation(libs.retrofit)
  testImplementation(libs.eithernet)
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

val shadowJar =
  tasks.shadowJar.apply {
    configure {
      archiveClassifier.set("")
      configurations = listOf(shade)
      relocate("kotlinx.metadata", "slack.lint.shaded.kotlinx.metadata")
      transformers.add(ServiceFileTransformer())
    }
  }

artifacts {
  runtimeOnly(shadowJar)
  archives(shadowJar)
}
