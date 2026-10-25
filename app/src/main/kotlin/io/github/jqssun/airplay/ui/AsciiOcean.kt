package io.github.jqssun.airplay.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/*
 * ASCII-океан для экрана музыки: процедурные акулы из символов (идея — stackedlol/ascii-sharks, MIT)
 * и стайки рыбок. Акула плывёт к случайной цели, поворачивает с ограниченной скоростью, у края
 * заворачивает к центру; тело — «позвоночник» по пройденному пути, поэтому на поворотах изгибается.
 * ~24 кадра в секунду, одна drawText на строку. level() — громкость музыки 0..1.
 */

private const val RAMP = " .:-=+*#%"
private const val NS = 28                  // сегментов позвоночника
private const val ROWS_ON_SCREEN = 46f

private fun bodyHalf(u: Float): Float {
    if (u < 0f || u > 1f) return 0f
    val nose = min(1f, u / 0.18f).pow(0.6f)
    val taper = min(1f, (1f - u).pow(1.4f) * 1.15f + 0.04f)
    return 0.11f * nose * taper
}

private fun angDiff(a: Float, b: Float): Float {
    var d = b - a
    while (d > PI) d -= (2 * PI).toFloat()
    while (d < -PI) d += (2 * PI).toFloat()
    return d
}

private const val MAX_TILT = 0.6f       // не круче ~35° к горизонту, чтобы акула оставалась в профиль

private fun clampTilt(a: Float): Float {
    val base = if (cos(a) >= 0f) 0f else PI.toFloat()
    return base + angDiff(base, a).coerceIn(-MAX_TILT, MAX_TILT)
}

private class Shark(var x: Float, var y: Float, var th: Float, val len: Float, val sp: Float, val ph: Float) {
    var tx = x; var ty = y; var retarget = 0f
    var dorsal = if (cos(th) >= 0f) -1f else 1f   // какая сторона тела — спина (всегда вверх по экрану)
    val trailX = ArrayList<Float>(); val trailY = ArrayList<Float>()
    val px = FloatArray(NS + 1); val py = FloatArray(NS + 1)
    init {
        for (i in 0..80) { trailX += x - cos(th) * i * len / 60; trailY += y - sin(th) * i * len / 60 }
    }
}

private class Fish(var x: Float, val y: Float, val vx: Float, val p: Float)

@Composable
fun AsciiOcean(level: () -> Float, modifier: Modifier = Modifier.fillMaxSize()) {
    var tick by remember { mutableLongStateOf(0L) }
    val ocean = remember { Ocean() }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                if (now - last >= 40_000_000L) {
                    ocean.step(min(0.1f, (now - last) / 1e9f), level())
                    last = now
                    tick = now
                }
            }
        }
    }
    Canvas(modifier) {
        tick
        ocean.draw(drawContext.canvas.nativeCanvas, size.width, size.height)
    }
}

private class Ocean {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }
    private val rnd = Random(System.nanoTime())
    private var w = 0f; private var h = 0f
    private var cols = 0; private var rows = 0
    private var cellW = 0f; private var cellH = 0f
    private var t = 0f; private var energy = 0f
    private val sharks = ArrayList<Shark>()
    private val fish = ArrayList<Fish>()
    private var grid = Array(0) { CharArray(0) }
    private var shade = Array(0) { FloatArray(0) }

    private fun layout(nw: Float, nh: Float) {
        w = nw; h = nh
        cellH = h / ROWS_ON_SCREEN
        paint.textSize = cellH * 1.05f
        cellW = paint.measureText("M")
        cols = (w / cellW).toInt() + 1
        rows = (h / cellH).toInt() + 1
        grid = Array(rows) { CharArray(cols) }
        shade = Array(rows) { FloatArray(cols) }
        sharks.clear()
        sharks += Shark(w * 0.25f, h * 0.35f, 0f, w * 0.30f, w * 0.05f, 0f)
        sharks += Shark(w * 0.55f, h * 0.72f, PI.toFloat(), w * 0.20f, w * 0.042f, 2f)
        fish.clear()
        repeat(24) { i ->
            val school = i / 8
            fish += Fish(rnd.nextFloat() * w, h * (0.2f + school * 0.28f) + rnd.nextFloat() * h * 0.06f,
                (if (school % 2 == 1) -1f else 1f) * w * 0.02f, rnd.nextFloat() * 6f)
        }
    }

    fun step(dt: Float, lvl: Float) {
        energy += (lvl.coerceIn(0f, 1f) - energy) * 0.03f
        val boost = 0.7f + energy * 0.8f
        t += dt * boost
        if (w == 0f) return
        val m = w * 0.14f; val mY = h * 0.2f
        for (s in sharks) {
            s.retarget -= dt
            if (s.retarget <= 0f || hypot(s.tx - s.x, s.ty - s.y) < w * 0.1f) {
                // цели только в левых 3/4 экрана — справа обложка
                s.tx = m + rnd.nextFloat() * (w * 0.75f - 2 * m)
                s.ty = mY + rnd.nextFloat() * (h - 2 * mY)
                s.retarget = 6f + rnd.nextFloat() * 6f
            }
            var want = atan2(s.ty - s.y, s.tx - s.x)
            val look = s.len * 0.6f
            val lx = s.x + cos(s.th) * look; val ly = s.y + sin(s.th) * look
            val right = cos(s.th) >= 0f
            if (lx < m * 0.5f) want = 0f else if (lx > w - m * 0.5f) want = PI.toFloat()
            if (ly < mY * 0.6f) want = if (right) 0.4f else PI.toFloat() - 0.4f
            else if (ly > h - mY * 0.6f) want = if (right) -0.4f else PI.toFloat() + 0.4f
            want = clampTilt(want)
            val maxTurn = s.sp / (s.len * 0.7f) * dt * boost
            s.th += angDiff(s.th, want).coerceIn(-maxTurn, maxTurn)
            s.x += cos(s.th) * s.sp * dt * boost
            s.y += sin(s.th) * s.sp * dt * boost
            val cx = cos(s.th)
            if (cx > 0.35f) s.dorsal = -1f else if (cx < -0.35f) s.dorsal = 1f
            s.trailX.add(0, s.x); s.trailY.add(0, s.y)
            var acc = 0f
            var i = 1
            while (i < s.trailX.size) {
                acc += hypot(s.trailX[i] - s.trailX[i - 1], s.trailY[i] - s.trailY[i - 1])
                if (acc > s.len * 1.2f) {
                    while (s.trailX.size > i + 1) { s.trailX.removeAt(s.trailX.size - 1); s.trailY.removeAt(s.trailY.size - 1) }
                    break
                }
                i++
            }
        }
        for (f in fish) {
            f.x += f.vx * dt * boost
            if (f.x > w + 20) f.x = -20f
            if (f.x < -20) f.x = w + 20
        }
    }

    /** Точки позвоночника вдоль пройденного пути + волна тела. */
    private fun spine(s: Shark) {
        val seg = s.len / NS
        s.px[0] = s.trailX[0]; s.py[0] = s.trailY[0]
        var idx = 0; var acc = 0f
        for (k in 1..NS) {
            val need = k * seg
            var placed = false
            while (idx < s.trailX.size - 1) {
                val ax = s.trailX[idx]; val ay = s.trailY[idx]; val bx = s.trailX[idx + 1]; val by = s.trailY[idx + 1]
                val l = hypot(bx - ax, by - ay)
                if (acc + l >= need) {
                    val f = if (l > 0f) (need - acc) / l else 0f
                    s.px[k] = ax + (bx - ax) * f; s.py[k] = ay + (by - ay) * f
                    placed = true; break
                }
                acc += l; idx++
            }
            if (!placed) { s.px[k] = s.trailX.last(); s.py[k] = s.trailY.last() }
        }
        val ox = s.px.copyOf(); val oy = s.py.copyOf()
        for (k in 0..NS) {
            val u = k.toFloat() / NS
            val a = max(0, k - 1); val b = min(NS, k + 1)
            var tx = ox[a] - ox[b]; var ty = oy[a] - oy[b]
            val l = hypot(tx, ty).takeIf { it > 0f } ?: 1f
            tx /= l; ty /= l
            val wv = sin(u * 2f * PI.toFloat() * 0.9f - t * 3.2f + s.ph) * u.pow(1.6f) * s.len * 0.045f
            s.px[k] = ox[k] - ty * wv; s.py[k] = oy[k] + tx * wv
        }
    }

    private fun rasterize(s: Shark) {
        spine(s)
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        for (k in 0..NS) { minX = min(minX, s.px[k]); maxX = max(maxX, s.px[k]); minY = min(minY, s.py[k]); maxY = max(maxY, s.py[k]) }
        val pad = s.len * 0.2f
        val c0 = max(0, ((minX - pad) / cellW).toInt()); val c1 = min(cols - 1, ((maxX + pad) / cellW).toInt())
        val r0 = max(0, ((minY - pad) / cellH).toInt()); val r1 = min(rows - 1, ((maxY + pad) / cellH).toInt())
        // хвост: свой базис по усреднённому направлению последней четверти тела, лопасти фиксированного размера
        val ex = s.px[NS]; val ey = s.py[NS]
        var ax0 = ex - s.px[NS - 7]; var ay0 = ey - s.py[NS - 7]
        val al = hypot(ax0, ay0).takeIf { it > 0f } ?: 1f
        ax0 /= al; ay0 /= al
        val lobe = s.len * 0.1f
        for (r in r0..r1) for (c in c0..c1) {
            val px = (c + 0.5f) * cellW; val py = (r + 0.5f) * cellH
            var best = Float.MAX_VALUE; var bu = 0f; var lat = 0f
            for (k in 0 until NS) {
                val ax = s.px[k]; val ay = s.py[k]
                val vx = s.px[k + 1] - ax; val vy = s.py[k + 1] - ay
                val l2 = (vx * vx + vy * vy).takeIf { it > 0f } ?: 1f
                val f = (((px - ax) * vx + (py - ay) * vy) / l2).coerceIn(0f, 1f)
                val qx = ax + vx * f; val qy = ay + vy * f
                val d = hypot(px - qx, py - qy)
                if (d < best) {
                    best = d
                    bu = (k + f) / NS
                    lat = if (vx * (py - ay) - vy * (px - ax) >= 0f) d else -d
                }
            }
            val up = -sign(lat) * s.dorsal > 0f
            val half = bodyHalf(bu) * s.len
            var d = if (half > 0f) best / half else 9f
            if (bu >= 0.28f && bu <= 0.45f && up) {
                val fu = (bu - 0.28f) / 0.17f
                if (best < half + (1f - fu) * fu.pow(0.3f) * s.len * 0.08f) d = min(d, 0.85f)
            }
            if (bu >= 0.22f && bu <= 0.36f && !up) {
                val fu = (bu - 0.22f) / 0.14f
                if (best < half + fu * s.len * 0.05f) d = min(d, 0.9f)
            }
            if (d < 1f) put(r, c, 1f - d)
            val rx = px - ex; val ry = py - ey
            val along = rx * ax0 + ry * ay0
            val perp = abs(rx * -ay0 + ry * ax0)
            if (along > -cellW * 0.5f && along < lobe) {
                val a01 = max(0f, along) / lobe
                if (abs(perp - a01 * lobe * 0.85f) < cellH * (0.75f - 0.25f * a01)) put(r, c, 0.25f)
            }
        }
        // глаз — на спинной стороне у головы
        var vx = s.px[3] - s.px[1]; var vy = s.py[3] - s.py[1]
        val l = hypot(vx, vy).takeIf { it > 0f } ?: 1f
        vx /= l; vy /= l
        val sg = -s.dorsal
        val off = bodyHalf(0.07f) * s.len * 0.35f
        val ec = ((s.px[2] - vy * off * sg) / cellW).toInt(); val er = ((s.py[2] + vx * off * sg) / cellH).toInt()
        if (ec in 0 until cols && er in 0 until rows) { grid[er][ec] = 'o'; shade[er][ec] = 1f }
    }

    private fun put(r: Int, c: Int, inner: Float) {
        val idx = (1 + inner * (RAMP.length - 2)).toInt().coerceIn(1, RAMP.length - 1)
        if (RAMP.indexOf(grid[r][c]) < idx) grid[r][c] = RAMP[idx]
        shade[r][c] = max(shade[r][c], 0.4f + inner * 0.6f)
    }

    private val hsv = FloatArray(3)

    fun draw(canvas: android.graphics.Canvas, nw: Float, nh: Float) {
        if (nw <= 0f || nh <= 0f) return
        if (abs(nw - w) > 1f || abs(nh - h) > 1f) layout(nw, nh)
        for (r in 0 until rows) { grid[r].fill(' '); shade[r].fill(0f) }
        for (f in fish) {
            val cx = (f.x / cellW).toInt(); val cy = ((f.y + sin(t * 1.3f + f.p) * cellH * 0.6f) / cellH).toInt()
            if (cx in 0 until cols && cy in 0 until rows) { grid[cy][cx] = if (f.vx > 0) '>' else '<'; shade[cy][cx] = 0.25f }
        }
        for (s in sharks) rasterize(s)
        for (r in 0 until rows) {
            val line = grid[r]
            var mx = 0f
            var any = false
            for (c in 0 until cols) if (line[c] != ' ') { any = true; if (shade[r][c] > mx) mx = shade[r][c] }
            if (!any) continue
            hsv[0] = 268f + 26f * sin(t * 0.35f + r * 0.12f); hsv[1] = 0.18f; hsv[2] = 0.9f
            val alpha = min(0.66f, 0.24f + 0.4f * mx)
            paint.color = android.graphics.Color.HSVToColor((alpha * 255).toInt(), hsv)
            canvas.drawText(line, 0, cols, 0f, (r + 1) * cellH, paint)
        }
    }
}
