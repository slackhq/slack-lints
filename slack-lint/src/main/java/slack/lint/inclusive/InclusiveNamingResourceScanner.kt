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
