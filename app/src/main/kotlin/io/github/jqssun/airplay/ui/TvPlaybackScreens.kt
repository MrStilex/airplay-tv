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
import androidx.compose.ui.draw.clip
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
import androidx.media3.ui.compose.material3.indicator.DurationText
import androidx.media3.ui.compose.material3.indicator.PositionText
import androidx.media3.ui.compose.material3.indicator.ProgressSlider
import io.github.jqssun.airplay.ui.gestures.ServerVolumeState
import io.github.jqssun.airplay.viewmodel.MainViewModel
import kotlin.math.roundToInt

// Та же палитра, что у экрана ожидания
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
        primary = PAccent, onPrimary = PBg, secondary = PAccent,
        surface = PBg, onSurface = PText, onSurfaceVariant = PDim,
        surfaceVariant = PCard, background = PBg, onBackground = PText
    ),
    content = content
)

/** Музыка по AirPlay на весь экран телевизора: обложка, название, прогресс, кнопки и громкость. */
@Composable
@AndroidxOptIn(UnstableApi::class)
fun TvNowPlayingScreen(viewModel: MainViewModel, serverName: String) = TvDark {
    val track by viewModel.trackInfo.collectAsState()
    val cover = track.coverArt?.asImageBitmap()
    val player = viewModel.dacpPlayer
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(player) { runCatching { playFocus.requestFocus() } }

    Box(Modifier.fillMaxSize().background(PBg)) {
        // Фон: та же обложка, растянутая и приглушённая (размытие на Android 9 недоступно)
        if (cover != null) {
            Image(cover, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.35f))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(PBg.copy(alpha = 0.55f), PBg.copy(alpha = 0.92f)))
            )
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, PBg.copy(alpha = 0.8f)))))

        Row(
            Modifier.fillMaxSize().padding(horizontal = 80.dp, vertical = 64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Обложка
            Box(
                Modifier
                    .fillMaxHeight(0.82f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                    .background(PCard),
                contentAlignment = Alignment.Center
            ) {
                if (cover != null) {
                    Image(cover, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.MusicNote, null, tint = PDim, modifier = Modifier.size(120.dp))
                }
            }
            Spacer(Modifier.width(64.dp))
            // Текст и управление
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Airplay, null, tint = PAccent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Сейчас играет · $serverName", color = PAccent, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(20.dp))
                Text(track.title.ifEmpty { "Без названия" }, color = PText, fontSize = 44.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 50.sp)
                if (track.artist.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(track.artist, color = PText.copy(alpha = 0.85f), fontSize = 26.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (track.album.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(track.album, color = PDim, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(36.dp))
                if (player != null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        PositionText(player, Modifier.padding(end = 12.dp))
                        Box(Modifier.weight(1f)) { ProgressSlider(player) }
                        DurationText(player, Modifier.padding(start = 12.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        PreviousButton(player, modifier = Modifier.size(64.dp).dpadFocus(CircleShape))
                        PlayPauseButton(
                            player,
                            modifier = Modifier
                                .size(84.dp)
                                .background(PAccent.copy(alpha = 0.18f), CircleShape)
                                .focusRequester(playFocus)
                                .dpadFocus(CircleShape),
                            iconSize = 52.dp
                        )
                        NextButton(player, modifier = Modifier.size(64.dp).dpadFocus(CircleShape))
                    }
                    Spacer(Modifier.height(28.dp))
                    TvVolumeRow()
                }
            }
        }
    }
}

@Composable
private fun TvVolumeRow() {
    val context = LocalContext.current
    val volume = remember { ServerVolumeState(context) }
    DisposableEffect(volume) { volume.handleLifecycle(this) }
    Row(
        Modifier
            .fillMaxWidth(0.75f)
            .border(1.dp, PBorder, RoundedCornerShape(18.dp))
            .background(PCard.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = PDim)
        Spacer(Modifier.width(12.dp))
        Slider(
            value = volume.percentage / 100f,
            onValueChange = { volume.update((it * 100).roundToInt()) },
            colors = SliderDefaults.colors(thumbColor = PAccent, activeTrackColor = PAccent, inactiveTrackColor = PBorder),
            modifier = Modifier
                .weight(1f)
                .dpadFocus()
                .dpadAdjust(onLeft = { volume.adjust(false) }, onRight = { volume.adjust(true) })
        )
        Spacer(Modifier.width(12.dp))
        Text("${volume.percentage}%", color = PText, fontSize = 16.sp, modifier = Modifier.width(48.dp))
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
            Icon(Icons.Filled.Airplay, null, tint = PAccent, modifier = Modifier.size(96.dp).alpha(a))
            Spacer(Modifier.height(28.dp))
            Text("Подключение…", color = PText, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Устройство подключилось к «$serverName». Запустите трансляцию на iPhone.",
                color = PDim, fontSize = 19.sp)
        }
    }
}
