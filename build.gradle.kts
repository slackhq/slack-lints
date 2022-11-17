// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.diffplug.gradle.spotless.KotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    mavenCentral()
    google()
    // Last because this proxies jcenter!
    gradlePluginPortal()
  }
  dependencies {
    // We have to declare this here in order for kotlin-facets to be generated in iml files
    // https://youtrack.jetbrains.com/issue/KT-36331
    classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
  }
}

plugins {
  alias(libs.plugins.spotless) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.detekt)
  alias(libs.plugins.lint) apply false
  alias(libs.plugins.ksp) apply false
}

val ktfmtVersion = libs.versions.ktfmt.get().toString()

allprojects {
  repositories {
    google()
    mavenCentral()
  }

  apply(plugin = "com.diffplug.spotless")
  configure<SpotlessExtension> {
    format("misc") {
      target("*.md", ".gitignore")
      trimTrailingWhitespace()
      endWithNewline()
    }
    kotlin {
      target("**/*.kt")
      ktfmt(ktfmtVersion).googleStyle()
      trimTrailingWhitespace()
      endWithNewline()
      licenseHeaderFile(rootProject.file("spotless/spotless.kt"))
      targetExclude("**/spotless.kt", "**/denylistedapis/*.kt")
    }
    // Externally adapted sources that should preserve their license header
    format("kotlinExternal", KotlinExtension::class.java) {
      target("**/denylistedapis/*.kt")
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
        "(import|plugins|buildscript|dependencies|pluginManagement)"
      )
    }
  }
}

subprojects {
  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

    tasks.withType<JavaCompile>().configureEach { options.release.set(11) }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.withType<KotlinCompile>().configureEach {
      kotlinOptions {
        jvmTarget = "11"
        // TODO re-enable once lint uses Kotlin 1.5
        //        allWarningsAsErrors = true
        //        freeCompilerArgs = freeCompilerArgs + listOf("-progressive")
      }
    }
  }

  tasks.withType<Detekt>().configureEach { jvmTarget = "11" }

  pluginManager.withPlugin("com.vanniktech.maven.publish") {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaTask>().configureEach {
      outputDirectory.set(rootDir.resolve("../docs/0.x"))
      dokkaSourceSets.configureEach { skipDeprecated.set(true) }
    }

    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
    }
  }
}
