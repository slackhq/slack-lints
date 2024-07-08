package slack.lint.annotations

/**
 * Annotation to allow suspend Retrofit functions to return Unit.
 * 
 * When applied to a suspend Retrofit function, this annotation permits the function 
 * to have a return type of Unit, which otherwise will be flagged as an issue.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class AllowUnitResponse
