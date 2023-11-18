package com.example.drumcustomview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.properties.Delegates


class DrumCustomView(
    context: Context,
    attributeSet: AttributeSet?
): View(context, attributeSet) {

    private val paint = Paint()
    private val rotationResult = RotationResult()
    
    private var drumRadius by Delegates.notNull<Float>()
    private var indicatorCircleRadius by Delegates.notNull<Float>()
    private var startAngle by Delegates.notNull<Float>()
    private var textSize by Delegates.notNull<Float>()

    private var centerDrumX by Delegates.notNull<Float>()
    private var centerDrumY by Delegates.notNull<Float>()
    private var centerIndicatorCircleX by Delegates.notNull<Float>()
    private var centerIndicatorCircleY by Delegates.notNull<Float>()
    private var startTextX by Delegates.notNull<Float>()
    private var startTextY by Delegates.notNull<Float>()
    private var leftSideRectX by Delegates.notNull<Float>()
    private var topRectY by Delegates.notNull<Float>()
    private var rightSideRectX by Delegates.notNull<Float>()
    private var bottomRectY by Delegates.notNull<Float>()
    private var imageWidth by Delegates.notNull<Int>()
    private var imageHeight by Delegates.notNull<Int>()
    private var startImageX by Delegates.notNull<Float>()
    private var startImageY by Delegates.notNull<Float>()

    private lateinit var timer: Timer
    private lateinit var bitmap: Bitmap
    private lateinit var resultColor: Colors

    init {
        drumRadius = DEFAULT_DRUM_RADIUS
        indicatorCircleRadius = DEFAULT_INDICATOR_CIRCLE_RADIUS
        startAngle = DEFAULT_START_ANGLE
        textSize = DEFAULT_TEXT_SIZE
        imageWidth = DEFAULT_IMAGE_WIDTH
        imageHeight = DEFAULT_IMAGE_HEIGHT
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val resolvedWidth = (drumRadius * 2).toInt()
        val resolvedHeight = (drumRadius * 4).toInt()
        setMeasuredDimension(resolvedWidth, resolvedHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        centerDrumX = drumRadius
        centerDrumY = drumRadius

        centerIndicatorCircleX = (width / 2).toFloat()
        centerIndicatorCircleY = COORDINATE_START

        startTextX = (width / 2).toFloat()
        startTextY = (height / 4 * 3).toFloat()

        startImageX = ((width - imageWidth) / 2).toFloat()
        startImageY = ((height / 2 - imageHeight) / 2 + height / 2).toFloat()

        paint.textSize = textSize
        paint.textAlign = Paint.Align.CENTER

        leftSideRectX = centerDrumX - drumRadius
        topRectY = centerDrumY - drumRadius
        rightSideRectX = centerDrumX + drumRadius
        bottomRectY = centerDrumY + drumRadius
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawDrum(canvas)
        drawIndicatorCircle(canvas)

        if (rotationResult.text != null)
            drawText(rotationResult.text ?: "", canvas)

        if (rotationResult.image != null)
            drawImage(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        performClick()
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        startRotation()
        return super.performClick()
    }

    fun startRotation() {
        val time = (MIN_DIGIT_RANDOM..MAX_DIGIT_RANDOM).random() * ONE_SECOND_LONG
        timer = Timer(time)
        timer.start()
    }

    fun resetDrum() {
        timer.cancel()
        startAngle = DEFAULT_START_ANGLE
        rotationResult.clearFields()
        invalidate()
    }

    fun changeSize(seekBarSize: Int) {
        drumRadius = seekBarSize * DRUM_RADIUS_COEFFICIENT
        indicatorCircleRadius = seekBarSize * INDICATOR_CIRCLE_RADIUS_COEFFICIENT
        textSize = seekBarSize * TEXT_SIZE_COEFFICIENT

        imageWidth = (seekBarSize * IMAGE_WIDTH_COEFFICIENT).toInt()
        imageHeight = (seekBarSize * IMAGE_HEIGHT_COEFFICIENT).toInt()

        centerDrumX = drumRadius
        centerDrumY = drumRadius

        leftSideRectX = centerDrumX - drumRadius
        topRectY = centerDrumY - drumRadius
        rightSideRectX = centerDrumX + drumRadius
        bottomRectY = centerDrumY + drumRadius

        requestLayout()
    }

    private suspend fun getResult() {
        when (rotationResult.color) {
            Colors.RED -> rotationResult.text = Colors.RED.text
            Colors.ORANGE -> rotationResult.image = getImage()
            Colors.YELLOW -> rotationResult.text = Colors.YELLOW.text
            Colors.GREEN -> rotationResult.image = getImage()
            Colors.LIGHT_BLUE -> rotationResult.text = Colors.LIGHT_BLUE.text
            Colors.DARK_BLUE -> rotationResult.image = getImage()
            Colors.VIOLET -> rotationResult.text = Colors.VIOLET.text
            else -> {}
        }
        invalidate()
    }

    private fun drawDrum(canvas: Canvas) {
        if (startAngle > FULL_ROTATION)
            startAngle -= FULL_ROTATION

        var currentAngle = startAngle

        for (color in DRUM_COLORS) {
            paint.color = context.getColor(color.colorId)
            canvas.drawArc(
                leftSideRectX, 
                topRectY, 
                rightSideRectX, 
                bottomRectY, 
                currentAngle, 
                SECTOR_SIZE, 
                true, 
                paint
            )

            if (currentAngle in MIN_ANGLE_VALUE..MAX_ANGLE_VALUE)
                resultColor = color

            currentAngle += SECTOR_SIZE

            if (currentAngle > FULL_ROTATION)
                currentAngle -= FULL_ROTATION
        }
    }

    private fun drawIndicatorCircle(canvas: Canvas) {
        paint.color = Color.BLACK
        canvas.drawCircle(centerIndicatorCircleX, centerIndicatorCircleY, indicatorCircleRadius, paint)
    }

    private fun drawText(text: String, canvas: Canvas) {
        paint.color = context.getColor(rotationResult.color?.colorId ?: 0)
        canvas.drawText(text, startTextX, startTextY, paint)
    }

    private suspend fun getImage(): Bitmap {
        val url = URL(IMAGES_URL)
        bitmap = BitmapFactory.decodeStream(withContext(Dispatchers.IO) {
            withContext(Dispatchers.IO) {
                url.openConnection()
            }.getInputStream()
        })
        return bitmap
    }

    private fun drawImage(canvas: Canvas) {
        canvas.drawBitmap(
            Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, false),
            startImageX,
            startImageY,
            null
        )
    }

    inner class Timer(duration: Long) : CountDownTimer(duration, TIMER_TICK_INTERVAL) {

        override fun onTick(millisUntilFinished: Long) {
            startAngle += ANGLE_ROTATION
            invalidate()
        }

        override fun onFinish() {
            timer.cancel()
            rotationResult.clearFields()
            rotationResult.color = resultColor
            CoroutineScope(Dispatchers.IO).launch {
                getResult()
            }
        }
    }

    inner class RotationResult {

        var color: Colors? = null
        var text: String? = null
        var image: Bitmap? = null

        fun clearFields() {
            color = null
            text = null
            image = null
        }
    }

    enum class Colors(val text: String, val colorId: Int) {
        RED ("RED", R.color.red),
        ORANGE ("ORANGE", R.color.orange),
        YELLOW ("YELLOW", R.color.yellow),
        GREEN ("GREEN", R.color.green),
        LIGHT_BLUE ("LIGHT BLUE", R.color.light_blue),
        DARK_BLUE ("DARK BLUE", R.color.dark_blue),
        VIOLET ("VIOLET", R.color.violet)
    }

    companion object {
        private val DRUM_COLORS = listOf(
            Colors.RED, Colors.ORANGE, Colors.YELLOW, Colors.GREEN,
            Colors.LIGHT_BLUE, Colors.DARK_BLUE, Colors.VIOLET
        )
        private const val IMAGES_URL = "https://loremflickr.com/320/240"
        private const val FULL_ROTATION = 360F
        private const val ANGLE_ROTATION = 10F
        private const val COORDINATE_START = 0F
        private const val ONE_SECOND_LONG = 1000L
        private const val MIN_DIGIT_RANDOM = 1
        private const val MAX_DIGIT_RANDOM = 5
        private const val TIMER_TICK_INTERVAL = 50L
        private const val DEFAULT_START_ANGLE = 270F
        private const val DEFAULT_DRUM_RADIUS = 250F
        private const val DEFAULT_INDICATOR_CIRCLE_RADIUS = 20F
        private const val DEFAULT_TEXT_SIZE = 90F
        private const val DEFAULT_IMAGE_WIDTH = 320
        private const val DEFAULT_IMAGE_HEIGHT = 240
        private const val DRUM_RADIUS_COEFFICIENT = 5F
        private const val INDICATOR_CIRCLE_RADIUS_COEFFICIENT = 0.4F
        private const val TEXT_SIZE_COEFFICIENT = 1.8F
        private const val IMAGE_WIDTH_COEFFICIENT = 6.4
        private const val IMAGE_HEIGHT_COEFFICIENT = 4.8
        private const val MAX_ANGLE_VALUE = 270F
        private val SECTOR_SIZE = FULL_ROTATION / DRUM_COLORS.size
        private val MIN_ANGLE_VALUE = MAX_ANGLE_VALUE - SECTOR_SIZE
    }
}