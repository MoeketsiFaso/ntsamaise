package com.moeketsifaso.ntsamaise

/** Splits long passages without dropping characters or splitting UTF-16 surrogate pairs. */
internal fun speechChunks(text: String, maxLength: Int): List<String> {
    require(maxLength >= 2)
    if (text.isBlank()) return emptyList()
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        var end = minOf(start + maxLength, text.length)
        if (end < text.length) {
            if (text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) end--
            val boundary = (end - 1 downTo start + (end - start) / 2)
                .firstOrNull { text[it].isWhitespace() }
            if (boundary != null) end = boundary + 1
        }
        chunks += text.substring(start, end)
        start = end
    }
    return chunks
}

/** One capture per user request; cancelled/inactive captures cannot overwrite a newer result. */
internal class TextCaptureRequests {
    private var nextId = 0L
    private var active: Long? = null
    private var pending: Long? = null

    @Synchronized
    fun request(): Long? {
        if (active != null) return null
        val id = ++nextId
        active = id
        pending = id
        return id
    }

    @Synchronized
    fun take(): Long? = pending.also { pending = null }

    @Synchronized
    fun complete(id: Long): Boolean {
        if (active != id) return false
        active = null
        return true
    }

    @Synchronized
    fun cancel() {
        active = null
        pending = null
    }
}
