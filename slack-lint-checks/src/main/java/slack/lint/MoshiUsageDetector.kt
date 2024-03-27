// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.getUMethod
import com.android.tools.lint.detector.api.isKotlin
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.util.InheritanceUtil.isInheritor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierTypeOrDefault
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UEnumConstant
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.getContainingUFile
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.kotlin.KotlinUAnnotation
import org.jetbrains.uast.kotlin.KotlinUClassLiteralExpression
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType
import slack.lint.moshi.MoshiLintUtil.hasMoshiAnnotation
import slack.lint.util.MetadataJavaEvaluator
import slack.lint.util.isBoxedPrimitive
import slack.lint.util.isInnerClass
import slack.lint.util.isObjectOrAny
import slack.lint.util.isPlatformType
import slack.lint.util.isString
import slack.lint.util.removeNode
import slack.lint.util.snakeToCamel
import slack.lint.util.sourceImplementation
import slack.lint.util.toScreamingSnakeCase
import slack.lint.util.unwrapSimpleNameReferenceExpression

/** A detector for a number of issues related to Moshi usage. */
// TODO could we detect call expressions to JsonInflater/Gson/Moshi and check if
//  the type going in is a moshi class and meets these requirements?
class MoshiUsageDetector : Detector(), SourceCodeScanner {
  override fun getApplicableUastTypes() = listOf(UClass::class.java)

  override fun createUastHandler(context: JavaContext): UElementHandler {
    val slackEvaluator = MetadataJavaEvaluator(context.file.name, context.evaluator)
    return object : UElementHandler() {
      override fun visitClass(node: UClass) {
        // Enums get checked in both languages because it's easy enough
        if (node.isEnum) {
          checkEnum(context, node)
          return
        }

        if (node.isAnnotationType) {
          checkJsonQualifierAnnotation(context, node)
        }

        context.uastFile?.lang?.let { language -> if (!isKotlin(language)) return }

        val adaptedByAnnotation = node.findAnnotation(FQCN_ADAPTED_BY)
        val jsonClassAnnotation = node.findAnnotation(FQCN_JSON_CLASS)
        if (adaptedByAnnotation != null) {
          if (jsonClassAnnotation != null) {
            // Report both
            context.report(
              ISSUE_DOUBLE_CLASS_ANNOTATION,
              context.getNameLocation(adaptedByAnnotation),
              ISSUE_DOUBLE_CLASS_ANNOTATION.getBriefDescription(TextFormat.TEXT),
              fix().removeNode(context, adaptedByAnnotation.sourcePsi!!),
            )
            context.report(
              ISSUE_DOUBLE_CLASS_ANNOTATION,
              context.getNameLocation(jsonClassAnnotation),
              ISSUE_DOUBLE_CLASS_ANNOTATION.getBriefDescription(TextFormat.TEXT),
              fix().removeNode(context, jsonClassAnnotation.sourcePsi!!),
            )
          }

          validateAdaptedByAnnotation(context, slackEvaluator, adaptedByAnnotation)
          return
        }

        checkSealedClass(node)

        if (jsonClassAnnotation == null) return

        val ktModifierListOwner = node.sourcePsi as KtModifierListOwner
        val visibility = ktModifierListOwner.visibilityModifierTypeOrDefault()
        if (visibility !in REQUIRED_VISIBILITIES) {
          val visibilityElement = ktModifierListOwner.visibilityModifier()
          val location = context.getLocation(visibilityElement!!)
          context.report(
            ISSUE_VISIBILITY,
            location,
            ISSUE_VISIBILITY.getBriefDescription(TextFormat.TEXT),
            quickfixData =
              fix()
                .replace()
                .name("Make 'internal'")
                .range(location)
                .shortenNames()
                .text(visibility.value)
                .with("internal")
                .autoFix()
                .build(),
          )
        }

        // Check that generateAdapter is false
        var usesCustomGenerator = false

        // Check the `JsonClass.generator` property first
        // If generator is an empty string (the default), then it's a class.
        // If it were something else, a generator is claiming to generate something for it
        // and we choose to ignore it in that case.
        // This can sometimes come back as the default empty String and sometimes as null if not
        // defined
        val generatorExpression = jsonClassAnnotation.findDeclaredAttributeValue("generator")
        val generator = generatorExpression?.evaluate() as? String
        if (generator != null) {
          if (generator.isBlank()) {
            context.report(
              ISSUE_BLANK_GENERATOR,
              context.getLocation(generatorExpression),
              ISSUE_BLANK_GENERATOR.getBriefDescription(TextFormat.TEXT),
              quickfixData = null,
            )
          } else {
            usesCustomGenerator = true
            if (generator.startsWith("sealed:")) {
              val typeLabel = generator.removePrefix("sealed:")
              if (typeLabel.isBlank()) {
                context.report(
                  ISSUE_BLANK_TYPE_LABEL,
                  context.getLocation(generatorExpression),
                  ISSUE_BLANK_TYPE_LABEL.getBriefDescription(TextFormat.TEXT),
                  quickfixData = null,
                )
              }
              if (!slackEvaluator.isSealed(node)) {
                context.report(
                  ISSUE_SEALED_MUST_BE_SEALED,
                  context.getNameLocation(node),
                  ISSUE_SEALED_MUST_BE_SEALED.getBriefDescription(TextFormat.TEXT),
                  quickfixData = null,
                )
              }
            }
          }
        }

        val generateAdapter = jsonClassAnnotation.findAttributeValue("generateAdapter")
        if (generateAdapter?.evaluate() != true) {
          context.report(
            ISSUE_GENERATE_ADAPTER_SHOULD_BE_TRUE,
            context.getLocation(generateAdapter as UExpression),
            ISSUE_GENERATE_ADAPTER_SHOULD_BE_TRUE.getBriefDescription(TextFormat.TEXT),
            fix()
              .replace()
              .name("Set to true")
              .text(generateAdapter.asSourceString())
              .with("true")
              .autoFix()
              .build(),
          )
        }

        // If they're using a custom generator, peace of out this
        if (usesCustomGenerator) return

        val isUnsupportedType =
          slackEvaluator.isObject(node) ||
            node.isAnnotationType ||
            node.isInterface ||
            node.isInnerClass(slackEvaluator) ||
            slackEvaluator.isAbstract(node)

        if (isUnsupportedType) {
          if (slackEvaluator.isObject(node)) {
            // Kotlin objects are ok in certain cases, so we give a more specific error message
            context.report(
              ISSUE_OBJECT,
              context.getNameLocation(jsonClassAnnotation),
              ISSUE_OBJECT.getBriefDescription(TextFormat.TEXT),
              fix().removeNode(context, jsonClassAnnotation.sourcePsi!!),
            )
          } else {
            context.report(
              ISSUE_UNSUPPORTED_TYPE,
              context.getNameLocation(jsonClassAnnotation),
              ISSUE_UNSUPPORTED_TYPE.getBriefDescription(TextFormat.TEXT),
              fix().removeNode(context, jsonClassAnnotation.sourcePsi!!),
            )
          }
          return
        } else if (!slackEvaluator.isData(node)) {
          // These should be data classes unless there's a very specific reason not to
          context.report(
            ISSUE_USE_DATA,
            context.getNameLocation(node),
            ISSUE_USE_DATA.getBriefDescription(TextFormat.TEXT),
          )
        }

        // Visit primary constructor properties
        val primaryConstructor =
          node.constructors
            .asSequence()
            .mapNotNull { it.getUMethod() }
            .firstOrNull { it.sourcePsi is KtPrimaryConstructor }

        if (primaryConstructor == null) {
          context.report(
            ISSUE_MISSING_PRIMARY,
            context.getNameLocation(node),
            ISSUE_MISSING_PRIMARY.getBriefDescription(TextFormat.TEXT),
            quickfixData = null,
          )
          return
        }

        val pConstructorPsi = primaryConstructor.sourcePsi as KtPrimaryConstructor
        val constructorVisibility = pConstructorPsi.visibilityModifierTypeOrDefault()
        if (constructorVisibility !in REQUIRED_VISIBILITIES) {
          val visibilityElement = pConstructorPsi.visibilityModifier()!!
          val location = context.getLocation(visibilityElement)
          context.report(
            ISSUE_PRIVATE_CONSTRUCTOR,
            location,
            ISSUE_PRIVATE_CONSTRUCTOR.getBriefDescription(TextFormat.TEXT),
            quickfixData =
              fix()
                .replace()
                .name("Make constructor 'internal'")
                .range(location)
                .shortenNames()
                .text(constructorVisibility.value)
                .with("internal")
                .autoFix()
                .build(),
          )
        }

        val jsonNames = mutableMapOf<String, PsiNamedElement>()
        for (parameter in primaryConstructor.uastParameters) {
          val sourcePsi = parameter.sourcePsi
          val defaultValueExpression =
            if (sourcePsi is KtParameter) {
              sourcePsi.defaultValue
            } else {
              null
            }
          val hasDefaultValue = defaultValueExpression != null
          if (
            parameter.uAnnotations.any { it.qualifiedName == "kotlin.jvm.Transient" } &&
              !hasDefaultValue
          ) {
            ISSUE_TRANSIENT_NEEDS_INIT
            context.report(
              ISSUE_TRANSIENT_NEEDS_INIT,
              context.getLocation(parameter as UElement),
              ISSUE_TRANSIENT_NEEDS_INIT.getBriefDescription(TextFormat.TEXT),
              quickfixData = null,
            )
          }
          if (sourcePsi is KtParameter && sourcePsi.isPropertyParameter()) {
            if (sourcePsi.isMutable) {
              val location = context.getLocation(sourcePsi.valOrVarKeyword!!)
              context.report(
                ISSUE_VAR_PROPERTY,
                location,
                ISSUE_VAR_PROPERTY.getBriefDescription(TextFormat.TEXT),
                quickfixData =
                  fix()
                    .replace()
                    .name("Make ${parameter.name} 'val'")
                    .range(location)
                    .shortenNames()
                    .text("var")
                    .with("val")
                    .autoFix()
                    .build(),
              )
            }
            val paramVisibility = sourcePsi.visibilityModifierTypeOrDefault()
            if (paramVisibility !in REQUIRED_VISIBILITIES) {
              val visibilityElement = sourcePsi.visibilityModifier()!!
              val location = context.getLocation(visibilityElement)
              context.report(
                ISSUE_PRIVATE_PARAMETER,
                location,
                ISSUE_PRIVATE_PARAMETER.getBriefDescription(TextFormat.TEXT),
                quickfixData =
                  fix()
                    .replace()
                    .name("Make ${parameter.name} 'internal'")
                    .range(location)
                    .shortenNames()
                    .text(paramVisibility.value)
                    .with("internal")
                    .autoFix()
                    .build(),
              )
            }

            val propertyAdaptedByAnnotation = parameter.findAnnotation(FQCN_ADAPTED_BY)
            if (propertyAdaptedByAnnotation != null) {
              validateAdaptedByAnnotation(context, slackEvaluator, propertyAdaptedByAnnotation)
            }

            val shouldCheckPropertyType =
              propertyAdaptedByAnnotation == null &&
                // If any JsonQualifier annotations are present, defer to whatever adapter looks at
                // them
                parameter.uAnnotations.none {
                  it.resolve()?.hasAnnotation(FQCN_JSON_QUALIFIER) == true
                }

            if (shouldCheckPropertyType) {
              checkMoshiType(
                context,
                slackEvaluator,
                parameter.type,
                parameter,
                parameter.typeReference!!,
                defaultValueExpression,
                nestedGenericCheck = false,
              )
            }

            // Note: this is a sort of round-about way to get all annotations because just calling
            // UAnnotatedElement.uAnnotations will only return one annotation with a matching name
            // when we want to check for possible duplicates here.
            val jsonAnnotations =
              (parameter.sourcePsi as KtParameter).annotationEntries.mapNotNull { annotationEntry ->
                (annotationEntry.toUElement() as KotlinUAnnotation).takeIf {
                  it.qualifiedName == FQCN_JSON
                }
              }
            var jsonAnnotationToValidate: UAnnotation? = null
            if (jsonAnnotations.isNotEmpty()) {
              if (jsonAnnotations.size != 1) {
                // If we have multiple @Json annotations, likely one is correct (i.e. @Json) and the
                // the others are someone guessing to use a site target but not realizing it's not
                // necessary. So, we suggest removing any with site targets.

                // If, for some reason, _all_ of them are using site targets, then keep one and let
                // a later detector suggest removing the site target.
                val annotationToKeep =
                  jsonAnnotations.find { it.sourcePsi.useSiteTarget == null } ?: jsonAnnotations[0]
                for (annotation in jsonAnnotations) {
                  if (annotation == annotationToKeep) continue
                  // Suggest removing the entire extra annotation
                  context.report(
                    ISSUE_JSON_SITE_TARGET,
                    context.getLocation(annotation),
                    ISSUE_JSON_SITE_TARGET.getBriefDescription(TextFormat.TEXT),
                    quickfixData = fix().removeNode(context, annotation.sourcePsi),
                  )
                }
              } else {
                // Check for site targets, which are redundant
                val jsonAnnotation = jsonAnnotations[0]
                jsonAnnotation.sourcePsi.useSiteTarget?.let { siteTarget ->
                  context.report(
                    ISSUE_JSON_SITE_TARGET,
                    context.getLocation(siteTarget),
                    ISSUE_JSON_SITE_TARGET.getBriefDescription(TextFormat.TEXT),
                    quickfixData =
                      fix()
                        .removeNode(context, jsonAnnotation.sourcePsi, text = "${siteTarget.text}:"),
                  )
                }

                jsonAnnotationToValidate = jsonAnnotation
              }
            }

            validateJsonName(context, parameter, jsonAnnotationToValidate, jsonNames)

            if (jsonAnnotationToValidate == null) {
              // If they already have an `@Json` annotation, don't warn because the IDE will already
              // nag them separately about this with better renaming tools.
              val name = sourcePsi.name ?: continue
              val camelCase = name.snakeToCamel()
              if (name != camelCase) {
                val propKeyword = sourcePsi.valOrVarKeyword!!.text
                context.report(
                  ISSUE_SNAKE_CASE,
                  context.getNameLocation(parameter as UElement),
                  ISSUE_SNAKE_CASE.getBriefDescription(TextFormat.TEXT),
                  quickfixData =
                    fix()
                      .replace()
                      .name("Add @Json(name = \"$name\") and rename to '$camelCase'")
                      .range(context.getLocation(sourcePsi))
                      .shortenNames()
                      .text("$propKeyword $name")
                      .with("@$FQCN_JSON(name = \"$name\") $propKeyword $camelCase")
                      .autoFix()
                      .build(),
                )
              }
            }
          } else {
            if (!hasDefaultValue) {
              context.report(
                ISSUE_PARAM_NEEDS_INIT,
                context.getLocation(parameter as UElement),
                ISSUE_PARAM_NEEDS_INIT.getBriefDescription(TextFormat.TEXT),
                quickfixData = null,
              )
            }
          }
        }
      }

      private fun checkSealedClass(node: UClass) {
        val typeLabelAnnotation = node.getAnnotation(FQCN_TYPE_LABEL)
        val defaultObjectAnnotation = node.getAnnotation(FQCN_DEFAULT_OBJECT)

        if (typeLabelAnnotation != null && defaultObjectAnnotation != null) {
          // Report both
          context.report(
            ISSUE_DOUBLE_TYPE_LABEL,
            context.getNameLocation(typeLabelAnnotation),
            ISSUE_DOUBLE_TYPE_LABEL.getBriefDescription(TextFormat.TEXT),
            fix().removeNode(context, typeLabelAnnotation),
          )
          context.report(
            ISSUE_DOUBLE_TYPE_LABEL,
            context.getNameLocation(defaultObjectAnnotation),
            ISSUE_DOUBLE_TYPE_LABEL.getBriefDescription(TextFormat.TEXT),
            fix().removeNode(context, defaultObjectAnnotation),
          )
          return
        }

        val isTypeLabeled = typeLabelAnnotation != null
        if (isTypeLabeled && node.hasTypeParameters()) {
          context.report(
            ISSUE_GENERIC_SEALED_SUBTYPE,
            context.getLocation((node.sourcePsi as KtClass).typeParameterList!!),
            ISSUE_GENERIC_SEALED_SUBTYPE.getBriefDescription(TextFormat.TEXT),
          )
        }

        val isDefaultObjectLabeled = defaultObjectAnnotation != null

        val isAnnotatedWithTypeLabelOrDefaultObject = isTypeLabeled || isDefaultObjectLabeled

        // Collect all superTypes since interfaces can be sealed too!
        // Filter out types in other packages as the compiler will enforce that for us.
        val currentPackage = node.getContainingUFile()?.packageName ?: return
        val sealedSuperTypeFound =
          node.superTypes
            .asSequence()
            .mapNotNull { slackEvaluator.getTypeClass(it)?.toUElementOfType<UClass>() }
            .filter { it.getContainingUFile()?.packageName == currentPackage }
            .firstOrNull { superType ->
              if (slackEvaluator.isSealed(superType)) {
                val superJsonClassAnnotation = superType.findAnnotation(FQCN_JSON_CLASS)
                if (superJsonClassAnnotation != null) {
                  val generatorExpression = superJsonClassAnnotation.findAttributeValue("generator")
                  val generator = generatorExpression?.evaluate() as? String
                  if (generator != null) {
                    if (generator.startsWith("sealed:")) {
                      if (!isAnnotatedWithTypeLabelOrDefaultObject) {
                        context.report(
                          ISSUE_MISSING_TYPE_LABEL,
                          context.getNameLocation(node),
                          ISSUE_MISSING_TYPE_LABEL.getBriefDescription(TextFormat.TEXT),
                          quickfixData = null,
                        )
                        return@firstOrNull true
                      } else {
                        return@firstOrNull true
                      }
                    }
                  }
                }
              }
              false
            }

        if (sealedSuperTypeFound != null) return

        // If we've reached here and have annotations, something is wrong
        if (isAnnotatedWithTypeLabelOrDefaultObject) {
          if (typeLabelAnnotation != null) {
            context.report(
              ISSUE_INAPPROPRIATE_TYPE_LABEL,
              context.getNameLocation(typeLabelAnnotation),
              ISSUE_INAPPROPRIATE_TYPE_LABEL.getBriefDescription(TextFormat.TEXT),
              quickfixData = fix().removeNode(context, typeLabelAnnotation),
            )
          }
          if (defaultObjectAnnotation != null) {
            context.report(
              ISSUE_INAPPROPRIATE_TYPE_LABEL,
              context.getNameLocation(defaultObjectAnnotation),
              ISSUE_INAPPROPRIATE_TYPE_LABEL.getBriefDescription(TextFormat.TEXT),
              quickfixData = fix().removeNode(context, defaultObjectAnnotation),
            )
          }
        }
      }
    }
  }

  private fun validateAdaptedByAnnotation(
    context: JavaContext,
    evaluator: MetadataJavaEvaluator,
    adaptedByAnnotation: UAnnotation,
  ) {
    // Check the adapter is a valid adapter type
    val adapterAttribute =
      (adaptedByAnnotation.findAttributeValue("adapter") as? KotlinUClassLiteralExpression)
        ?: return
    val targetType = adapterAttribute.type ?: return
    val targetClass = evaluator.getTypeClass(targetType) ?: return
    val implementsAdapter =
      isInheritor(targetClass, true, FQCN_JSON_ADAPTER) ||
        isInheritor(targetClass, true, FQCN_JSON_ADAPTER_FACTORY)
    if (!implementsAdapter) {
      context.report(
        ISSUE_ADAPTED_BY_REQUIRES_ADAPTER,
        context.getLocation(adapterAttribute),
        ISSUE_ADAPTED_BY_REQUIRES_ADAPTER.getBriefDescription(TextFormat.TEXT),
        quickfixData = null,
      )
    } else if (!targetClass.hasAnnotation("androidx.annotation.Keep")) {
      context.report(
        ISSUE_ADAPTED_BY_REQUIRES_KEEP,
        context.getLocation(adapterAttribute),
        ISSUE_ADAPTED_BY_REQUIRES_KEEP.getBriefDescription(TextFormat.TEXT),
        quickfixData = null,
      )
    }
  }

  private fun checkMoshiType(
    context: JavaContext,
    evaluator: MetadataJavaEvaluator,
    psiType: PsiType,
    parameter: UParameter,
    typeNode: UElement,
    defaultValueExpression: KtExpression?,
    nestedGenericCheck: Boolean = true,
  ) {

    if (psiType is PsiPrimitiveType) return

    if (psiType is PsiArrayType) {
      val componentType = psiType.componentType
      val componentTypeName =
        if (componentType is PsiPrimitiveType) {
          componentType.boxedTypeName!!.let {
            if (it == "java.lang.Integer") "Int" else it.removePrefix("java.lang.")
          }
        } else {
          typeNode.sourcePsi!!.text.trim().removePrefix("Array<").removeSuffix(">")
        }
      val replacement = "List<$componentTypeName>"
      context.report(
        ISSUE_ARRAY,
        context.getLocation(typeNode),
        ISSUE_ARRAY.getBriefDescription(TextFormat.TEXT),
        quickfixData =
          fix()
            .replace()
            .name("Change to $replacement")
            .range(context.getLocation(typeNode))
            .shortenNames()
            .text(typeNode.sourcePsi!!.text)
            .with(replacement)
            .autoFix()
            .build()
            .takeUnless { nestedGenericCheck },
      )
      return
    }

    if (psiType !is PsiClassType) return

    val psiClass =
      evaluator.getTypeClass(psiType)
        ?: error(
          "Could not load class for ${psiType.className} on ${parameter.getUastParentOfType<UClass>()!!.name}.${parameter.name}"
        )

    if (psiClass is PsiTypeParameter) return

    if (psiClass.isString() || psiClass.isObjectOrAny() || psiClass.isBoxedPrimitive()) return

    if (psiClass.isEnum) {
      if (!psiClass.hasMoshiAnnotation()) {
        context.report(
          ISSUE_ENUM_PROPERTY_COULD_BE_MOSHI,
          context.getLocation(typeNode),
          ISSUE_ENUM_PROPERTY_COULD_BE_MOSHI.getBriefDescription(TextFormat.TEXT),
          quickfixData = null,
        )
      } else if (
        defaultValueExpression is KtReferenceExpression ||
          defaultValueExpression is KtQualifiedExpression
      ) {
        val defaultValueText = defaultValueExpression.text
        if ("UNKNOWN" in defaultValueText) {
          val oldText = " = $defaultValueText"
          context.report(
            ISSUE_ENUM_PROPERTY_DEFAULT_UNKNOWN,
            context.getLocation(defaultValueExpression),
            ISSUE_ENUM_PROPERTY_DEFAULT_UNKNOWN.getBriefDescription(TextFormat.TEXT),
            quickfixData =
              fix()
                .replace()
                .name("Remove '$oldText'")
                .range(context.getLocation(parameter as UElement))
                .shortenNames()
                .text(oldText)
                .with("")
                .autoFix()
                .build()
                .takeUnless { nestedGenericCheck },
          )
        }
      }
      return
    }

    // Check collections
    val isJsonCollection =
      psiClass.qualifiedName == "java.util.List" ||
        psiClass.qualifiedName == "java.util.Collection" ||
        psiClass.qualifiedName == "java.util.Set" ||
        psiClass.qualifiedName == "java.util.Map"
    if (isJsonCollection) {

      // Do a fuzzy check for mutability. PSI doesn't tell us if they're Kotlin mutable types so we
      // look at the source manually.
      val source = typeNode.sourcePsi!!.text
      val correctedImmutableType =
        when {
          source.startsWith("MutableCollection") -> "Collection"
          source.startsWith("MutableList") -> "List"
          source.startsWith("MutableSet") -> "Set"
          source.startsWith("MutableMap") -> "Map"
          else -> null
        }
      if (correctedImmutableType != null) {
        context.report(
          ISSUE_MUTABLE_COLLECTIONS,
          context.getLocation(typeNode),
          ISSUE_MUTABLE_COLLECTIONS.getBriefDescription(TextFormat.TEXT),
          quickfixData =
            fix()
              .replace()
              .name("Change to $correctedImmutableType")
              .range(context.getLocation(typeNode))
              .shortenNames()
              .text("Mutable$correctedImmutableType<")
              .with("$correctedImmutableType<")
              .autoFix()
              .build()
              .takeUnless { nestedGenericCheck },
        )
      }

      for (typeArg in psiType.parameters) {
        // It's generic, check each generic.
        // TODO we currently report the whole type even if it's just one bad generic. Ideally
        //  we just underline the generic type but as mentioned higher up this is hard in PSI.
        //  Requires separately mapping PSI type args to the modeled versions. Example below:
        //    val argsPsi = (typeNode.sourcePsi!!.children[0] as KtUserType).typeArguments
        checkMoshiType(context, evaluator, typeArg, parameter, typeNode, null)
      }
      return
    }

    when {
      isInheritor(psiClass, "java.util.Collection") -> {
        // A second, more flexible collection check that can suggest better alternatives
        val source = typeNode.sourcePsi!!.text
        val type =
          when {
            "Collection" in source -> "Collection"
            "List" in source -> "List"
            "Set" in source -> "Set"
            else -> null
          }
        val fix =
          if (type == null) {
            null
          } else {
            fix()
              .replace()
              .name("Change to $type")
              .range(context.getLocation(typeNode))
              .shortenNames()
              .text(source.substringBefore("<"))
              .with(type)
              .autoFix()
              .build()
              .takeUnless { nestedGenericCheck }
          }
        context.report(
          ISSUE_NON_MOSHI_CLASS_COLLECTION,
          context.getLocation(typeNode),
          ISSUE_NON_MOSHI_CLASS_COLLECTION.getBriefDescription(TextFormat.TEXT)
            .withHint(psiClass.name),
          quickfixData = fix,
        )
      }
      isInheritor(psiClass, "java.util.Map") -> {
        // A second, more flexible map check that can suggest better alternatives
        context.report(
          ISSUE_NON_MOSHI_CLASS_MAP,
          context.getLocation(typeNode),
          ISSUE_NON_MOSHI_CLASS_MAP.getBriefDescription(TextFormat.TEXT).withHint(psiClass.name),
          quickfixData =
            fix()
              .replace()
              .name("Change to Map")
              .range(context.getLocation(typeNode))
              .shortenNames()
              .text(typeNode.sourcePsi!!.text.substringBefore("<"))
              .with("Map")
              .autoFix()
              .build()
              .takeUnless { nestedGenericCheck },
        )
      }
      psiClass.isPlatformType() -> {
        // Warn because this isn't supported
        // Eventually error when gson interop is out
        context.report(
          ISSUE_NON_MOSHI_CLASS_PLATFORM,
          context.getLocation(typeNode),
          ISSUE_NON_MOSHI_CLASS_PLATFORM.getBriefDescription(TextFormat.TEXT)
            .withHint(psiClass.name),
          quickfixData = null,
        )
      }
      psiClass.hasMoshiAnnotation() -> {
        if (psiType.hasParameters()) {
          for (typeArg in psiType.parameters) {
            // It's generic, check each generic.
            // TODO we currently report the whole type even if it's just one bad generic. Ideally
            //  we just underline the generic type but as mentioned higher up this is hard in PSI
            checkMoshiType(context, evaluator, typeArg, parameter, typeNode, null)
          }
        } else {
          return
        }
      }
      else -> {
        if (psiClass.qualifiedName?.startsWith("slack") == true) {
          // Slack class, suggest making it JsonClass
          context.report(
            ISSUE_NON_MOSHI_CLASS_INTERNAL,
            context.getLocation(typeNode),
            ISSUE_NON_MOSHI_CLASS_INTERNAL.getBriefDescription(TextFormat.TEXT)
              .withHint(psiClass.name),
            quickfixData = null,
          )
        } else {
          // Other class, error not supported
          context.report(
            ISSUE_NON_MOSHI_CLASS_EXTERNAL,
            context.getLocation(typeNode),
            ISSUE_NON_MOSHI_CLASS_EXTERNAL.getBriefDescription(TextFormat.TEXT)
              .withHint(psiClass.name),
            quickfixData = null,
          )
        }
      }
    }
  }

  /**
   * Validates @Json name annotation usage and also checks for `@SerializedName` usage. Returns the
   * Json.name value, if any.
   */
  private fun <T> validateJsonName(
    context: JavaContext,
    member: T,
    jsonAnnotation: UAnnotation?,
    seenNames: MutableMap<String, PsiNamedElement>,
  ) where T : UAnnotated, T : PsiNamedElement {
    var jsonNameValue: String? = null
    if (jsonAnnotation != null) {
      val jsonNameAttr = jsonAnnotation.findAttributeValue("name")
      val jsonName = jsonNameAttr?.evaluate() as? String
      when {
        jsonName == null -> {
          // Ignored, sometimes UAST stubs an incomplete annotation without members
        }
        jsonName.isBlank() -> {
          context.report(
            ISSUE_BLANK_JSON_NAME,
            context.getLocation(jsonNameAttr),
            ISSUE_BLANK_JSON_NAME.getBriefDescription(TextFormat.TEXT),
          )
        }
        jsonName == member.name -> {
          context.report(
            ISSUE_REDUNDANT_JSON_NAME,
            context.getLocation(jsonNameAttr),
            ISSUE_REDUNDANT_JSON_NAME.getBriefDescription(TextFormat.TEXT),
            quickfixData = fix().removeNode(context, jsonAnnotation.sourcePsi!!),
          )
        }
        else -> {
          // Save this to compare to SerializedName later
          jsonNameValue = jsonName
        }
      }
    }

    val jsonName = jsonNameValue ?: member.name!!

    seenNames.put(jsonName, member)?.let { existingMember ->
      // Report both
      context.report(
        ISSUE_DUPLICATE_JSON_NAME,
        context.getNameLocation(member as PsiElement),
        "Name '$jsonName' is duplicated by member '${existingMember.name}'.",
      )
      context.report(
        ISSUE_DUPLICATE_JSON_NAME,
        context.getNameLocation(existingMember),
        "Name '$jsonName' is duplicated by member '${member.name}'.",
      )
    }

    // Check for a leftover `@SerializedName`
    member.findAnnotation(FQCN_SERIALIZED_NAME)?.let { serializedName ->
      val name = serializedName.findAttributeValue("value")?.evaluate() as String
      val alternateCount =
        (serializedName.findAttributeValue("alternate")?.sourcePsi
            as? KtCollectionLiteralExpression)
          ?.getInnerExpressions()
          ?.size ?: -1
      val hasAlternates = alternateCount > 0

      var fix: LintFix? = null
      // If alternates are present, offer no suggestion because there's no equivalent in @Json
      // If both are present and have the same value, offer to delete it
      // If both are present with different values, offer no fix, developer needs to reconcile
      // If only @SerializedName is present, offer to replace with @Json
      if (jsonAnnotation != null) {
        if (jsonNameValue == name && !hasAlternates) {
          fix = fix().removeNode(context, serializedName.sourcePsi!!)
        }
      } else if (!hasAlternates) {
        fix =
          fix()
            .replace()
            .name("Replace with @Json(name = \"$name\")")
            .range(context.getLocation(serializedName))
            .shortenNames()
            .text(serializedName.sourcePsi!!.text)
            .with("@$FQCN_JSON(name = \"$name\")")
            .autoFix()
            .build()
      }
      context.report(
        ISSUE_SERIALIZED_NAME,
        context.getLocation(serializedName),
        ISSUE_SERIALIZED_NAME.getBriefDescription(TextFormat.TEXT),
        quickfixData = fix,
      )
    }
  }

  /** A simple check for `@JsonQualifier` annotation classes. */
  private fun checkJsonQualifierAnnotation(context: JavaContext, node: UClass) {
    if (!node.hasAnnotation(FQCN_JSON_QUALIFIER)) return

    // JsonQualifier annotations must have RUNTIME retention and support targeting FIELD
    // Try both the Kotlin and Java annotations. In Kotlin we try both
    val retentionAnnotationPair: Pair<String, String>
    val targetAnnotationPair: Pair<String, String>
    val isKotlin = isKotlin(node.language)
    if (isKotlin) {
      retentionAnnotationPair = "kotlin.annotation.Retention" to "value"
      targetAnnotationPair = "kotlin.annotation.Target" to "allowedTargets"
    } else {
      retentionAnnotationPair = "java.lang.annotation.Retention" to "value"
      targetAnnotationPair = "java.lang.annotation.Target" to "value"
    }

    node.findAnnotation(retentionAnnotationPair.first)?.let { retentionAnnotation ->
      val retentionValue =
        retentionAnnotation
          .findAttributeValue(retentionAnnotationPair.second)
          ?.unwrapSimpleNameReferenceExpression()
          .run {
            this
              ?: if (isKotlin) {
                // Undefined would be weird but ok, default again is RUNTIME
                return@let
              } else {
                // Always required in Java
                error("Not possible")
              }
          }
      if (retentionValue.identifier != "RUNTIME") {
        val fix =
          if (isKotlin) {
            // Fix is just to remove it because RUNTIME is the default in Kotlin.
            fix().removeNode(context, retentionAnnotation.sourcePsi!!)
          } else {
            // Java we need to change it
            fix()
              .replace()
              .name("Replace with RUNTIME")
              .range(context.getLocation(retentionValue))
              .shortenNames()
              .text(retentionValue.identifier)
              .with("RUNTIME")
              .autoFix()
              .build()
          }
        context.report(
          ISSUE_QUALIFIER_RETENTION,
          context.getLocation(retentionAnnotation.sourcePsi!!),
          ISSUE_QUALIFIER_RETENTION.getBriefDescription(TextFormat.TEXT),
          quickfixData = fix,
        )
      }
    }
      ?: run {
        if (!isKotlin) {
          // Default in Kotlin when unannotated is RUNTIME but not in Java!
          // TODO can we add it for them?
          context.report(
            ISSUE_QUALIFIER_RETENTION,
            context.getNameLocation(node),
            ISSUE_QUALIFIER_RETENTION.getBriefDescription(TextFormat.TEXT),
          )
        }
      }

    // It's ok if Target is missing, default in both languages includes FIELD
    node.findAnnotation(targetAnnotationPair.first)?.let { targetAnnotation ->
      val targetValues =
        when (
          val targetsAttr = targetAnnotation.findAttributeValue(targetAnnotationPair.second)!!
        ) {
          is UCallExpression -> {
            // Covers all of these cases
            // @Target(FIELD, PROPERTY)
            // @Target([FIELD, PROPERTY])
            // @Target({FIELD, PROPERTY})
            targetsAttr.valueArguments
          }
          is UReferenceExpression -> {
            // @Target(FIELD)
            listOf(targetsAttr)
          }
          else -> {
            error("Unrecognized annotation attr value: $targetsAttr")
          }
        }
      if (targetValues.none { it.unwrapSimpleNameReferenceExpression().identifier == "FIELD" }) {
        context.report(
          ISSUE_QUALIFIER_TARGET,
          context.getLocation(targetAnnotation.sourcePsi!!),
          ISSUE_QUALIFIER_TARGET.getBriefDescription(TextFormat.TEXT),
        )
      }
    }
  }

  /**
   * A simple check for a number of issues related to enum use in Moshi.
   *
   * In short - enums serialized with Moshi _must_ meet the following requirements:
   * - Be annotated with `@JsonClass`.
   * - Reserve their first member as `UNKNOWN` for handling unrecognized enums.
   *
   * This lint will attempt to detect if an enum is used with Moshi and check these requirements.
   * They should not generate adapters (i.e. `JsonClass.generateAdapter` should be `false`) but are
   * exempt from that requirement and the requirements of this lint in general if they define a
   * custom `JsonClass.generator` value.
   */
  private fun checkEnum(context: JavaContext, node: UClass) {
    val jsonClassAnnotation = node.findAnnotation(FQCN_JSON_CLASS)
    val constants = node.uastDeclarations.filterIsInstance<UEnumConstant>()
    val hasJsonAnnotatedConstant = constants.any { it.hasAnnotation(FQCN_JSON) }
    val isPresumedMoshi = jsonClassAnnotation != null || hasJsonAnnotatedConstant

    // If it's not annotated and no members have @Json, it's not a moshi class
    if (!isPresumedMoshi) return

    // Check that generateAdapter is false
    var usesCustomGenerator = false
    if (jsonClassAnnotation != null) {
      // Check the `JsonClass.generator` property first
      // If generator is an empty string (the default), then it's a standard enum.
      // If it were something else, a generator is claiming to generate something for it
      // and we choose to ignore it in that case.
      // This can sometimes come back as the default empty String and sometimes as null if not
      // defined
      val generator = jsonClassAnnotation.findAttributeValue("generator")?.evaluate() as? String
      usesCustomGenerator = !generator.isNullOrBlank()
      if (!usesCustomGenerator) {
        val generateAdapter =
          jsonClassAnnotation.findAttributeValue("generateAdapter") as ULiteralExpression
        if (generateAdapter.evaluate() != false) {
          context.report(
            ISSUE_ENUM_JSON_CLASS_GENERATED,
            context.getLocation(generateAdapter),
            ISSUE_ENUM_JSON_CLASS_GENERATED.getBriefDescription(TextFormat.TEXT),
            fix()
              .replace()
              .name("Set to false")
              .text(generateAdapter.asSourceString())
              .with("false")
              .autoFix()
              .build(),
          )
        }
      }
    } else {
      // If an @Json is present but not @JsonClass, suggest it
      context.report(
        ISSUE_ENUM_JSON_CLASS_MISSING,
        context.getNameLocation(node),
        ISSUE_ENUM_JSON_CLASS_MISSING.getBriefDescription(TextFormat.TEXT),
        // TODO can we add it for them?
        quickfixData = null,
      )
    }

    // If they're using a custom generator, peace of out this
    if (usesCustomGenerator) return

    // Visit members, ensure first is UNKNOWN if annotated
    val unknownIndex = constants.indexOfFirst { it.name == "UNKNOWN" }
    if (unknownIndex == -1) {
      context.report(
        ISSUE_ENUM_UNKNOWN,
        context.getNameLocation(node),
        ISSUE_ENUM_UNKNOWN.getBriefDescription(TextFormat.TEXT),
        quickfixData = null, // TODO can we add an enum for them?
      )
    } else {
      val constant = constants[unknownIndex]
      if (unknownIndex != 0) {
        context.report(
          ISSUE_ENUM_UNKNOWN,
          context.getNameLocation(constant),
          ISSUE_ENUM_UNKNOWN.getBriefDescription(TextFormat.TEXT),
          quickfixData = null, // TODO can we reorder it for them?
        )
      } else {
        val jsonAnnotation = constant.getAnnotation(FQCN_JSON)
        if (jsonAnnotation != null) {
          val source = jsonAnnotation.text
          context.report(
            ISSUE_ENUM_ANNOTATED_UNKNOWN,
            context.getNameLocation(node),
            ISSUE_ENUM_ANNOTATED_UNKNOWN.getBriefDescription(TextFormat.TEXT),
            fix()
              .replace()
              .name("Remove @Json")
              .range(context.getLocation(jsonAnnotation))
              .shortenNames()
              .text(source)
              .with("")
              .autoFix()
              .build(),
          )
        }
      }
    }

    val jsonNames = mutableMapOf<String, PsiNamedElement>()
    for ((index, constant) in constants.withIndex()) {
      if (index == unknownIndex) continue

      val jsonAnnotation = constant.findAnnotation(FQCN_JSON)
      validateJsonName(context, constant, jsonAnnotation, jsonNames)

      val name = constant.name
      val screamingSnake = name.toScreamingSnakeCase()
      if (name != screamingSnake) {
        // Only suggest a new Json annotation if one isn't already present
        val fixName: String
        val fixReplacement: String
        if (jsonAnnotation == null) {
          fixName = "Add @Json(name = \"$name\") and rename to '$screamingSnake'"
          fixReplacement = "@$FQCN_JSON(name = \"$name\") $screamingSnake"
        } else {
          fixName = "Rename to '$screamingSnake'"
          fixReplacement = screamingSnake
        }
        context.report(
          ISSUE_ENUM_CASING,
          context.getNameLocation(constant),
          ISSUE_ENUM_CASING.getBriefDescription(TextFormat.TEXT),
          quickfixData =
            fix()
              .replace()
              .name(fixName)
              .range(context.getNameLocation(constant))
              .shortenNames()
              .text(name)
              .with(fixReplacement)
              .autoFix()
              .build(),
        )
      }
    }
  }

  companion object {
    private const val FQCN_ADAPTED_BY = "dev.zacsweers.moshix.adapters.AdaptedBy"
    private const val FQCN_JSON_CLASS = "com.squareup.moshi.JsonClass"
    private const val FQCN_JSON = "com.squareup.moshi.Json"
    private const val FQCN_JSON_ADAPTER = "com.squareup.moshi.JsonAdapter"
    private const val FQCN_JSON_ADAPTER_FACTORY = "com.squareup.moshi.JsonAdapter.Factory"
    private const val FQCN_JSON_QUALIFIER = "com.squareup.moshi.JsonQualifier"
    private const val FQCN_TYPE_LABEL = "dev.zacsweers.moshix.sealed.annotations.TypeLabel"
    private const val FQCN_DEFAULT_OBJECT = "dev.zacsweers.moshix.sealed.annotations.DefaultObject"
    private const val FQCN_SERIALIZED_NAME = "com.google.gson.annotations.SerializedName"

    private val REQUIRED_VISIBILITIES = setOf(KtTokens.PUBLIC_KEYWORD, KtTokens.INTERNAL_KEYWORD)

    // This hint mechanism is solely to help identify nested type arguments in the error message
    // since we can't easily highlight them in nested generics. We can remove this if we figure
    // out a good way to do it.
    private const val HINT = "%HINT%"

    private fun String.withHint(hint: String?): String {
      return replace(HINT, hint.orEmpty())
    }

    private fun createIssue(
      subId: String,
      briefDescription: String,
      explanation: String,
      severity: Severity = Severity.ERROR,
    ): Issue =
      Issue.create(
        "MoshiUsage$subId",
        briefDescription,
        explanation,
        Category.CORRECTNESS,
        6,
        severity,
        implementation = sourceImplementation<MoshiUsageDetector>(),
      )

    private val ISSUE_MISSING_TYPE_LABEL =
      createIssue(
        "MissingTypeLabel",
        "Sealed Moshi subtypes must be annotated with @TypeLabel or @DefaultObject.",
        """
        moshi-sealed requires sealed subtypes to be annotated with @TypeLabel or @DefaultObject. \
        Otherwise, moshi-sealed will fail to compile.
      """
          .trimIndent(),
      )

    private val ISSUE_BLANK_GENERATOR =
      createIssue(
        "BlankGenerator",
        "Don't use blank JsonClass.generator values.",
        """
        The default for JsonClass.generator is "", it's redundant to specify an empty one and an \
        error to specify a blank one.
      """
          .trimIndent(),
      )

    private val ISSUE_BLANK_TYPE_LABEL =
      createIssue(
        "BlankTypeLabel",
        "Moshi-sealed requires a type label specified after the 'sealed:' prefix.",
        "Moshi-sealed requires a type label specified after the 'sealed:' prefix.",
      )

    private val ISSUE_SEALED_MUST_BE_SEALED =
      createIssue(
        "SealedMustBeSealed",
        "Moshi-sealed can only be applied to 'sealed' types.",
        "Moshi-sealed can only be applied to 'sealed' types.",
      )

    private val ISSUE_GENERATE_ADAPTER_SHOULD_BE_TRUE =
      createIssue(
        "GenerateAdapterShouldBeTrue",
        "JsonClass.generateAdapter must be true in order for Moshi code gen to run.",
        "JsonClass.generateAdapter must be true in order for Moshi code gen to run.",
      )

    private val ISSUE_PRIVATE_CONSTRUCTOR =
      createIssue(
        "PrivateConstructor",
        "Constructors in Moshi classes cannot be private.",
        """
        Constructors in Moshi classes cannot be private. \
        Otherwise Moshi cannot invoke it during decoding.
      """
          .trimIndent(),
      )

    private val ISSUE_PRIVATE_PARAMETER =
      createIssue(
        "PrivateConstructorProperty",
        "Constructor parameter properties in Moshi classes cannot be private.",
        """
        Constructor parameter properties in Moshi classes cannot be private. \
        Otherwise these properties will not be visible in serialization.
      """
          .trimIndent(),
      )

    private val ISSUE_TRANSIENT_NEEDS_INIT =
      createIssue(
        "TransientNeedsInit",
        "Transient constructor properties must have default values.",
        """
        Transient constructor property parameters in Moshi classes must have default values. Since \
        these parameters do not participate in serialization, Moshi cannot fulfill them otherwise \
        during construction.
      """
          .trimIndent(),
      )

    private val ISSUE_INAPPROPRIATE_TYPE_LABEL =
      createIssue(
        "InappropriateTypeLabel",
        "Inappropriate @TypeLabel or @DefaultObject annotation.",
        """
        This class declares a @TypeLabel or @DefaultObject annotation but does not appear to \
        subclass a sealed Moshi type. Please remove these annotations or extend the appropriate \
        sealed Moshi-serialized class.
      """
          .trimIndent(),
      )

    private val ISSUE_DOUBLE_TYPE_LABEL =
      createIssue(
        "DoubleTypeLabel",
        "Only use one of @TypeLabel or @DefaultObject.",
        """
        Only one of @TypeLabel and @DefaultObject annotations should be present. It is an error to \
        declare both!
      """
          .trimIndent(),
      )

    private val ISSUE_GENERIC_SEALED_SUBTYPE =
      createIssue(
        "GenericSealedSubtype",
        "Sealed subtypes used with moshi-sealed cannot be generic.",
        """
        Moshi has no way of conveying generics information to sealed subtypes when we create an \
        adapter from the base type. As a result, you should remove generics from this subtype.
      """
          .trimIndent(),
      )

    private val ISSUE_DOUBLE_CLASS_ANNOTATION =
      createIssue(
        "DoubleClassAnnotation",
        "Only use one of @AdaptedBy or @JsonClass.",
        """
        Only one of @AdaptedBy and @JsonClass annotations should be present. It is an error to \
        declare both!
      """
          .trimIndent(),
      )

    private val ISSUE_VISIBILITY =
      createIssue(
        "ClassVisibility",
        "@JsonClass-annotated types must be public, package-private, or internal.",
        """
        @JsonClass-annotated types must be public, package-private, or internal. Otherwise, Moshi
        will not be able to access them from generated adapters.
      """
          .trimIndent(),
      )

    private val ISSUE_PARAM_NEEDS_INIT =
      createIssue(
        "ParamNeedsInit",
        "Constructor non-property parameters in Moshi classes must have default values.",
        """
        Constructor non-property parameters in Moshi classes must have default values. Since these \
        parameters do not participate in serialization, Moshi cannot fulfill them otherwise during \
        construction.
      """
          .trimIndent(),
      )

    private val ISSUE_BLANK_JSON_NAME =
      createIssue(
        "BlankJsonName",
        "Don't use blank names in `@Json`.",
        """
        Blank names in `@Json`, while technically legal, are likely a programmer error and
        likely to cause encoding issues.
      """
          .trimIndent(),
      )

    private val ISSUE_REDUNDANT_JSON_NAME =
      createIssue(
        "RedundantJsonName",
        "Json.name with the same value as the property/enum member name is redundant.",
        """
        Redundant Json.name values can make code noisier and harder to read, consider removing it \
        or suppress this warning with a commented suppression explaining why it's needed.
      """
          .trimIndent(),
        severity = Severity.WARNING,
      )

    private val ISSUE_SERIALIZED_NAME =
      createIssue(
        "SerializedName",
        "Use Moshi's @Json rather than Gson's @SerializedName.",
        """
        @SerializedName is specific to Gson and will not work with Moshi. Replace it with Moshi's \
        equivalent @Json annotation instead (or remove it if @Json is defined already).
      """
          .trimIndent(),
      )

    private val ISSUE_QUALIFIER_RETENTION =
      createIssue(
        "QualifierRetention",
        "JsonQualifiers must have RUNTIME retention.",
        """
        Moshi uses these annotations at runtime, as such they must be available at runtime. In \
        Kotlin, this is the default and you can just remove the Retention annotation. In Java, \
        you must specify it explicitly with @Retention(RUNTIME).
      """
          .trimIndent(),
      )

    private val ISSUE_QUALIFIER_TARGET =
      createIssue(
        "QualifierTarget",
        "JsonQualifiers must include FIELD targeting.",
        """
        Moshi code gen stores these annotations on generated adapter fields, as such they must be \
        allowed on fields. Please specify it explicitly as @Target(FIELD).
      """
          .trimIndent(),
      )

    private val ISSUE_JSON_SITE_TARGET =
      createIssue(
        "RedundantSiteTarget",
        "Use of site-targets on @Json are redundant.",
        """
        Use of site-targets on @Json are redundant and can be removed. Only one, target-less @Json \
        annotation is necessary.
      """
          .trimIndent(),
      )

    private val ISSUE_OBJECT =
      createIssue(
        "Object",
        "Object types cannot be annotated with @JsonClass.",
        """
        Object types cannot be annotated with @JsonClass. The only way they are permitted to \
        participate in Moshi serialization is if they are a sealed subtype of a Moshi sealed \
        class and annotated with `@TypeLabel` or `@DefaultObject` accordingly.
      """
          .trimIndent(),
      )

    private val ISSUE_USE_DATA =
      createIssue(
        "UseData",
        "Model classes should be immutable data classes.",
        """
        @JsonClass-annotated models should be immutable data classes unless there's a very \
        specific reason not to. If you want a custom equals/hashcode/toString impls, make it data \
        anyway and override the ones you need. If you want non-property parameter values, consider \
        making them `@Transient` properties instead with default values.
      """
          .trimIndent(),
      )

    private val ISSUE_UNSUPPORTED_TYPE =
      createIssue(
        "UnsupportedType",
        "This type cannot be annotated with @JsonClass.",
        """
        Abstract, interface, annotation, and inner class types cannot be annotated with @JsonClass. \
        If you intend to decode this with a custom adapter, use @AdaptedBy.
      """
          .trimIndent(),
      )

    private val ISSUE_ADAPTED_BY_REQUIRES_ADAPTER =
      createIssue(
        "AdaptedByRequiresAdapter",
        "@AdaptedBy.adapter must be a JsonAdapter or JsonAdapter.Factory.",
        """
        @AdaptedBy.adapter must be a subclass of JsonAdapter or implement JsonAdapter.Factory.
      """
          .trimIndent(),
      )

    private val ISSUE_ADAPTED_BY_REQUIRES_KEEP =
      createIssue(
        "AdaptedByRequiresKeep",
        "Adapters targeted by @AdaptedBy must have @Keep.",
        """
        Adapters targeted by @AdaptedBy must be annotated with @Keep in order to be reflectively \
        looked up at runtime.
      """
          .trimIndent(),
      )

    private val ISSUE_MISSING_PRIMARY =
      createIssue(
        "MissingPrimary",
        "@JsonClass-annotated types must have a primary constructor or be sealed.",
        """
        @JsonClass-annotated types must have a primary constructor or be sealed. Otherwise, they \
        either have no serializable properties or all the potentially serializable properties are \
        mutable (which is not a case we want!).
      """
          .trimIndent(),
      )

    private val ISSUE_ENUM_PROPERTY_COULD_BE_MOSHI =
      createIssue(
        "EnumPropertyCouldBeMoshi",
        "Consider making enum properties also use Moshi.",
        """
        While we have Gson interop, it's convenient to move enums used by Moshi classes to also \
        use Moshi so that you can leverage built-in support for UNKNOWN handling and get lint \
        checks for it. Simply add `@JsonClass` to this enum class and the appropriate lint will \
        guide you.
      """
          .trimIndent(),
        Severity.WARNING,
      )

    private val ISSUE_ENUM_PROPERTY_DEFAULT_UNKNOWN =
      createIssue(
        "EnumPropertyDefaultUnknown",
        "Suspicious default value to 'UNKNOWN' for a Moshi enum.",
        """
        The enum type of this property is handled by Moshi. This means it will default to \
        'UNKNOWN' if an unrecognized enum is encountered in decoding. At best, it is redundant to \
        default a property to this value. At worst, it can change nullability semantics if the \
        enum should actually allow nullable values or null on absence.
      """
          .trimIndent(),
      )

    private val ISSUE_VAR_PROPERTY =
      createIssue(
        "VarProperty",
        "Moshi properties should be immutable.",
        """
        While var properties are technically possible, they should not be used with Moshi classes \
        as it can lead to asymmetric encoding and thread-safety issues. Consider making this val.
      """
          .trimIndent(),
        Severity.WARNING,
      )

    private val ISSUE_SNAKE_CASE =
      createIssue(
        "SnakeCase",
        "Consider using `@Json(name = ...)` rather than direct snake casing.",
        """
        Moshi offers `@Json` annotations to specify names to use in JSON serialization, similar \
        to Gson's `@SerializedName`. This can help avoid snake_case properties in source directly.
      """
          .trimIndent(),
        Severity.WARNING,
      )

    private val ISSUE_DUPLICATE_JSON_NAME =
      createIssue(
        "DuplicateJsonName",
        "Duplicate JSON names are errors as JSON does not allow duplicate keys in objects.",
        """
        Duplicate JSON names are errors as JSON does not allow duplicate keys in objects.
      """
          .trimIndent(),
      )

    private val ISSUE_NON_MOSHI_CLASS_PLATFORM =
      createIssue(
        "NonMoshiClassPlatform",
        "Platform type '$HINT' is not natively supported by Moshi.",
        """
        The property type is a platform type (i.e. from java.*, kotlin.*, android.*). Moshi only \
        natively supports a small subset of these (primitives, String, and collection interfaces). \
        Otherwise, moshi-gson-interop will hand serialization of this property to Gson, which may \
        or may not handle it. This will eventually become an error after GSON is removed.
      """
          .trimIndent(),
        Severity.WARNING,
      )

    private val ISSUE_ARRAY =
      createIssue(
        "Array",
        "Prefer List over Array.",
        """
        Array types are not supported by Moshi, please use a List instead. Arrays are expensive to \
        manage in JSON as we don't know lengths ahead of time and they are a mutable code smell in \
        what should be immutable value classes. Otherwise, moshi-gson-interop will hand \
        serialization of this property to Gson, which may or may not handle it. This will \
        eventually become an error after GSON is removed.
      """
          .trimIndent(),
        Severity.WARNING,
      )

    private val ISSUE_MUTABLE_COLLECTIONS =
      createIssue(
        "MutableCollections",
        "Use immutable collections rather than mutable versions.",
        """
        While mutable collections are technically possible, they should not be used with Moshi \
        classes as it can lead to asymmetric encoding and thread-safety issues. Please make them \
        immutable versions instead.
      """
          .trimIndent(),
      )

    private val ISSUE_NON_MOSHI_CLASS_COLLECTION =
      createIssue(
        "NonMoshiClassCollection",
        "Concrete Collection type '$HINT' is not natively supported by Moshi.",
        """
        The property type is concrete Collection type (i.e. ArrayList, HashSet, etc). Moshi only \
        natively supports their interface types (List, Set, etc). Consider upcasting to the
        interface type. Otherwise, moshi-gson-interop will hand serialization of this property to \
        Gson, which may or may not handle it.
      """
          .trimIndent(),
        Severity.INFORMATIONAL,
      )

    private val ISSUE_NON_MOSHI_CLASS_MAP =
      createIssue(
        "NonMoshiClassMap",
        "Concrete Map type '$HINT' is not natively supported by Moshi.",
        """
        The property type is concrete Map type (i.e. LinkedHashMap, HashMap, etc). Moshi only \
        natively supports their interface type (Map). Consider upcasting to the interface type. \
        Otherwise, moshi-gson-interop will hand serialization of this property to Gson, which may \
        or may not handle it.
      """
          .trimIndent(),
        Severity.INFORMATIONAL,
      )

    private val ISSUE_NON_MOSHI_CLASS_INTERNAL =
      createIssue(
        "NonMoshiClassInternal",
        "Non-Moshi internal type '$HINT' is not natively supported by Moshi.",
        """
        The property type is an internal type (i.e. slack.*) but is not a Moshi class itself. \
        moshi-gson-interop will hand serialization of this property to Gson, but consider \
        converting this type to Moshi as well to improve runtime performance and consistency.
      """
          .trimIndent(),
        Severity.INFORMATIONAL,
      )

    private val ISSUE_NON_MOSHI_CLASS_EXTERNAL =
      createIssue(
        "NonMoshiClassExternal",
        "External type '$HINT' is not natively supported by Moshi.",
        """
        The property type is an external type (i.e. not a Slack or built-in type). Moshi will try
        to serialize these reflectively, which is not something we want. Either write a custom \
        adapter and annotating this property with `@AdaptedBy` or exclude/remove this type's use. \
        Otherwise, moshi-gson-interop will hand serialization of this property to Gson, which may \
        or may not handle it (also with reflection).
      """
          .trimIndent(),
      )

    val ISSUE_ENUM_JSON_CLASS_GENERATED =
      createIssue(
        "EnumJsonClassGenerated",
        "Enums annotated with @JsonClass must not set `generateAdapter` to true.",
        """
        Enums annotated with @JsonClass do not need to set "generateAdapter" to true and should \
        set it to false.
      """
          .trimIndent(),
      )

    val ISSUE_ENUM_ANNOTATED_UNKNOWN =
      createIssue(
        "EnumAnnotatedUnknown",
        "UNKNOWN members in @JsonClass-annotated enums should not be annotated with @Json",
        """
        UNKNOWN members in @JsonClass-annotated enums should not be annotated with @Json. These \
        members are only used as a fallback and never expected in actual JSON bodies.
      """
          .trimIndent(),
      )

    val ISSUE_ENUM_UNKNOWN =
      createIssue(
        "EnumMissingUnknown",
        "Enums serialized with Moshi must reserve the first member as UNKNOWN.",
        """
        For backward compatibility, enums serialized with Moshi must reserve the first \
        member as "UNKNOWN". We will automatically substitute this when encountering \
        an unrecognized value for this enum during decoding.
      """
          .trimIndent(),
      )

    val ISSUE_ENUM_JSON_CLASS_MISSING =
      createIssue(
        "EnumMissingJsonClass",
        "Enums serialized with Moshi should be annotated with @JsonClass.",
        """
        This enum appears to use Moshi for serialization. Please also add an @JsonClass \
        annotation to it to ensure safe handling with unknown values and R8 optimization.
      """
          .trimIndent(),
      )

    val ISSUE_ENUM_CASING =
      createIssue(
        "EnumCasing",
        "Consider using `@Json(name = ...)` rather than lower casing.",
        """
        Moshi offers `@Json` annotations to specify names to use in JSON serialization, similar \
        to Gson's `@SerializedName`. This can help avoid lower-casing enum properties in source \
        directly.
      """
          .trimIndent(),
        severity = Severity.WARNING,
      )

    // Please keep in alphabetical order for readability
    fun issues(): List<Issue> =
      listOf(
        ISSUE_ADAPTED_BY_REQUIRES_ADAPTER,
        ISSUE_ADAPTED_BY_REQUIRES_KEEP,
        ISSUE_ARRAY,
        ISSUE_BLANK_GENERATOR,
        ISSUE_BLANK_JSON_NAME,
        ISSUE_BLANK_TYPE_LABEL,
        ISSUE_DOUBLE_CLASS_ANNOTATION,
        ISSUE_DOUBLE_TYPE_LABEL,
        ISSUE_DUPLICATE_JSON_NAME,
        ISSUE_ENUM_ANNOTATED_UNKNOWN,
        ISSUE_ENUM_CASING,
        ISSUE_ENUM_JSON_CLASS_GENERATED,
        ISSUE_ENUM_JSON_CLASS_MISSING,
        ISSUE_ENUM_PROPERTY_COULD_BE_MOSHI,
        ISSUE_ENUM_PROPERTY_DEFAULT_UNKNOWN,
        ISSUE_ENUM_UNKNOWN,
        ISSUE_GENERATE_ADAPTER_SHOULD_BE_TRUE,
        ISSUE_GENERIC_SEALED_SUBTYPE,
        ISSUE_INAPPROPRIATE_TYPE_LABEL,
        ISSUE_JSON_SITE_TARGET,
        ISSUE_MISSING_PRIMARY,
        ISSUE_MISSING_TYPE_LABEL,
        ISSUE_MUTABLE_COLLECTIONS,
        ISSUE_QUALIFIER_RETENTION,
        ISSUE_QUALIFIER_TARGET,
        ISSUE_NON_MOSHI_CLASS_COLLECTION,
        ISSUE_NON_MOSHI_CLASS_EXTERNAL,
        ISSUE_NON_MOSHI_CLASS_INTERNAL,
        ISSUE_NON_MOSHI_CLASS_MAP,
        ISSUE_NON_MOSHI_CLASS_PLATFORM,
        ISSUE_OBJECT,
        ISSUE_PARAM_NEEDS_INIT,
        ISSUE_PRIVATE_CONSTRUCTOR,
        ISSUE_PRIVATE_PARAMETER,
        ISSUE_REDUNDANT_JSON_NAME,
        ISSUE_SEALED_MUST_BE_SEALED,
        ISSUE_SERIALIZED_NAME,
        ISSUE_SNAKE_CASE,
        ISSUE_TRANSIENT_NEEDS_INIT,
        ISSUE_UNSUPPORTED_TYPE,
        ISSUE_USE_DATA,
        ISSUE_VAR_PROPERTY,
        ISSUE_VISIBILITY,
      )
  }
}
