// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package slack.lint.inclusive

import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.ResourceXmlDetector
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element

@Suppress("UnstableApiUsage")
class InclusiveNamingResourceScanner : ResourceXmlDetector() {

  private lateinit var blocklist: Set<String>

  override fun beforeCheckRootProject(context: Context) {
    super.beforeCheckRootProject(context)
    blocklist = InclusiveNamingChecker.loadBlocklist(context)
  }

  override fun getApplicableElements(): List<String> = ALL

  override fun visitAttribute(context: XmlContext, attribute: Attr) {
    if (blocklist.isEmpty()) return
    InclusiveNamingChecker.XmlChecker(context, blocklist)
      .check(attribute, attribute.name, "attribute")
  }

  override fun visitDocument(context: XmlContext, document: Document) {
    super.visitDocument(context, document)
    if (blocklist.isEmpty()) return
  }

  override fun visitElement(context: XmlContext, element: Element) {
    if (blocklist.isEmpty()) return
  }
}
