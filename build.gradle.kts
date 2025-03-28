// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.diffplug.gradle.spotless.KotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.dokka)
  alias(libs.plugins.detekt)
  alias(libs.plugins.lint) apply false
  alias(libs.plugins.ksp) apply false
}

dokka {
  dokkaPublications.html {
    outputDirectory.set(rootDir.resolve("docs/api/0.x"))
    includes.from(project.layout.projectDirectory.file("README.md"))
  }
}

val ktfmtVersion = libs.versions.ktfmt.get()

allprojects {
  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
    format("misc") {
      target("*.md", ".gitignore")
      trimTrailingWhitespace()
      endWithNewline()
    }

    val externalSourceGlobs =
      arrayOf("**/denylistedapis/*.kt", "**/ExceptionMessageDetector*.kt", "**/util/Names.kt")

    kotlin {
      target("**/*.kt")
      ktfmt(ktfmtVersion).googleStyle()
      trimTrailingWhitespace()
      endWithNewline()
      licenseHeaderFile(rootProject.file("spotless/spotless.kt"))
      targetExclude("**/spotless.kt", *externalSourceGlobs)
    }
    // Externally adapted sources that should preserve their license header
    format("kotlinExternal", KotlinExtension::class.java) {
      target(*externalSourceGlobs)
      ktfmt(ktfmtVersion).googleStyle()
      trimTrailingWhitespace()
      endWithNewline()
    }
    kotlinGradle {
      ktfmt(ktfmtVersion).googleStyle()
      trimTrailingWhitespace()
      endWithNewline()
      licenseHeaderFile(
        rootProject.file("spotless/spotless.kt"),
        "(import|plugins|buildscript|dependencies|pluginManagement)",
      )
    }
  }
}

val jdk = libs.versions.jdk.get().toInt()
val lintJvmTargetString: String = libs.versions.lintJvmTarget.get()
val runtimeJvmTargetString: String = libs.versions.runtimeJvmTarget.get()

subprojects {
  val isChecksProject = path == ":slack-lint-checks"
  val jvmTargetString =
    if (isChecksProject) {
      lintJvmTargetString
    } else {
      runtimeJvmTargetString
    }
  val jvmTargetInt = jvmTargetString.toInt()
  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      toolchain { languageVersion.set(JavaLanguageVersion.of(jdk)) }
    }

    tasks.withType<JavaCompile>().configureEach { options.release.set(jvmTargetInt) }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.withType<KotlinCompile>().configureEach {
      compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jvmTargetString))
        // TODO re-enable on checks if lint ever targets latest kotlin versions
        if (isChecksProject) {
          // Lint forces Kotlin 1.9 still
          languageVersion.set(KotlinVersion.KOTLIN_1_9)
        } else {
          allWarningsAsErrors.set(true)
        }
      }
    }
  }

  tasks.withType<Detekt>().configureEach { jvmTarget = jvmTargetString }

  pluginManager.withPlugin("com.vanniktech.maven.publish") {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaTaskPartial>().configureEach {
      outputDirectory.set(layout.buildDirectory.dir("docs/partial"))
      dokkaSourceSets.configureEach { skipDeprecated.set(true) }
    }

    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
    }
  }
}
