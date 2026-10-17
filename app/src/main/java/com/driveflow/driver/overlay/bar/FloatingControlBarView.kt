package com.driveflow.driver.overlay.bar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.driveflow.driver.R
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.overlay.model.BarMode
import com.driveflow.driver.overlay.model.FloatingBarState
import com.driveflow.driver.overlay.placement.CardMarkerPlacement
import java.util.Locale

/**
 * Floating control bar drawn above ride apps over the map area.
 *
 * Provides:
 * - Drag handle to reposition smoothly.
 * - Verdict chip with color and summary.
 * - Key metrics (Fare, Km, Rate, Road State).
 * - Direct action buttons: Report problem, Collapse to dot, Close.
 * - Screen reader content descriptions on interactive controls.
 */
@SuppressLint("ViewConstructor")
class FloatingControlBarView(
    context: Context,
    private val onPositionChanged: (x: Int, y: Int) -> Unit,
    private val onReportClicked: (request: ParsedRequest?, verdict: Verdict?) -> Unit,
    private val onCloseClicked: () -> Unit,
    private val isRtl: Boolean = true
) : FrameLayout(context) {

    private val density = resources.displayMetrics.density
    private fun dp(value: Int): Int = (value * density + 0.5f).toInt()

    // Root containers
    private val expandedBarLayout: LinearLayout
    private val collapsedDotLayout: FrameLayout

    // Interactive & Display Elements
    private val dragHandle: ImageView
    private val logoIcon: ImageView
    private val verdictChip: TextView
    private val numberRowLayout: LinearLayout
    private val fareText: TextView
    private val kmText: TextView
    private val rateText: TextView
    private val roadStateBadge: TextView
    private val statusMessageText: TextView
    private val reportButton: ImageView
    private val collapseButton: ImageView
    private val closeButton: ImageView

    // Dot elements
    private val dotLogo: ImageView
    private val dotStatusIndicator: View

    // Drag tracking
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    var currentX: Int = 0
        private set
    var currentY: Int = 0
        private set

    var state: FloatingBarState = FloatingBarState()
        private set

    init {
        // 1. Collapsed Dot Layout (38dp circular button)
        val dotSize = dp(38)
        collapsedDotLayout = FrameLayout(context).apply {
            layoutParams = LayoutParams(dotSize, dotSize).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0E2230"))
                setStroke(dp(1), Color.parseColor("#F6C945"))
            }
            visibility = View.GONE
            contentDescription = context.getString(R.string.overlay_btn_expand)

            dotLogo = ImageView(context).apply {
                val iconSize = dp(20)
                layoutParams = LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
                setImageResource(R.drawable.ic_driveflow_logo)
            }
            addView(dotLogo)

            dotStatusIndicator = View(context).apply {
                val indicatorSize = dp(8)
                layoutParams = LayoutParams(indicatorSize, indicatorSize).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = dp(2)
                    marginEnd = dp(2)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(CardMarkerPlacement.COLOR_GREEN)
                }
            }
            addView(dotStatusIndicator)

            setOnClickListener {
                expandBar()
            }
        }
        addView(collapsedDotLayout)

        // 2. Expanded Bar Layout (Navy Surface #0E2230 with 12dp rounded corners)
        expandedBarLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#0E2230"))
                setStroke(dp(1), Color.parseColor("#22435A"))
            }
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        addView(expandedBarLayout)

        // Header row: [Drag Handle] [Logo] [Verdict Chip / Status] [Report] [Collapse] [Close]
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Drag Handle
        dragHandle = ImageView(context).apply {
            val hSize = dp(24)
            layoutParams = LinearLayout.LayoutParams(hSize, hSize).apply {
                marginEnd = dp(4)
            }
            setImageResource(android.R.drawable.ic_menu_sort_by_size)
            setColorFilter(Color.parseColor("#9FB3C2"))
            contentDescription = context.getString(R.string.overlay_btn_drag)
        }
        headerRow.addView(dragHandle)

        // Logo
        logoIcon = ImageView(context).apply {
            val lSize = dp(20)
            layoutParams = LinearLayout.LayoutParams(lSize, lSize).apply {
                marginEnd = dp(6)
            }
            setImageResource(R.drawable.ic_driveflow_logo)
        }
        headerRow.addView(logoIcon)

        // Verdict chip
        verdictChip = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
            textSize = 12f
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(6).toFloat()
                setColor(CardMarkerPlacement.COLOR_GRAY)
            }
            text = context.getString(R.string.overlay_state_reading)
        }
        headerRow.addView(verdictChip)

        // Status message (when reading, paused, or license issue)
        statusMessageText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
            textSize = 12f
            setTextColor(Color.parseColor("#9FB3C2"))
            visibility = View.GONE
        }
        headerRow.addView(statusMessageText)

        // Report Button
        reportButton = ImageView(context).apply {
            val bSize = dp(24)
            layoutParams = LinearLayout.LayoutParams(bSize, bSize).apply {
                marginEnd = dp(4)
            }
            setImageResource(android.R.drawable.ic_menu_info_details)
            setColorFilter(Color.parseColor("#F6C945"))
            contentDescription = context.getString(R.string.overlay_btn_report)
            setOnClickListener {
                onReportClicked(state.request, state.verdict)
            }
        }
        headerRow.addView(reportButton)

        // Collapse Button
        collapseButton = ImageView(context).apply {
            val bSize = dp(24)
            layoutParams = LinearLayout.LayoutParams(bSize, bSize).apply {
                marginEnd = dp(4)
            }
            setImageResource(android.R.drawable.arrow_down_float)
            setColorFilter(Color.parseColor("#9FB3C2"))
            contentDescription = context.getString(R.string.overlay_btn_collapse)
            setOnClickListener {
                collapseBar()
            }
        }
        headerRow.addView(collapseButton)

        // Close Button
        closeButton = ImageView(context).apply {
            val bSize = dp(24)
            layoutParams = LinearLayout.LayoutParams(bSize, bSize)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#9FB3C2"))
            contentDescription = context.getString(R.string.overlay_btn_close)
            setOnClickListener {
                onCloseClicked()
            }
        }
        headerRow.addView(closeButton)

        expandedBarLayout.addView(headerRow)

        // Number row: [Fare] | [Total Km] | [Needed vs Gross] | [Road state]
        numberRowLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
        }

        fareText = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(0, 0, dp(8), 0)
        }
        numberRowLayout.addView(fareText)

        kmText = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#9FB3C2"))
            setPadding(0, 0, dp(8), 0)
        }
        numberRowLayout.addView(kmText)

        rateText = TextView(context).apply {
            textSize = 12f
            setTextColor(Color.parseColor("#7CF5C4")) // Mint
            setPadding(0, 0, dp(8), 0)
        }
        numberRowLayout.addView(rateText)

        roadStateBadge = TextView(context).apply {
            textSize = 10f
            setTextColor(Color.parseColor("#F6C945")) // Gold
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(4).toFloat()
                setColor(Color.parseColor("#18384E"))
            }
            setPadding(dp(4), dp(1), dp(4), dp(1))
            visibility = View.GONE
        }
        numberRowLayout.addView(roadStateBadge)

        expandedBarLayout.addView(numberRowLayout)

        setupDragTouch()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragTouch() {
        val touchListener = OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = currentX
                    initialY = currentY
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val newX = initialX + dx
                        val newY = initialY + dy
                        updatePosition(newX, newY, notify = false)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        onPositionChanged(currentX, currentY)
                    }
                    true
                }
                else -> false
            }
        }

        dragHandle.setOnTouchListener(touchListener)
        collapsedDotLayout.setOnTouchListener(touchListener)
    }

    fun updateState(newState: FloatingBarState) {
        this.state = newState
        alpha = newState.alpha

        // Collapsed vs Expanded
        if (newState.isCollapsed) {
            expandedBarLayout.visibility = View.GONE
            collapsedDotLayout.visibility = View.VISIBLE
            val dotColor = newState.verdict?.tone?.let { CardMarkerPlacement.getColorForTone(it) }
                ?: CardMarkerPlacement.COLOR_GRAY
            (dotStatusIndicator.background as? GradientDrawable)?.setColor(dotColor)
            return
        }

        expandedBarLayout.visibility = View.VISIBLE
        collapsedDotLayout.visibility = View.GONE

        when (newState.mode) {
            BarMode.READING -> {
                verdictChip.visibility = View.VISIBLE
                verdictChip.text = context.getString(R.string.overlay_state_reading)
                setChipColor(CardMarkerPlacement.COLOR_GRAY)
                numberRowLayout.visibility = View.GONE
                statusMessageText.visibility = View.GONE
            }
            BarMode.WAITING_DETAILS -> {
                verdictChip.visibility = View.VISIBLE
                verdictChip.text = context.getString(R.string.overlay_state_waiting_details)
                setChipColor(CardMarkerPlacement.COLOR_GRAY)
                numberRowLayout.visibility = View.GONE
                statusMessageText.visibility = View.GONE
            }
            BarMode.LICENSE_PROBLEM -> {
                verdictChip.visibility = View.VISIBLE
                verdictChip.text = context.getString(R.string.overlay_state_license_problem)
                setChipColor(CardMarkerPlacement.COLOR_RED)
                numberRowLayout.visibility = View.GONE
                statusMessageText.visibility = View.VISIBLE
                statusMessageText.text = newState.statusMessage ?: ""
            }
            BarMode.READER_PAUSED -> {
                verdictChip.visibility = View.VISIBLE
                verdictChip.text = context.getString(R.string.overlay_state_reader_paused)
                setChipColor(CardMarkerPlacement.COLOR_ORANGE)
                numberRowLayout.visibility = View.GONE
                statusMessageText.visibility = View.GONE
            }
            BarMode.UPDATE_AVAILABLE -> {
                verdictChip.visibility = View.VISIBLE
                verdictChip.text = context.getString(R.string.overlay_state_update_available)
                setChipColor(CardMarkerPlacement.COLOR_GREEN)
                numberRowLayout.visibility = View.GONE
                statusMessageText.visibility = View.GONE
            }
            BarMode.VERDICT -> {
                val verdict = newState.verdict
                val request = newState.request
                if (verdict != null && request != null) {
                    verdictChip.visibility = View.VISIBLE
                    verdictChip.text = if (isRtl) verdict.titleAr else verdict.titleEn
                    val color = CardMarkerPlacement.getColorForTone(
                        verdict.tone,
                        verdict.flags.contains("OPEN_FOR_ROUTE")
                    )
                    setChipColor(color)

                    numberRowLayout.visibility = View.VISIBLE
                    statusMessageText.visibility = View.GONE

                    // Fare
                    val fareVal = request.fare
                    fareText.text = if (fareVal != null) {
                        String.format(Locale.US, "%.0f %s", fareVal, request.currency ?: "EGP")
                    } else "--"

                    // Total Km
                    val totalKmVal = verdict.totalKm ?: request.totalKm
                    kmText.text = if (totalKmVal != null) {
                        String.format(Locale.US, "%.1f km", totalKmVal)
                    } else "--"

                    // Rate
                    val gross = verdict.grossPerKm
                    val net = verdict.netPerKm
                    rateText.text = when {
                        net != null -> String.format(Locale.US, "%.1f net/km", net)
                        gross != null -> String.format(Locale.US, "%.1f /km", gross)
                        else -> ""
                    }

                    // Road State
                    when (request.roadState) {
                        RoadState.EASY -> {
                            roadStateBadge.visibility = View.VISIBLE
                            roadStateBadge.text = context.getString(R.string.overlay_road_easy)
                        }
                        RoadState.SLOW -> {
                            roadStateBadge.visibility = View.VISIBLE
                            roadStateBadge.text = context.getString(R.string.overlay_road_slow)
                        }
                        RoadState.HARD -> {
                            roadStateBadge.visibility = View.VISIBLE
                            roadStateBadge.text = context.getString(R.string.overlay_road_hard)
                        }
                        RoadState.UNKNOWN -> {
                            roadStateBadge.visibility = View.GONE
                        }
                    }
                } else {
                    verdictChip.text = context.getString(R.string.overlay_state_reading)
                    setChipColor(CardMarkerPlacement.COLOR_GRAY)
                    numberRowLayout.visibility = View.GONE
                }
            }
            BarMode.HIDDEN -> {
                visibility = View.GONE
                return
            }
        }

        visibility = View.VISIBLE
    }

    private fun setChipColor(color: Int) {
        (verdictChip.background as? GradientDrawable)?.setColor(color)
    }

    fun updatePosition(x: Int, y: Int, notify: Boolean = true) {
        this.currentX = x
        this.currentY = y
        val lp = layoutParams as? LayoutParams
        if (lp != null) {
            lp.leftMargin = x
            lp.topMargin = y
            layoutParams = lp
        }
        if (notify) {
            onPositionChanged(x, y)
        }
    }

    fun collapseBar() {
        state = state.copy(isCollapsed = true)
        updateState(state)
    }

    fun expandBar() {
        state = state.copy(isCollapsed = false)
        updateState(state)
    }
}
