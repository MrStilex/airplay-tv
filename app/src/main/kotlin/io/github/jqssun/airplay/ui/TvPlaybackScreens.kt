package io.github.jqssun.airplay.ui

import androidx.annotation.OptIn as AndroidxOptIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jqssun.airplay.bridge.NativeBridge
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.material3.buttons.NextButton
import androidx.media3.ui.compose.material3.buttons.PlayPauseButton
import androidx.media3.ui.compose.material3.buttons.PreviousButton
import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import io.github.jqssun.airplay.ui.gestures.ServerVolumeState
import io.github.jqssun.airplay.viewmodel.MainViewModel
import kotlin.math.roundToInt

// Мягкая фиолетовая палитра экрана музыки
private val SBg = Color(0xFF0B0714)
private val SText = Color(0xF2FFFFFF)
private val SDim = Color(0x99D6BEFF)
private val SSoft = Color(0xCCEBDCFF)

// Палитра экрана «Подключение…»
private val PBg = Color(0xFF0E131C)
private val PCard = Color(0xFF151D2B)
private val PBorder = Color(0xFF263247)
private val PAccent = Color(0xFF00E5FF)
private val PText = Color(0xFFF4F7FB)
private val PDim = Color(0xFF9AA6B8)

/** Тёмная тема Material, чтобы стандартные кнопки и ползунки media3 были в цветах экрана. */
@Composable
private fun TvDark(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = SSoft, onPrimary = SBg, secondary = SSoft,
        surface = SBg, onSurface = SText, onSurfaceVariant = SDim,
        surfaceVariant = Color(0x1AFFFFFF), background = SBg, onBackground = SText
    ),
    content = content
)

/** Мягкая подсветка выбранной пультом кнопки: полупрозрачный круг вместо рамки. */
private fun Modifier.softFocus(): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this.onFocusChanged { focused = it.isFocused || it.hasFocus }
        .background(Color.White.copy(alpha = if (focused) 0.22f else 0f), CircleShape)
}

/** Музыка по AirPlay на весь экран телевизора: ASCII-акулы фоном, мягкая обложка и управление. */
@Composable
@AndroidxOptIn(UnstableApi::class)
fun TvNowPlayingScreen(viewModel: MainViewModel, serverName: String) = TvDark {
    val track by viewModel.trackInfo.collectAsState()
    val cover = track.coverArt?.asImageBitmap()
    val player = viewModel.dacpPlayer
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(player) { runCatching { playFocus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(SBg)) {
        AsciiOcean(level = { (NativeBridge.nativeAudioLevel() * 4f).coerceAtMost(1f) })
        // мягкое затемнение под текстом справа
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color.Transparent, 0.5f to Color.Transparent, 0.72f to SBg.copy(alpha = 0.82f), 1f to SBg.copy(alpha = 0.9f))
            )
        )
        Column(
            Modifier.fillMaxHeight().width(300.dp).align(Alignment.CenterEnd).padding(end = 44.dp, top = 28.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(176.dp)
                    .shadow(24.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF7A4FD0), spotColor = Color(0xFF000000))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF241638)),
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) Image(cover, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.MusicNote, null, tint = SDim, modifier = Modifier.size(56.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Сейчас играет · $serverName", color = SDim, fontSize = 10.sp, letterSpacing = 0.4.sp)
            Spacer(Modifier.height(4.dp))
            Text(track.title.ifEmpty { "Без названия" }, color = SText, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 24.sp)
            if (track.artist.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(track.artist, color = SText.copy(alpha = 0.72f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (track.album.isNotEmpty()) {
                Text(track.album, color = SDim.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (player != null) {
                Spacer(Modifier.height(14.dp))
                SoftProgress(player)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PreviousButton(player, modifier = Modifier.size(36.dp).softFocus())
                    PlayPauseButton(
                        player,
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color.White.copy(alpha = 0.10f), CircleShape)
                            .focusRequester(playFocus)
                            .softFocus(),
                        iconSize = 26.dp
                    )
                    NextButton(player, modifier = Modifier.size(36.dp).softFocus())
                }
            }
        }
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Тонкая мягкая полоска прогресса с временем по бокам — без ползунка. */
@Composable
private fun SoftProgress(player: Player) {
    var pos by remember { mutableStateOf(0L) }
    var dur by remember { mutableStateOf(0L) }
    LaunchedEffect(player) {
        while (true) {
            pos = player.currentPosition.coerceAtLeast(0L)
            dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
            delay(500)
        }
    }
    val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Color(0xFF9B7BE0), SSoft))))
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(fmt(pos), color = SDim, fontSize = 10.sp)
        Text(if (dur > 0) fmt(dur) else "", color = SDim, fontSize = 10.sp)
    }
}

/** Пока iPhone подключился, но картинка или звук ещё не пошли. */
@Composable
fun TvConnectingScreen(serverName: String) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val a by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF13233A), PBg), radius = 1400f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Airplay, null, tint = PAccent, modifier = Modifier.size(56.dp).alpha(a))
            Spacer(Modifier.height(16.dp))
            Text("Подключение…", color = PText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Устройство подключилось к «$serverName». Запустите трансляцию на iPhone.",
                color = PDim, fontSize = 12.sp)
        }
    }
}
