package com.uoa.core.utils

class Event<out T>(val content: Any?) {
    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): Any? {
        if (!hasBeenHandled) {
            hasBeenHandled = true
            return content
        }
        return null
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): Any? = content
}
