/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.ktfmt.nativeImage

import com.oracle.svm.core.annotate.Substitute
import com.oracle.svm.core.annotate.TargetClass
import com.oracle.svm.core.annotate.TargetElement
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Substitution that lets `kotlinc`'s infrastructure work under SVM by skipping
 * `registerApplicationExtensionPointsAndExtensionsFrom`, which is incompatible and not needed
 * anyway.
 *
 * Only used by the Native Image build; never loaded on the JVM.
 */
@Suppress("unused")
@TargetClass(KotlinCoreEnvironment.Companion::class)
internal class KotlinCoreEnvironmentCompanion {
  @Substitute
  @TargetElement(name = "registerApplicationExtensionPointsAndExtensionsFrom")
  private fun stubbedRegisterApplicationExtensionPointsAndExtensionsFrom(
      configuration: CompilerConfiguration,
      configFilePath: String,
  ) {
    // Nothing at this time.
  }
}
