package com.canolabs.movieshelf.domain.model

/**
 * A validated search query.
 *
 * [text] is what the user typed (trimmed, inner whitespace collapsed) and is used for display and for
 * the API request. [key] is the normalized cache identity: two submissions that only differ in case
 * or spacing share the same cached result set and the same recent-search entry.
 */
class SearchQuery private constructor(
    val text: String,
    val key: String,
) {
    override fun equals(other: Any?): Boolean = other is SearchQuery && other.key == key
    override fun hashCode(): Int = key.hashCode()
    override fun toString(): String = "SearchQuery($text)"

    companion object {
        private val WHITESPACE = Regex("\\s+")

        /** Returns `null` for blank input. */
        fun from(raw: String): SearchQuery? {
            val text = raw.trim().replace(WHITESPACE, " ")
            if (text.isEmpty()) return null
            return SearchQuery(text = text, key = text.lowercase())
        }
    }
}
