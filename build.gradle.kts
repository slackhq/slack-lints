// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
import com.diffplug.gradle.spotless.KotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
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
        if (isChecksProject) {
          // Lint forces older Kotlin versions
          // Check via kotlin-for-lint.sh
          languageVersion.set(KotlinVersion.KOTLIN_2_2)
        } else {
          allWarningsAsErrors.set(true)
        }
      }
    }
  }

  tasks.withType<Detekt>().configureEach { jvmTarget = jvmTargetString }

  pluginManager.withPlugin("com.vanniktech.maven.publish") {
    apply(plugin = "org.jetbrains.dokka")

    extensions.configure<DokkaExtension> {
      basePublicationsDirectory.convention(layout.buildDirectory.dir("dokkaDir"))
      dokkaSourceSets.configureEach {
        skipDeprecated.convention(true)
        documentedVisibilities.add(VisibilityModifier.Public)
        reportUndocumented.convention(true)
        perPackageOption {
          matchingRegex.set(".*\\.internal.*")
          suppress.set(true)
        }
        sourceLink {
          localDirectory.convention(layout.projectDirectory.dir("src"))
          val relPath = rootProject.projectDir.toPath().relativize(projectDir.toPath())
          remoteUrl(
            providers.gradleProperty("POM_SCM_URL").map { scmUrl -> "$scmUrl/tree/main/$relPath" }
          )
          remoteLineSuffix.convention("#L")
        }
      }
    }

    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(automaticRelease = true)
      signAllPublications()
    }
  }
}
