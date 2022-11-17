// Copyright (C) 2021 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
plugins { kotlin("jvm") }

if (hasProperty("SlackRepositoryUrl")) {
  apply(plugin = "com.vanniktech.maven.publish")
}
