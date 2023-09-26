// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.mocking

import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import slack.lint.BaseSlackLintTest

class MockDetectorOptionsTest : BaseSlackLintTest() {

  override fun lint(): TestLintTask {
    return super.lint()
      .configureOption(
        MockDetector.MOCK_ANNOTATIONS,
        "io.mockk.impl.annotations.MockK,io.mockk.impl.annotations.SpyK"
      )
      .configureOption(
        MockDetector.MOCK_FACTORIES,
        "io.mockk.MockKKt#mockk,io.mockk.MockKKt#mockkClass,io.mockk.MockKKt#mockkObject,io.mockk.MockKKt#spyk"
      )
  }

  override fun getDetector() = MockDetector()

  override fun getIssues() = MockDetector.ISSUES.toList()

  @Test
  fun tests() {
    val source =
      kotlin(
          "test/slack/test/MyTests.kt",
          """
          package slack.test

          import io.mockk.impl.annotations.MockK
          import io.mockk.impl.annotations.SpyK
          import io.mockk.mockk
          import io.mockk.mockkClass
          import io.mockk.mockkObject
          import io.mockk.spyk

          class MyTests {
            @MockK lateinit var fieldDataMock: DataClass
            @SpyK lateinit var fieldDataSpy: DataClass

            fun dataClass() {
              val localMock1: DataClass = mockk()
              val localMock2 = mockk<DataClass>()

              val localSpy1: DataClass = spyk()
              val localSpy2 = spyk<DataClass>()
              val localSpy3 = spyk(localMock1)

              // KClass<DataClass> is not detected, explicit type needed!
              val localClassMock = mockkClass<DataClass>(DataClass::class)
            }

            @MockK lateinit var fieldSealedMock: SealedClass
            @SpyK lateinit var fieldSealedSpy: SealedClass

            fun sealedClass() {
              val localMock1: SealedClass = mockk()
              val localMock2 = mockk<SealedClass>()

              val localSpy1: SealedClass = spyk()
              val localSpy2 = spyk<SealedClass>()
              val localSpy3 = spyk(localMock1)

              // KClass<SealedClass> is not detected, explicit type needed!
              val localClassMock = mockkClass<SealedClass>(SealedClass::class)
            }

            @MockK lateinit var fieldObjectMock: ObjectClass
            @SpyK lateinit var fieldObjectSpy: ObjectClass

            fun objectClass() {
              val localMock1: ObjectClass = mockk()
              val localMock2 = mockk<ObjectClass>()

              val localSpy1: ObjectClass = spyk()
              val localSpy2 = spyk<ObjectClass>()
              val localSpy3 = spyk(localMock1)

              // KClass<ObjectClass> is not detected, explicit type needed!
              val localClassMock = mockkClass<ObjectClass>(ObjectClass::class)

              // Wrong Error: 'kotlin.Unit' is an object, so mocking it should not be necessary [DoNotMockObjectClass]
              // Wrong Warning: platform type 'kotlin.Unit' should not be mocked [DoNotMockPlatformTypes]
              // mockkObject(ObjectClass)
            }

            fun platformTypes() {
              // java.
              mockk<Comparable<String>>()
              mockk<Runnable>()
              // kotlin.
              mockk<FileTreeWalk>()
              mockk<Lazy<String>>()
            }
          }
        """
        )
        .indented()

    lint()
      .files(*stubs(), source)
      .run()
      .expect(
        """
        test/slack/test/MyTests.kt:11: Error: 'slack.test.DataClass' is a data class, so mocking it should not be necessary [DoNotMockDataClass]
          @MockK lateinit var fieldDataMock: DataClass
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:12: Error: 'slack.test.DataClass' is a data class, so mocking it should not be necessary [DoNotMockDataClass]
          @SpyK lateinit var fieldDataSpy: DataClass
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:15: Error: 'slack.test.DataClass' is a data class, so mocking it should not be necessary [DoNotMockDataClass]
            val localMock1: DataClass = mockk()
                                        ~~~~~~~
        test/slack/test/MyTests.kt:16: Error: 'slack.test.DataClass' is a data class, so mocking it should not be necessary [DoNotMockDataClass]
            val localMock2 = mockk<DataClass>()
                             ~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:18: Error: 'slack.test.DataClass' is a data class, so mocking it should not be necessary [DoNotMockDataClass]
            val localSpy1: DataClass = spyk()
                                       ~~~~~~
        test/slack/test/MyTests.kt:19: Error: 'slack.test.DataClass' is a data class, so mocking it should not be necessary [DoNotMockDataClass]
            val localSpy2 = spyk<DataClass>()
                            ~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:20: Error: 'slack.test.DataClass' is a data class, so mocking it should not be necessary [DoNotMockDataClass]
            val localSpy3 = spyk(localMock1)
                            ~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:23: Error: 'slack.test.DataClass' is a data class, so mocking it should not be necessary [DoNotMockDataClass]
            val localClassMock = mockkClass<DataClass>(DataClass::class)
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:41: Error: 'slack.test.ObjectClass' is an object, so mocking it should not be necessary [DoNotMockObjectClass]
          @MockK lateinit var fieldObjectMock: ObjectClass
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:42: Error: 'slack.test.ObjectClass' is an object, so mocking it should not be necessary [DoNotMockObjectClass]
          @SpyK lateinit var fieldObjectSpy: ObjectClass
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:45: Error: 'slack.test.ObjectClass' is an object, so mocking it should not be necessary [DoNotMockObjectClass]
            val localMock1: ObjectClass = mockk()
                                          ~~~~~~~
        test/slack/test/MyTests.kt:46: Error: 'slack.test.ObjectClass' is an object, so mocking it should not be necessary [DoNotMockObjectClass]
            val localMock2 = mockk<ObjectClass>()
                             ~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:48: Error: 'slack.test.ObjectClass' is an object, so mocking it should not be necessary [DoNotMockObjectClass]
            val localSpy1: ObjectClass = spyk()
                                         ~~~~~~
        test/slack/test/MyTests.kt:49: Error: 'slack.test.ObjectClass' is an object, so mocking it should not be necessary [DoNotMockObjectClass]
            val localSpy2 = spyk<ObjectClass>()
                            ~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:50: Error: 'slack.test.ObjectClass' is an object, so mocking it should not be necessary [DoNotMockObjectClass]
            val localSpy3 = spyk(localMock1)
                            ~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:53: Error: 'slack.test.ObjectClass' is an object, so mocking it should not be necessary [DoNotMockObjectClass]
            val localClassMock = mockkClass<ObjectClass>(ObjectClass::class)
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:62: Warning: platform type 'java.lang.Comparable' should not be mocked [DoNotMockPlatformTypes]
            mockk<Comparable<String>>()
            ~~~~~~~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:63: Warning: platform type 'java.lang.Runnable' should not be mocked [DoNotMockPlatformTypes]
            mockk<Runnable>()
            ~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:65: Warning: platform type 'kotlin.io.FileTreeWalk' should not be mocked [DoNotMockPlatformTypes]
            mockk<FileTreeWalk>()
            ~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:66: Warning: platform type 'kotlin.Lazy' should not be mocked [DoNotMockPlatformTypes]
            mockk<Lazy<String>>()
            ~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:26: Error: 'slack.test.SealedClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
          @MockK lateinit var fieldSealedMock: SealedClass
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:27: Error: 'slack.test.SealedClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
          @SpyK lateinit var fieldSealedSpy: SealedClass
          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:30: Error: 'slack.test.SealedClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            val localMock1: SealedClass = mockk()
                                          ~~~~~~~
        test/slack/test/MyTests.kt:31: Error: 'slack.test.SealedClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            val localMock2 = mockk<SealedClass>()
                             ~~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:33: Error: 'slack.test.SealedClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            val localSpy1: SealedClass = spyk()
                                         ~~~~~~
        test/slack/test/MyTests.kt:34: Error: 'slack.test.SealedClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            val localSpy2 = spyk<SealedClass>()
                            ~~~~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:35: Error: 'slack.test.SealedClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            val localSpy3 = spyk(localMock1)
                            ~~~~~~~~~~~~~~~~
        test/slack/test/MyTests.kt:38: Error: 'slack.test.SealedClass' is a sealed type and has a restricted type hierarchy, use a subtype instead. [DoNotMockSealedClass]
            val localClassMock = mockkClass<SealedClass>(SealedClass::class)
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        24 errors, 4 warnings
        """
          .trimIndent()
      )
  }

  private val mockK =
    kotlin(
        "test/io/mockk/impl/annotations/MockK.kt",
        """
    package io.mockk.impl.annotations

    annotation class MockK
    """
      )
      .indented()

  private val spyK =
    kotlin(
        "test/io/mockk/impl/annotations/SpyK.kt",
        """
    package io.mockk.impl.annotations

    annotation class SpyK
    """
      )
      .indented()

  private val mockKExtensions =
    kotlin(
        "test/io/mockk/MockK.kt",
        """
    package io.mockk

    inline fun <reified T : Any> mockk(
        name: String? = null,
        relaxed: Boolean = false,
        vararg moreInterfaces: KClass<*>,
        relaxUnitFun: Boolean = false,
        block: T.() -> Unit = {}
    ): T = TODO()

    inline fun <reified T : Any> spyk(
        name: String? = null,
        vararg moreInterfaces: KClass<*>,
        recordPrivateCalls: Boolean = false,
        block: T.() -> Unit = {}
    ): T = TODO()

    inline fun <reified T : Any> spyk(
        objToCopy: T,
        name: String? = null,
        vararg moreInterfaces: KClass<*>,
        recordPrivateCalls: Boolean = false,
        block: T.() -> Unit = {}
    ): T = TODO()

    inline fun <T : Any> mockkClass(
        type: KClass<T>,
        name: String? = null,
        relaxed: Boolean = false,
        vararg moreInterfaces: KClass<*>,
        relaxUnitFun: Boolean = false,
        block: T.() -> Unit = {}
    ): T = TODO()

    inline fun mockkObject(vararg objects: Any, recordPrivateCalls: Boolean = false): Unit = TODO()
    """
      )
      .indented()

  private val dataClass =
    kotlin(
        """
    package slack.test

    data class DataClass(val foo: String, val list: List<DataClass> = emptyList())
    """
      )
      .indented()

  private val sealedClass =
    kotlin("""
    package slack.test

    sealed class SealedClass
    """).indented()

  private val objectClass =
    kotlin("""
    package slack.test

    object ObjectClass
    """).indented()

  private fun stubs() = arrayOf(mockK, spyK, mockKExtensions, dataClass, sealedClass, objectClass)
}
