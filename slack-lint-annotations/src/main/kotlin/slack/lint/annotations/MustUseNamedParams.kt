package slack.lint.annotations

/**
 * Callers to this function must named all parameters. This is useful in cases where arguments may
 * change in order and you want to avoid source-breaking changes.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MustUseNamedParams
