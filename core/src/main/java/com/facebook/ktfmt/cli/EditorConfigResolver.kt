package com.facebook.ktfmt.cli

import com.facebook.ktfmt.format.FormattingOptions
import com.facebook.ktfmt.format.TrailingCommaManagementStrategy
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.PropertyTypeRegistry
import org.ec4j.core.Resource
import org.ec4j.core.ResourceProperties
import org.ec4j.core.ResourcePropertiesService
import org.ec4j.core.model.EditorConfig
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.Version.CURRENT

object EditorConfigResolver {

  private val ijContinuationIndentSize: PropertyType<Int> =
      PropertyType.LowerCasingPropertyType(
          "ij_continuation_indent_size",
          "Denotes the continuation indent size. Useful to distinguish code blocks versus continuation lines",
          PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
      )

  private val commaManagementStrategy: PropertyType<TrailingCommaManagementStrategy> =
      PropertyType.LowerCasingPropertyType(
          "ktfmt_trailing_comma_management_strategy",
          "Ktfmt Trailing Comma Management Strategy",
          { _: String, value: String ->
            TrailingCommaManagementStrategy.entries
                .find { it.name.lowercase() == value }
                ?.let { PropertyType.PropertyValue.valid(value, it) }
                ?: PropertyType.PropertyValue.invalid(
                    value,
                    "Unknown ktfmt_trailing_comma_management_strategy value '$value'",
                )
          },
          TrailingCommaManagementStrategy.entries.map { it.name.lowercase() }.toSet(),
      )

  private val propertyTypeRegistry by lazy {
    PropertyTypeRegistry.builder()
        .defaults()
        .type(PropertyType.max_line_length) // missing from defaults?
        .type(ijContinuationIndentSize)
        .type(commaManagementStrategy)
        .build()
  }

  private object Cache : org.ec4j.core.Cache {
    val cached = ConcurrentHashMap<Resource, EditorConfig>()

    override fun get(
        editorConfigFile: Resource,
        loader: EditorConfigLoader,
    ): EditorConfig = cached.computeIfAbsent(editorConfigFile, loader::load)
  }

  private val resourcePropertiesService: ResourcePropertiesService by lazy {
    ResourcePropertiesService.builder()
        .cache(Cache)
        .loader(EditorConfigLoader.of(CURRENT, propertyTypeRegistry))
        .build()
  }

  fun resolveFormattingOptions(file: File, baseOptions: FormattingOptions): FormattingOptions =
      resourcePropertiesService
          .queryProperties(Resource.Resources.ofPath(file.toPath(), Charsets.UTF_8))
          .resolveFormattingOptions(baseOptions)

  private fun ResourceProperties.resolveFormattingOptions(
      baseOptions: FormattingOptions,
  ): FormattingOptions {
    // `max_line_length` may return null to indicate 'off', in which case we keep the base maxWidth
    val maxWidth =
        getValue(PropertyType.max_line_length, baseOptions.maxWidth, false) ?: baseOptions.maxWidth

    // `indent_size` may return null to indicate 'tab', in which case we defer to `tab_width`
    val blockIndent =
        getValue(PropertyType.indent_size, baseOptions.blockIndent, false)
            ?: getValue(PropertyType.tab_width, baseOptions.blockIndent, false)

    val continuationIndent =
        getValue(ijContinuationIndentSize, baseOptions.continuationIndent, false)

    val trailingCommaStrategy =
        getValue(commaManagementStrategy, baseOptions.trailingCommaManagementStrategy, false)

    val resolved =
        baseOptions.copy(
            maxWidth = maxWidth,
            blockIndent = blockIndent,
            continuationIndent = continuationIndent,
            trailingCommaManagementStrategy = trailingCommaStrategy,
        )
    return resolved
  }
}
