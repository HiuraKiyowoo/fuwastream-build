package com.fuwanime.ui.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.fuwanime.R
import com.fuwanime.ui.components.ListCardShape
import kotlin.math.roundToInt

/**
 * In-app floating player. Renders the SAME ExoPlayer the player screen uses in a second [PlayerView], so
 * leaving the player screen keeps the stream running (no re-buffer, no pause). Drag to move, tap to reopen
 * the full player, X to stop. The drag consumes the pointer, so it never triggers the tab swipe underneath.
 */
@Composable
fun MiniPlayerOverlay(
    player: ExoPlayer?,
    bottomPadding: Dp,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    if (player == null) return
    var playing by remember(player) { mutableStateOf(player.isPlaying) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomPadding + 12.dp),
    ) {
        val density = LocalDensity.current
        val cardW = with(density) { 212.dp.toPx() }
        val cardH = cardW * 9f / 16f
        val maxX = (constraints.maxWidth - cardW).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight - cardH).coerceAtLeast(0f)
        // Starts bottom-end; clamped again whenever the available area changes (rotation, rail, insets).
        var x by remember { mutableStateOf(maxX) }
        var y by remember { mutableStateOf(maxY) }
        LaunchedEffect(maxX, maxY) {
            x = x.coerceIn(0f, maxX)
            y = y.coerceIn(0f, maxY)
        }

        androidx.compose.foundation.layout.Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .width(212.dp)
                .aspectRatio(16f / 9f)
                .clip(ListCardShape)
                .background(Color.Black)
                .clickable { onExpand() }
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        x = (x + drag.x).coerceIn(0f, maxX)
                        y = (y + drag.y).coerceIn(0f, maxY)
                    }
                },
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        this.player = player
                    }
                },
                update = { it.player = player },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.watch_close), tint = Color.White)
            }
            IconButton(
                onClick = { if (playing) player.pause() else player.play() },
                modifier = Modifier.align(Alignment.Center),
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (playing) R.string.watch_pause else R.string.watch_play),
                    tint = Color.White,
                )
            }
        }
    }
}
