package com.backpackingmap.backpackingmap.ui

// NOTE: Remember that full action names are part of our public api

enum class BMIntentAction(private val suffix: String) {
    ShowActiveTrack("show_track");

    fun actionName(): String = "$PREFIX.$suffix"

    companion object {
        const val PREFIX = "com.backpackingmap.action"

        fun fromName(actionName: String): BMIntentAction? {
            if (!actionName.startsWith(PREFIX)) {
                return null
            }
            val suffix = actionName.substring(PREFIX.length + 1)

            for (action in values()) {
                if (action.suffix == suffix) {
                    return action
                }
            }

            return null
        }
    }
}