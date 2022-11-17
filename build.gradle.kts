/*
 * Copyright (C) 2021 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.diffplug.gradle.spotless.KotlinExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import com.vanniktech.maven.publish.MavenPublishPluginExtension
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
    val ktlintVersion = "0.41.0"
    val ktlintUserData = mapOf("indent_size" to "2", "continuation_indent_size" to "2")
    kotlin {
      target("**/*.kt")
      ktlint(ktlintVersion).userData(ktlintUserData)
      trimTrailingWhitespace()
      endWithNewline()
      licenseHeaderFile(rootProject.file("spotless/spotless.kt"))
      targetExclude("**/spotless.kt", "**/denylistedapis/*.kt")
    }
    // Externally adapted sources that should preserve their license header
    format("kotlinExternal", KotlinExtension::class.java) {
      target("**/denylistedapis/*.kt")
      ktlint(ktlintVersion).userData(ktlintUserData)
      trimTrailingWhitespace()
      endWithNewline()
    }
    kotlinGradle {
      ktlint(ktlintVersion).userData(ktlintUserData)
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
    configure<JavaPluginExtension> {
      toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
      }
    }

    tasks.withType<JavaCompile>().configureEach {
      options.release.set(11)
    }
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

  tasks.withType<Detekt>().configureEach {
    jvmTarget = "11"
  }

  pluginManager.withPlugin("com.vanniktech.maven.publish") {
    apply(plugin = "org.jetbrains.dokka")

    tasks.named<DokkaTask>("dokkaHtml") {
      outputDirectory.set(rootDir.resolve("../docs/0.x"))
      dokkaSourceSets.configureEach {
        skipDeprecated.set(true)
      }
    }

    configure<MavenPublishPluginExtension> {
      // Prevent publishing to MavenCentral
      sonatypeHost = null
    }

    // Add our maven repository repo
    configure<PublishingExtension> {
      val url = providers.gradleProperty("SlackRepositoryUrl")
        .get()
      repositories {
        maven {
          name = "SlackRepository"
          setUrl(url)
          credentials(PasswordCredentials::class.java)
        }
      }
    }
  }
}
