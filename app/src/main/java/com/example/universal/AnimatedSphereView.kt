package com.example.universal

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*

class AnimatedSphereView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 100f
    private var currentRadius = 100f

    // Animation properties
    private var colorPhase = 0f
    private var pulsePhase = 0f
    private var morphPhase = 0f
    private var shimmerPhase = 0f
    private var speechIntensity = 0f

    // Speech reaction properties
    private var isReacting = false
    private var reactionIntensity = 0f

    // Animators
    private var colorAnimator: ValueAnimator? = null
    private var pulseAnimator: ValueAnimator? = null
    private var morphAnimator: ValueAnimator? = null
    private var shimmerAnimator: ValueAnimator? = null
    private var speechAnimator: ValueAnimator? = null

    // Colors for the gradient
    private val colors = intArrayOf(
        Color.parseColor("#FF6B35"), // Orange
        Color.parseColor("#F7931E"), // Yellow-Orange
        Color.parseColor("#FFD23F"), // Yellow
        Color.parseColor("#06FFA5"), // Green
        Color.parseColor("#118AB2"), // Blue
        Color.parseColor("#073B4C"), // Dark Blue
        Color.parseColor("#8E44AD"), // Purple
        Color.parseColor("#E74C3C")  // Red
    )

    init {
        setupAnimations()
    }

    private fun setupAnimations() {
        // Color cycling animation
        colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                colorPhase = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Pulse animation
        pulseAnimator = ValueAnimator.ofFloat(0f, 2 * PI.toFloat()).apply {
            duration = 2000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                pulsePhase = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Morph animation (changes shape)
        morphAnimator = ValueAnimator.ofFloat(0f, 2 * PI.toFloat()).apply {
            duration = 6000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                morphPhase = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        // Shimmer effect
        shimmerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                shimmerPhase = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun startSpeechReaction() {
        isReacting = true
        speechAnimator?.cancel()

        speechAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 1000
            addUpdateListener { animation ->
                speechIntensity = animation.animatedValue as Float
                reactionIntensity = speechIntensity * 2f
                invalidate()
            }
            start()
        }
    }

    fun startListeningAnimation() {
        speechAnimator?.cancel()

        speechAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                speechIntensity = animation.animatedValue as Float
                reactionIntensity = speechIntensity * 0.5f
                invalidate()
            }
            start()
        }
    }

    fun stopSpeechReaction() {
        isReacting = false
        speechAnimator?.cancel()

        speechAnimator = ValueAnimator.ofFloat(speechIntensity, 0f).apply {
            duration = 500
            addUpdateListener { animation ->
                speechIntensity = animation.animatedValue as Float
                reactionIntensity = speechIntensity
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = minOf(w, h) / 4f
        currentRadius = baseRadius
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculate dynamic radius with morphing and speech reaction
        val morphOffset = sin(morphPhase) * 20f
        val pulseOffset = sin(pulsePhase) * 15f
        val speechOffset = reactionIntensity * 40f
        currentRadius = baseRadius + morphOffset + pulseOffset + speechOffset

        // Create dynamic color gradient
        val colorIndex = (colorPhase * colors.size).toInt()
        val colorProgress = (colorPhase * colors.size) - colorIndex

        val currentColor = if (colorIndex < colors.size - 1) {
            interpolateColor(colors[colorIndex], colors[colorIndex + 1], colorProgress)
        } else {
            interpolateColor(colors[colorIndex], colors[0], colorProgress)
        }

        val nextColor = if (colorIndex < colors.size - 2) {
            colors[colorIndex + 2]
        } else {
            colors[(colorIndex + 2) % colors.size]
        }

        // Create radial gradient with speech intensity
        val gradientRadius = currentRadius * (1.2f + speechIntensity * 0.5f)
        val radialGradient = RadialGradient(
            centerX, centerY, gradientRadius,
            intArrayOf(
                Color.WHITE,
                currentColor,
                nextColor,
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.4f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )

        // Draw glow effect
        glowPaint.shader = radialGradient
        glowPaint.alpha = (100 + speechIntensity * 155).toInt()
        canvas.drawCircle(centerX, centerY, gradientRadius, glowPaint)

        // Create morphing sphere points
        val numPoints = 8
        val morphedPoints = mutableListOf<PointF>()

        for (i in 0 until numPoints) {
            val angle = (i * 2 * PI / numPoints).toFloat()
            val morphOffset1 = sin(morphPhase + angle * 3) * 15f
            val morphOffset2 = cos(morphPhase * 1.5f + angle * 2) * 10f
            val speechMorph = sin(angle + speechIntensity * PI) * speechIntensity * 25f

            val radius = currentRadius + morphOffset1 + morphOffset2 + speechMorph
            val x = centerX + cos(angle) * radius
            val y = centerY + sin(angle) * radius

            morphedPoints.add(PointF(x.toFloat(), y.toFloat()))
        }

        // Create main sphere gradient
        val mainGradient = RadialGradient(
            centerX - currentRadius * 0.3f,
            centerY - currentRadius * 0.3f,
            currentRadius * 1.5f,
            intArrayOf(
                Color.WHITE,
                currentColor,
                darkenColor(currentColor, 0.7f)
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = mainGradient
        paint.alpha = 255

        // Draw morphed sphere using path
        val path = Path()
        if (morphedPoints.isNotEmpty()) {
            path.moveTo(morphedPoints[0].x, morphedPoints[0].y)

            for (i in 1 until morphedPoints.size) {
                val current = morphedPoints[i]
                val previous = morphedPoints[i - 1]
                val next = morphedPoints[(i + 1) % morphedPoints.size]

                // Create smooth curves between points
                val cp1x = previous.x + (current.x - previous.x) * 0.5f
                val cp1y = previous.y + (current.y - previous.y) * 0.5f
                val cp2x = current.x + (next.x - current.x) * 0.3f
                val cp2y = current.y + (next.y - current.y) * 0.3f

                path.cubicTo(cp1x, cp1y, cp2x, cp2y, current.x, current.y)
            }

            // Close the path
            val first = morphedPoints[0]
            val last = morphedPoints[morphedPoints.size - 1]
            path.cubicTo(
                last.x + (first.x - last.x) * 0.5f,
                last.y + (first.y - last.y) * 0.5f,
                first.x, first.y,
                first.x, first.y
            )
            path.close()
        }

        canvas.drawPath(path, paint)

        // Add shimmer effect
        val shimmerX = centerX + cos(shimmerPhase * 2 * PI) * currentRadius * 0.6f
        val shimmerY = centerY + sin(shimmerPhase * 2 * PI) * currentRadius * 0.6f

        val shimmerGradient = RadialGradient(
            shimmerX.toFloat(), shimmerY.toFloat(), currentRadius * 0.4f,
            intArrayOf(
                Color.argb(150, 255, 255, 255),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        shimmerPaint.shader = shimmerGradient
        canvas.drawCircle(shimmerX.toFloat(), shimmerY.toFloat(), currentRadius * 0.4f, shimmerPaint)

        // Add speech reaction particles
        if (speechIntensity > 0.1f) {
            drawSpeechParticles(canvas)
        }
    }

    private fun drawSpeechParticles(canvas: Canvas) {
        val particleCount = (speechIntensity * 20).toInt()
        val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (i in 0 until particleCount) {
            val angle = (i * 2 * PI / particleCount + speechIntensity * 4).toFloat()
            val distance = currentRadius + speechIntensity * 50f + sin(angle * 3 + pulsePhase) * 20f

            val x = centerX + cos(angle) * distance
            val y = centerY + sin(angle) * distance

            val particleSize = speechIntensity * 8f + sin(angle * 2 + morphPhase) * 3f

            // Particle gradient
            val particleGradient = RadialGradient(
                x, y, particleSize,
                intArrayOf(
                    Color.argb((speechIntensity * 255).toInt(), 255, 255, 255),
                    Color.argb((speechIntensity * 100).toInt(), 255, 200, 100),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )

            particlePaint.shader = particleGradient
            canvas.drawCircle(x, y, particleSize, particlePaint)
        }
    }

    private fun interpolateColor(color1: Int, color2: Int, fraction: Float): Int {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)

        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        val r = (r1 + (r2 - r1) * fraction).toInt()
        val g = (g1 + (g2 - g1) * fraction).toInt()
        val b = (b1 + (b2 - b1) * fraction).toInt()

        return Color.rgb(r, g, b)
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.rgb(r, g, b)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAllAnimations()
    }

    private fun stopAllAnimations() {
        colorAnimator?.cancel()
        pulseAnimator?.cancel()
        morphAnimator?.cancel()
        shimmerAnimator?.cancel()
        speechAnimator?.cancel()
    }
}