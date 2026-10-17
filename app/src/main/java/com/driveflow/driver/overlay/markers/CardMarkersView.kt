package com.driveflow.driver.overlay.markers

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.data.prefs.MarkerSize
import com.driveflow.driver.overlay.model.MarkerItem
import com.driveflow.driver.overlay.placement.CardMarkerPlacement

/**
 * Custom full-screen overlay [View] that efficiently draws rounded color verdict marks
 * beside detected ride request cards within a single hardware-accelerated layer.
 *
 * Rules:
 * - Single window / view, never one window per mark.
 * - Zero allocations in [onDraw].
 * - 150ms color change animation.
 * - Excluded from accessibility tree ([View.IMPORTANT_FOR_ACCESSIBILITY_NO]).
 */
@SuppressLint("ViewConstructor")
class CardMarkersView(
    context: Context,
    private val isRtl: Boolean = true
) : View(context) {

    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2f
    }

    private val routeIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    // Pre-allocated drawing cache
    private val drawRectF = RectF()
    private val routePath = Path()

    // Active markers list
    private val activeMarkers = mutableListOf<MarkerItem>()
    private var markerSize: MarkerSize = MarkerSize.NORMAL
    private var currentOpacityPercent: Int = 100

    // Animators cache
    private val animators = mutableMapOf<String, ValueAnimator>()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    /**
     * Updates displayed markers. Called from overlay coordinator on UI thread.
     */
    fun updateMarkers(
        requests: List<ParsedRequest>,
        verdicts: Map<String, Verdict>,
        size: MarkerSize = markerSize,
        opacityPercent: Int = 100
    ) {
        this.markerSize = size
        this.currentOpacityPercent = opacityPercent

        val screenW = width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val screenH = height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        val density = resources.displayMetrics.density
        val sizePx = CardMarkerPlacement.getMarkerSizePx(size, density)

        val newMarkers = ArrayList<MarkerItem>(requests.size)
        val existingKeys = activeMarkers.associateBy { it.requestKey }

        for (req in requests) {
            val verdict = verdicts[req.key]
            val tone = verdict?.tone ?: Tone.GRAY
            val isOpenForRoute = verdict?.flags?.contains("OPEN_FOR_ROUTE") == true

            val targetColor = CardMarkerPlacement.getColorForTone(tone, isOpenForRoute)
            val markerRect = CardMarkerPlacement.calculateMarkerRect(
                cardBounds = req.bounds,
                markerSizePx = sizePx,
                isRtl = isRtl,
                screenWidth = screenW,
                screenHeight = screenH
            )

            val existing = existingKeys[req.key]
            if (existing != null) {
                if (existing.targetColorHex != targetColor) {
                    existing.targetColorHex = targetColor
                    startColorAnimation(existing)
                }
                newMarkers.add(
                    existing.copy(
                        bounds = markerRect,
                        tone = tone,
                        isOpenForRoute = isOpenForRoute
                    )
                )
            } else {
                newMarkers.add(
                    MarkerItem(
                        requestKey = req.key,
                        bounds = markerRect,
                        tone = tone,
                        isOpenForRoute = isOpenForRoute,
                        colorHex = targetColor,
                        targetColorHex = targetColor,
                        animProgress = 1.0f
                    )
                )
            }
        }

        activeMarkers.clear()
        activeMarkers.addAll(newMarkers)
        invalidate()
    }

    private fun startColorAnimation(item: MarkerItem) {
        animators[item.requestKey]?.cancel()
        val fromColor = item.colorHex
        val toColor = item.targetColorHex

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                item.animProgress = progress
                item.colorHex = blendColors(fromColor, toColor, progress)
                invalidate()
            }
        }
        animators[item.requestKey] = animator
        animator.start()
    }

    private fun blendColors(from: Int, to: Int, ratio: Float): Int {
        val inverse = 1f - ratio
        val a = (Color.alpha(from) * inverse + Color.alpha(to) * ratio).toInt()
        val r = (Color.red(from) * inverse + Color.red(to) * ratio).toInt()
        val g = (Color.green(from) * inverse + Color.green(to) * ratio).toInt()
        val b = (Color.blue(from) * inverse + Color.blue(to) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }

    /**
     * Clears all markers immediately (e.g. when app changes or screen turns off).
     */
    fun clearMarkers() {
        for (anim in animators.values) {
            anim.cancel()
        }
        animators.clear()
        activeMarkers.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (activeMarkers.isEmpty()) return

        val alphaMultiplier = CardMarkerPlacement.mapOpacityToAlpha(currentOpacityPercent)

        for (i in 0 until activeMarkers.size) {
            val marker = activeMarkers[i]
            val bounds = marker.bounds

            val baseColor = marker.colorHex
            val alpha = (Color.alpha(baseColor) * alphaMultiplier).toInt().coerceIn(0, 255)
            markerPaint.color = Color.argb(
                alpha,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )

            drawRectF.set(
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                bounds.right.toFloat(),
                bounds.bottom.toFloat()
            )

            val cornerRadius = drawRectF.width() / 2f
            canvas.drawRoundRect(drawRectF, cornerRadius, cornerRadius, markerPaint)

            // Outer subtle stroke
            strokePaint.alpha = (180 * alphaMultiplier).toInt()
            canvas.drawRoundRect(drawRectF, cornerRadius, cornerRadius, strokePaint)

            // Draw route indicator symbol if OPEN_FOR_ROUTE
            if (marker.isOpenForRoute) {
                drawRouteSymbol(canvas, drawRectF, alphaMultiplier)
            }
        }
    }

    private fun drawRouteSymbol(canvas: Canvas, rect: RectF, alphaMultiplier: Float) {
        routeIconPaint.alpha = (220 * alphaMultiplier).toInt()
        routePath.reset()

        val cx = rect.centerX()
        val cy = rect.centerY()
        val radius = rect.width() * 0.22f

        // Small directional arrowhead / pin mark
        routePath.moveTo(cx - radius, cy + radius * 0.5f)
        routePath.lineTo(cx, cy - radius)
        routePath.lineTo(cx + radius, cy + radius * 0.5f)

        canvas.drawPath(routePath, routeIconPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearMarkers()
    }
}
