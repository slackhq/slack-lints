// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

@Suppress("UnstableApiUsage")
class DeprecatedSqlUsageDetectorTest : BaseSlackLintTest() {

  override fun getDetector(): Detector = DeprecatedSqlUsageDetector()

  override fun getIssues(): MutableList<Issue> = mutableListOf(DeprecatedSqlUsageDetector.ISSUE)

  @Test
  fun testJavaInspection() {
    val deprecatedJavaExample =
      java(
          """
          package foo;

          import android.database.sqlite.SQLiteDatabase;

          public static class SqlUsageTestFailure {
            public static void delete(SQLiteDatabase db) {
              db.execSQL("DROP TABLE IF EXISTS foo");
            }
          }
        """
        )
        .indented()

    lint()
      .files(deprecatedJavaExample)
      .run()
      .expect(
        """
          src/foo/SqlUsageTestFailure.java:7: Warning: All SQL querying should be performed using SqlDelight [DeprecatedSqlUsage]
              db.execSQL("DROP TABLE IF EXISTS foo");
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          0 errors, 1 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun testKotlinInspection() {
    val deprecatedKotlinExample =
      kotlin(
          """
          package foo

          import android.database.sqlite.SQLiteDatabase

          object SqlUsageTestFailure {
            fun delete(db: SQLiteDatabase) {
              db.execSQL("DROP TABLE IF EXISTS foo")
            }
          }
        """
        )
        .indented()

    lint()
      .files(deprecatedKotlinExample)
      .run()
      .expect(
        """
          src/foo/SqlUsageTestFailure.kt:7: Warning: All SQL querying should be performed using SqlDelight [DeprecatedSqlUsage]
              db.execSQL("DROP TABLE IF EXISTS foo")
              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          0 errors, 1 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun testInnocentJava() {
    val innocentJavaExample =
      java(
          """
          package foo;

          import android.database.sqlite.SQLiteDatabase;

          public static class SqlUsageTestFailure {
            public static void delete(SQLiteDatabase db) {
              db.getVersion();
            }
          }
        """
        )
        .indented()

    lint().files(innocentJavaExample).run().expectClean()
  }

  @Test
  fun testInnocentKotlin() {
    val innocentKotlinExample =
      kotlin(
          """
          package foo

          object SqlUsageTestFailure {
            fun delete(db: SupportSQLiteDatabase) {
              db.getVersion()
              FeatureFlagStore().update("foo")
            }
          }

          class FeatureFlagStore {
            fun update(feature: String) = Unit
          }
        """
        )
        .indented()

    lint().files(innocentKotlinExample).run().expectClean()
  }
}
