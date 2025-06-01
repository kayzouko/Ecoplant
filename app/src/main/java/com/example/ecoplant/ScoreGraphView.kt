package com.example.ecoplant

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import android.view.View
import kotlin.math.min

class ScoreGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var score: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val arcPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 8f
    }

    /** * Constructeur pour initialiser la vue avec des attributs personnalisés.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = size / 2f - 10f

        //cercle de fond
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        //arc de score
        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawArc(rect, -90f, 360 * score, false, arcPaint)
    }

    /** * Mesure la vue pour qu'elle soit carrée.
     * Utilise la taille minimale entre la largeur et la hauteur.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }
}