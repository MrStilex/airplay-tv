package io.github.jqssun.airplay.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Airplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.Inet4Address
import kotlinx.coroutines.delay

// Палитра экрана ожидания (в духе AirPlay TV): тёмно-синий фон, бирюзовый акцент.
private val Bg = Color(0xFF0E131C)
private val BgGlow = Color(0xFF13233A)
private val Card = Color(0xFF151D2B)
private val CardBorder = Color(0xFF263247)
private val Accent = Color(0xFF00E5FF)
private val Ok = Color(0xFF22C55E)
private val TextMain = Color(0xFFF4F7FB)
private val TextDim = Color(0xFF9AA6B8)

/** Первый IPv4-адрес активной сети, или null. */
private fun localIpv4(ctx: Context): String? {
    val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return null
    val lp = cm.getLinkProperties(cm.activeNetwork) ?: return null
    return lp.linkAddresses.map(LinkAddress::getAddress).firstOrNull { it is Inet4Address }?.hostAddress
}

/**
 * Полноэкранный экран ожидания для телевизора: имя приставки, адрес и короткая инструкция по-русски.
 * Показывается, пока сервер запущен и никто не подключён; кнопка «Настройки» открывает обычный интерфейс.
 */
@Composable
fun TvIdleScreen(serverName: String, port: Int, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    var ip by remember { mutableStateOf(localIpv4(ctx)) }
    LaunchedEffect(Unit) {
        while (true) { delay(10_000); ip = localIpv4(ctx) }   // адрес может смениться после переподключения Wi-Fi
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(BgGlow, Bg), radius = 1600f))
            .padding(horizontal = 72.dp, vertical = 56.dp)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            // Левая часть: заголовок, имя, адрес, кнопка
            Column(Modifier.weight(1.1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Airplay, null, tint = Accent, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("AirPlay", color = Accent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(18.dp))
                    Text(
                        "● Готов к трансляции",
                        color = Ok, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .border(1.dp, Ok.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .background(Ok.copy(alpha = 0.10f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(36.dp))
                Text("Готово к подключению", color = TextMain, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Выберите это устройство в «Повторе экрана» или в AirPlay на iPhone, iPad или Mac.",
                    color = TextDim, fontSize = 19.sp, lineHeight = 26.sp
                )
                Spacer(Modifier.height(32.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                        .background(Card, RoundedCornerShape(24.dp))
                        .padding(horizontal = 28.dp, vertical = 8.dp)
                ) {
                    InfoRow("Имя устройства", serverName, Accent, big = true)
                    HorizontalDivider(color = CardBorder)
                    InfoRow("Адрес", if (ip != null) "$ip:$port" else "нет сети", TextMain, big = false)
                }
                Spacer(Modifier.height(32.dp))
                var focused by remember { mutableStateOf(false) }
                Button(
                    onClick = onSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (focused) Accent else Card,
                        contentColor = if (focused) Bg else TextMain
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .focusRequester(focus)
                        .onFocusChanged { focused = it.isFocused }
                        .border(2.dp, if (focused) Color.White else CardBorder, RoundedCornerShape(16.dp))
                ) {
                    Text("НАСТРОЙКИ", fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
                }
            }
            Spacer(Modifier.width(56.dp))
            // Правая часть: как подключиться
            Column(
                Modifier
                    .weight(1f)
                    .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
                    .background(Card.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
                    .padding(36.dp)
            ) {
                Text("Как подключиться", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                Step(1, "Одна сеть Wi-Fi", "iPhone, iPad или Mac должен быть в той же сети, что и приставка")
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 14.dp))
                Step(2, "Пункт управления", "Нажмите «Повтор экрана» или значок AirPlay в приложении")
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 14.dp))
                Step(3, "Выберите «$serverName»", "Трансляция начнётся сразу, на весь экран")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color, big: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", color = TextDim, fontSize = 19.sp)
        Spacer(Modifier.width(16.dp))
        Text(value, color = valueColor, fontSize = if (big) 28.sp else 22.sp,
            fontWeight = if (big) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun Step(n: Int, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).border(1.dp, CardBorder, CircleShape).background(Bg, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text("$n", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = TextDim, fontSize = 16.sp, lineHeight = 22.sp)
        }
    }
}
