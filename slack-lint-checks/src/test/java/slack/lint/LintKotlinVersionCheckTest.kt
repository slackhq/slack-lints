package slack.lint

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Lint chases Kotlin versions differently than what we declare our build with. This test is just
 * here to catch those for our own awareness and should be updated whenever lint updates its own.
 */
class LintKotlinVersionCheckTest {
  @Test
  fun check() {
    assertThat(KotlinVersion.CURRENT).isEqualTo(KotlinVersion(1, 9, 20))
  }
}
