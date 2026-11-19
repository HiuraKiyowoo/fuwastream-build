package com.fuwanime.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * In-app floating player: set when the player screen is left while playback is still running, so the
 * same ExoPlayer keeps streaming inside a small draggable window above the tabs. App-scoped, main thread.
 */
class MiniPlayer {
    var active by mutableStateOf(false)
        private set
    /** Player route to reopen when the mini window is tapped. */
    var route by mutableStateOf<String?>(null)
        private set
    var title by mutableStateOf("")
        private set
    var episodeKey by mutableStateOf<String?>(null)
        private set

    fun show(route: String, title: String, episodeKey: String) {
        this.route = route
        this.title = title
        this.episodeKey = episodeKey
        active = true
    }

    fun hide() {
        active = false
        route = null
        episodeKey = null
    }
}
