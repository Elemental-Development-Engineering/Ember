package com.elementaldevelopment.diagnostics.internal

internal object Limits {
    const val MAX_TAG_LENGTH = 40
    const val MAX_MESSAGE_LENGTH = 240
    const val MAX_USER_NOTE_LENGTH = 1000
    const val MAX_ATTRIBUTE_KEY_LENGTH = 30
    const val MAX_ATTRIBUTE_VALUE_LENGTH = 120
    const val MAX_ATTRIBUTES_PER_ENTRY = 8
    const val MAX_THROWABLE_MESSAGE_LENGTH = 300
    const val TRIMMED_SUFFIX = "… [trimmed]"
}

internal fun trimToMaxLength(input: String, maxLength: Int): String {
    if (input.length <= maxLength) return input
    val cutoff = maxLength - Limits.TRIMMED_SUFFIX.length
    if (cutoff <= 0) return Limits.TRIMMED_SUFFIX.take(maxLength)
    return input.take(cutoff) + Limits.TRIMMED_SUFFIX
}

internal fun trimAttributes(
    attributes: Map<String, String>,
): Map<String, String> {
    return attributes.entries
        .take(Limits.MAX_ATTRIBUTES_PER_ENTRY)
        .associate { (key, value) ->
            trimToMaxLength(key, Limits.MAX_ATTRIBUTE_KEY_LENGTH) to
                trimToMaxLength(value, Limits.MAX_ATTRIBUTE_VALUE_LENGTH)
        }
}
