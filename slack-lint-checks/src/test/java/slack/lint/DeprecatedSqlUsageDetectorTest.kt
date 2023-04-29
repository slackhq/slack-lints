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

  private val sqliteDatabaseClass =
    java(
      """
        package android.database.sqlite;

        public class SQLiteDatabase {
            public int delete(String table, String whereClause, String[] whereArgs) {
                return 0;
            }

            public void	execSQL(String sql) { }

            public void	execSQL(String sql, Object[] bindArgs) { }

            public long	insert(String table, String nullColumnHack, ContentValues values) {
                return 0;
            }

            public long	insertOrThrow(String table, String nullColumnHack, ContentValues values) {
                return 0;
            }

            public long	insertWithOnConflict(String table, String nullColumnHack,
                                                ContentValues initialValues,
                                                int conflictAlgorithm) {
                return 0;
            }

            public Cursor query(boolean distinct, String table, String[] columns, String selection,
                                String[] selectionArgs, String groupBy, String having,
                                String orderBy, String limit) {
                return null;
            }

            public Cursor query(String table, String[] columns, String selection,
                                String[] selectionArgs, String groupBy, String having,
                                String orderBy, String limit) {
                return null;
            }

            public Cursor query(boolean distinct, String table, String[] columns, String selection,
                                String[] selectionArgs, String groupBy, String having,
                                String orderBy, String limit,
                                CancellationSignal cancellationSignal) {
                return null;
            }

            public Cursor query(String table, String[] columns, String selection,
                                String[] selectionArgs, String groupBy, String having,
                                String orderBy) {
                return null;
            }

            public long update(String table, ContentValues values, String whereClause,
                                String[] whereArgs) {
                return 0;
            }

            public long updateWithOnConflict(String table, ContentValues values, String whereClause,
                                                String[] whereArgs, int conflictAlgorithm) {
                return 0;
            }
        }
      """
    )

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
      .files(deprecatedJavaExample, sqliteDatabaseClass)
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
      .files(deprecatedKotlinExample, sqliteDatabaseClass)
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

    lint().files(innocentJavaExample, sqliteDatabaseClass).run().expectClean()
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

    lint().files(innocentKotlinExample, sqliteDatabaseClass).run().expectClean()
  }
}
