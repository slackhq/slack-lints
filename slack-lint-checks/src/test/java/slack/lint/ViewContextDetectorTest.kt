// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

@Suppress("UnstableApiUsage")
class ViewContextDetectorTest : BaseSlackLintTest() {
  @Test
  fun test_customViewInternalCaller() {
    lint()
      .files(loadStub("ViewContextDetectorTestCustomViewInternalCaller.java"))
      .run()
      .expect(
        """
          src/main/java/ViewContextDetectorTestCustomViewInternalCaller.java:11: Error: Unsafe cast of Context to Activity [CastingViewContextToActivity]
              Activity a = (Activity) getContext();
                           ~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun test_externalCallerOnView() {
    lint()
      .files(loadStub("ViewContextDetectorTestExternalCallerOnView.java"))
      .run()
      .expect(
        """
          src/main/java/ViewContextDetectorTestExternalCallerOnView.java:12: Error: Unsafe cast of Context to Activity [CastingViewContextToActivity]
              Activity activity = (Activity) view.getContext();
                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun test_externalCallerOnCustomView() {
    lint()
      .files(loadStub("ViewContextDetectorTestExternalCallerOnCustomView.java"))
      .run()
      .expect(
        """
          src/main/java/ViewContextDetectorTestExternalCallerOnCustomView.java:12: Error: Unsafe cast of Context to Activity [CastingViewContextToActivity]
              Activity activity = (Activity) view.getContext();
                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
          1 errors, 0 warnings
        """
          .trimIndent()
      )
  }

  @Test
  fun test_contentProvider() {
    lint().files(loadStub("ViewContextDetectorTestContentProvider.java")).run().expectClean()
  }

  override fun getDetector(): Detector {
    return ViewContextDetector()
  }

  override fun getIssues(): List<Issue> {
    return listOf(*ViewContextDetector.issues)
  }

  /*@Test
  // need to improve detector to catch errors when the cast occurs later.
  public void test_shouldFailButDoesNot() {
    lint()
        .files(
            java("" +
                "package foo;\n" +
                "import android.app.Activity;\n" +
                "import android.content.Context;\n" +
                "import android.util.AttributeSet;\n" +
                "import android.widget.TextView;\n" +
                "public class Example {\n" +
                "  TextView view;\n" +
                "  public Example(TextView v) {\n" +
                "    view = v;\n" +
                "  }\n" +
                "  public void bar() {\n" +
                "    Context context = view.getContext();\n" +
                "    Activity a = (Activity) context;\n" +
                "  }\n" +
                "}\n"
            )
        )
        .issues(ViewContextDetector.ISSUE_VIEW_CONTEXT_CAST)
        .run()
        .expectClean();
  }*/
}
