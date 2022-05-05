package slack.lint

import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class UseAnvilForMultibindingDetectorTest : BaseSlackLintTest() {
    override fun getDetector(): Detector = UseAnvilForMultibindingDetector()

    override fun getIssues(): List<Issue> = UseAnvilForMultibindingDetector.issues

    override val skipTestModes: Array<TestMode> =
        arrayOf(TestMode.FULLY_QUALIFIED, TestMode.IMPORT_ALIAS)

    @Test
    fun `Has all ContributesMultibinding on direct implementation`() {
        lint().files(
            ANVIL_STUBS,
            MUST_USE_ANVIL_INTERFACES_STUB,
            kotlin(
                """
                package slack.test
                import slack.commons.android.persistence.cachebuster.CacheFileLogoutAware
                import slack.commons.android.persistence.cachebuster.CacheResetAware
                import com.squareup.anvil.annotations.ContributesMultibinding
                
                @ContributesMultibinding(CacheFileLogoutAware::class)
                @ContributesMultibinding(boundType = CacheResetAware::class)
                class TestDao: CacheResetAware, CacheFileLogoutAware
                """.trimIndent()
            )
        ).run().expectClean()
    }

    @Test
    fun `Has all ContributesMultibinding for the super type`() {
        lint().files(
            ANVIL_STUBS,
            MUST_USE_ANVIL_INTERFACES_STUB,
            kotlin(
                """
                package slack.test
                import slack.commons.android.persistence.cachebuster.CacheFileLogoutAware
                import slack.commons.android.persistence.cachebuster.CacheResetAware
                import com.squareup.anvil.annotations.ContributesMultibinding
                
                abstract class AbstractDao: CacheResetAware, CacheFileLogoutAware
                
                interface DaoInterface: CacheResetAware, CacheFileLogoutAware

                @ContributesMultibinding(CacheFileLogoutAware::class)
                @ContributesMultibinding(boundType = CacheResetAware::class)
                class TestDao: BaseDao

                @ContributesMultibinding(CacheFileLogoutAware::class)
                @ContributesMultibinding(boundType = CacheResetAware::class)
                class TestDao: DaoInterface
                """.trimIndent()
            )
        ).run().expectClean()
    }

    @Test
    fun `Doesn't raise error on abstract class or interface`() {
        lint().files(
            ANVIL_STUBS,
            MUST_USE_ANVIL_INTERFACES_STUB,
            kotlin(
                """
                package slack.test
                import slack.commons.android.persistence.cachebuster.CacheFileLogoutAware
                import slack.commons.android.persistence.cachebuster.CacheResetAware
                import com.squareup.anvil.annotations.ContributesMultibinding
                
                abstract class AbstractDao: CacheResetAware, CacheFileLogoutAware
                
                interface DaoInterface: CacheResetAware, CacheFileLogoutAware
                """.trimIndent()
            )
        ).run().expectClean()
    }

    @Test
    fun `Missing ContributesMultibinding`() {
        lint().files(
            ANVIL_STUBS,
            MUST_USE_ANVIL_INTERFACES_STUB,
            kotlin(
                """
                package slack.test
                import slack.commons.android.persistence.cachebuster.CacheResetAware

                class TestDao: CacheResetAware
                """.trimIndent()
            )
        ).run()
            .expectErrorCount(1)
            .expectContains("Consider using @ContributesMultibinding instead of writing a binding method for CacheResetAware")
    }

    @Test
    fun `Only has ContributesMultibinding for another boundType`() {
        lint().files(
            ANVIL_STUBS,
            MUST_USE_ANVIL_INTERFACES_STUB,
            kotlin(
                """
                package slack.test
                import slack.commons.android.persistence.cachebuster.CacheFileLogoutAware
                import slack.commons.android.persistence.cachebuster.CacheResetAware
        
                @ContributesMultibinding(boundType = CacheFileLogoutAware::class)
                class TestDao: CacheResetAware
                """.trimIndent()
            )
        ).run()
            .expectErrorCount(1)
            .expectContains("Consider using @ContributesMultibinding instead of writing a binding method for CacheResetAware")
    }

    companion object {
        private val ANVIL_STUBS = kotlin(
            """
      package com.squareup.anvil.annotations

      @Repeatable
      annotation class ContributesMultibinding(
          val boundType: KClass<*> = Unit::class
        )
        """.trimIndent()
        )

        private val MUST_USE_ANVIL_INTERFACES_STUB = kotlin(
            """
      package slack.commons.android.persistence.cachebuster

      interface CacheResetAware
      interface CacheFileLogoutAware

      """.trimIndent()
        )
    }
}