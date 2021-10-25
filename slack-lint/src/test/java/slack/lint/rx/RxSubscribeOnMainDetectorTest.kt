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
package slack.lint.rx

import org.junit.Test
import slack.lint.BaseSlackLintTest

class RxSubscribeOnMainDetectorTest : BaseSlackLintTest() {

  private val rxJavaJar3 = rxJavaJar3()

  private val androidSchedulers = java(
    """
      package io.reactivex.rxjava3.android.schedulers;

      import io.reactivex.rxjava3.core.Scheduler;

      public final class AndroidSchedulers {
          public static Scheduler mainThread() {
              return null;
          }
      }
    """
  ).indented()

  override fun getDetector() = RxSubscribeOnMainDetector()
  override fun getIssues() = listOf(RxSubscribeOnMainDetector.ISSUE)

  @Test
  fun subscribeOnMain_fullyQualified_fails_java() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        java(
          """
            package com.slack.lint;

            import io.reactivex.rxjava3.core.Observable;
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

            public class Foo {
              public void bar(Observable obs) {
                obs.subscribeOn(AndroidSchedulers.mainThread());
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.java:8: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              obs.subscribeOn(AndroidSchedulers.mainThread());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.java line 8: Replace with observeOn():
          @@ -8 +8
          -     obs.subscribeOn(AndroidSchedulers.mainThread());
          +     obs.observeOn(AndroidSchedulers.mainThread());
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnMain_fullyQualified_fails_kotlin() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Observable
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

            object Foo {
              fun bar(obs: Observable<Any>) {
                obs.subscribeOn(AndroidSchedulers.mainThread())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:8: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              obs.subscribeOn(AndroidSchedulers.mainThread())
                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.kt line 8: Replace with observeOn():
          @@ -8 +8
          -     obs.subscribeOn(AndroidSchedulers.mainThread())
          +     obs.observeOn(AndroidSchedulers.mainThread())
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnMain_staticImport_fails_java() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        java(
          """
            package com.slack.lint;

            import io.reactivex.rxjava3.core.Observable;
            import static io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread;

            public class Foo {
              public void bar(Observable obs) {
                obs.subscribeOn(mainThread());
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.java:8: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              obs.subscribeOn(mainThread());
                  ~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.java line 8: Replace with observeOn():
          @@ -8 +8
          -     obs.subscribeOn(mainThread());
          +     obs.observeOn(mainThread());
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnMain_staticImport_fails_kotlin() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Observable
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread

            class Foo {
              fun bar(obs: Observable<Any>) {
                obs.subscribeOn(mainThread())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:8: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              obs.subscribeOn(mainThread())
                  ~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.kt line 8: Replace with observeOn():
          @@ -8 +8
          -     obs.subscribeOn(mainThread())
          +     obs.observeOn(mainThread())
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnMain_outsideAssignment_field_fails_java() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        java(
          """
            package com.slack.lint;

            import io.reactivex.rxjava3.core.Observable;
            import io.reactivex.rxjava3.core.Scheduler;
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

            public class Foo {
              private final Scheduler scheduler = AndroidSchedulers.mainThread();
              public void bar(Observable obs) {
                obs.subscribeOn(scheduler);
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.java:10: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              obs.subscribeOn(scheduler);
                  ~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.java line 10: Replace with observeOn():
          @@ -10 +10
          -     obs.subscribeOn(scheduler);
          +     obs.observeOn(scheduler);
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnMain_outsideAssignment_local_fails_java() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        java(
          """
            package com.slack.lint;

            import io.reactivex.rxjava3.core.Observable;
            import io.reactivex.rxjava3.core.Scheduler;
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

            public class Foo {
              public void bar(Observable obs) {
                Scheduler scheduler = AndroidSchedulers.mainThread();
                obs.subscribeOn(scheduler);
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.java:10: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              obs.subscribeOn(scheduler);
                  ~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.java line 10: Replace with observeOn():
          @@ -10 +10
          -     obs.subscribeOn(scheduler);
          +     obs.observeOn(scheduler);
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnMain_outsideAssignment_fails_field_kotlin() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Observable
            import io.reactivex.rxjava3.core.Scheduler
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

            object Foo {
              private val scheduler = AndroidSchedulers.mainThread()
              fun bar(obs: Observable<Any>) {
                obs.subscribeOn(scheduler)
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:10: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              obs.subscribeOn(scheduler)
                  ~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.kt line 10: Replace with observeOn():
          @@ -10 +10
          -     obs.subscribeOn(scheduler)
          +     obs.observeOn(scheduler)
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnMain_outsideAssignment_fails_local_kotlin() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Observable
            import io.reactivex.rxjava3.core.Scheduler
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

            object Foo {
              fun bar(obs: Observable<Any>) {
                val scheduler = AndroidSchedulers.mainThread()
                obs.subscribeOn(scheduler)
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:10: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              obs.subscribeOn(scheduler)
                  ~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.kt line 10: Replace with observeOn():
          @@ -10 +10
          -     obs.subscribeOn(scheduler)
          +     obs.observeOn(scheduler)
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnIo_passes_java() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        java(
          """
            package com.slack.lint;

            import io.reactivex.rxjava3.core.Observable;
            import io.reactivex.rxjava3.schedulers.Schedulers;

            public class Foo {
              public void bar(Observable obs) {
                obs.subscribeOn(Schedulers.io());
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expectClean()
  }

  @Test
  fun subscribeOnIo_passes_kotlin() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Observable
            import io.reactivex.rxjava3.schedulers.Schedulers

            object Foo {
              fun bar(obs: Observable<Any>) {
                obs.subscribeOn(Schedulers.io())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expectClean()
  }

  @Test
  fun subscribeOnIo_staticImport_passes_java() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        java(
          """
            package com.slack.lint;

            import io.reactivex.rxjava3.core.Observable;
            import io.reactivex.rxjava3.schedulers.Schedulers.io;

            public class Foo {
              public void bar(Observable obs) {
                obs.subscribeOn(io());
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .allowCompilationErrors() // Until AGP 7.1.0 https://groups.google.com/g/lint-dev/c/BigCO8sMhKU
      .run()
      .expectClean()
  }

  @Test
  fun subscribeOnIo_staticImport_passes_kotlin() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Observable
            import io.reactivex.rxjava3.schedulers.Schedulers.io

            object Foo {
              fun bar(obs: Observable<Any>) {
                obs.subscribeOn(io())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expectClean()
  }

  @Test
  fun subscribeOnIo_outsideAssignment_passes_java() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        java(
          """
            package com.slack.lint;

            import io.reactivex.rxjava3.core.Observable;
            import io.reactivex.rxjava3.core.Scheduler;
            import io.reactivex.rxjava3.schedulers.Schedulers;

            public class Foo {
              private final Scheduler scheduler = Schedulers.io();
              public void bar(Observable obs) {
                obs.subscribeOn(scheduler);
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expectClean()
  }

  @Test
  fun subscribeOnIo_outsideAssignment_passes_kotlin() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Observable
            import io.reactivex.rxjava3.schedulers.Schedulers

            object Foo {
              private val scheduler: Scheduler = Schedulers.io()
              fun bar(obs: Observable<Any>) {
                obs.subscribeOn(scheduler)
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expectClean()
  }

  @Test
  fun subscribeOnMain_flowable_fails() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Flowable
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

            object Foo {
              fun bar(flow: Flowable<Any>) {
                flow.subscribeOn(AndroidSchedulers.mainThread())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:8: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              flow.subscribeOn(AndroidSchedulers.mainThread())
                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.kt line 8: Replace with observeOn():
          @@ -8 +8
          -     flow.subscribeOn(AndroidSchedulers.mainThread())
          +     flow.observeOn(AndroidSchedulers.mainThread())
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnIo_flowable_passes() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Flowable
            import io.reactivex.rxjava3.schedulers.Schedulers

            object Foo {
              fun bar(flow: Flowable<Any>) {
                flow.subscribeOn(Schedulers.io())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expectClean()
  }

  @Test
  fun subscribeOnMain_maybe_fails() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Maybe
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

            object Foo {
              fun bar(maybe: Maybe<Any>) {
                maybe.subscribeOn(AndroidSchedulers.mainThread())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:8: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              maybe.subscribeOn(AndroidSchedulers.mainThread())
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.kt line 8: Replace with observeOn():
          @@ -8 +8
          -     maybe.subscribeOn(AndroidSchedulers.mainThread())
          +     maybe.observeOn(AndroidSchedulers.mainThread())
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnIo_maybe_passes() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Maybe
            import io.reactivex.rxjava3.schedulers.Schedulers

            object Foo {
              fun bar(maybe: Maybe<Any>) {
                maybe.subscribeOn(Schedulers.io())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expectClean()
  }

  @Test
  fun subscribeOnMain_single_fails() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Single
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

            object Foo {
              fun bar(single: Single<Any>) {
                single.subscribeOn(AndroidSchedulers.mainThread())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:8: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              single.subscribeOn(AndroidSchedulers.mainThread())
                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.kt line 8: Replace with observeOn():
          @@ -8 +8
          -     single.subscribeOn(AndroidSchedulers.mainThread())
          +     single.observeOn(AndroidSchedulers.mainThread())
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnIo_single_passes() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Single
            import io.reactivex.rxjava3.schedulers.Schedulers

            object Foo {
              fun bar(single: Single<Any>) {
                single.subscribeOn(Schedulers.io())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expectClean()
  }

  @Test
  fun subscribeOnMain_completable_fails() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Completable
            import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

            object Foo {
              fun bar(completable: Completable) {
                completable.subscribeOn(AndroidSchedulers.mainThread())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .run()
      .expect(
        """
          src/com/slack/lint/Foo.kt:8: Error: This will make the code for the initial subscription (above this line) run on the main thread. You probably want observeOn(AndroidSchedulers.mainThread()). [SubscribeOnMain]
              completable.subscribeOn(AndroidSchedulers.mainThread())
                          ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """.trimIndent()
      )
      .expectFixDiffs(
        """
          Fix for src/com/slack/lint/Foo.kt line 8: Replace with observeOn():
          @@ -8 +8
          -     completable.subscribeOn(AndroidSchedulers.mainThread())
          +     completable.observeOn(AndroidSchedulers.mainThread())
        """.trimIndent()
      )
  }

  @Test
  fun subscribeOnIo_completable_passes() {
    lint()
      .files(
        rxJavaJar3,
        androidSchedulers,
        kotlin(
          """
            package com.slack.lint

            import io.reactivex.rxjava3.core.Flowable
            import io.reactivex.rxjava3.schedulers.Schedulers

            object Foo {
              fun bar(completable: Completable) {
                completable.subscribeOn(Schedulers.io())
              }
            }
          """
        ).indented()
      )
      .issues(RxSubscribeOnMainDetector.ISSUE)
      .allowCompilationErrors() // Until AGP 7.1.0 https://groups.google.com/g/lint-dev/c/BigCO8sMhKU
      .run()
      .expectClean()
  }
}
