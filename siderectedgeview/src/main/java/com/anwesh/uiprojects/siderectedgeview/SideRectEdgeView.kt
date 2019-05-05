package com.anwesh.uiprojects.siderectedgeview

/**
 * Created by anweshmishra on 05/05/19.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color

val nodes : Int = 5
val lines : Int = 4
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val scGap : Float = 0.05f
val scDiv : Double = 0.51
val foreColor : Int = Color.parseColor("#673AB7")
val backColor : Int = Color.parseColor("#BDBDBD")

fun Int.inverse() : Float = 1f / this
fun Float.scaleFactor() : Float = Math.floor(this / scDiv).toFloat()
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.mirrorValue(a : Int, b : Int) : Float {
    val k : Float = scaleFactor()
    return (1 - k) * a.inverse() + k * b.inverse()
}
fun Float.updateValue(dir : Float,  a : Int, b : Int) : Float = mirrorValue(a, b) * dir * scGap

fun Canvas.drawSideRectEdge(size : Float, sc : Float, paint : Paint) {
    save()
    translate(-size, size)
    drawLine(0f, 0f, 0f, -size / 2, paint)
    rotate(180f * sc)
    drawLine(0f, 0f, size, 0f, paint)
    restore()
}

fun Canvas.drawSRENode(i : Int, scale : Float, paint : Paint) {
    val sc1 : Float = scale.divideScale(0, 2)
    val sc2 : Float = scale.divideScale(1, 2)
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    paint.strokeCap = Paint.Cap.ROUND
    paint.color = foreColor
    val parts : Int = lines / 2
    save()
    translate(w / 2, gap * (i + 1))
    for (j in 0..(parts - 1)) {
        val sc2j : Float = sc2.divideScale(j, parts)
        save()
        scale(1f - 2 * (j %2), 1f)
        translate(-(w / 2 - 2 * size) * sc2j, 0f)
        val scj : Float = sc1.divideScale(j, parts)
        for (k in 0..(parts - 1)) {
            val sck : Float = scj.divideScale(k, parts)
            drawSideRectEdge(size, sck, paint)
        }
        restore()
    }
    restore()
}

class SideRectEdgeView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scale.updateValue(dir, lines, lines / 2)
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop(){
            if (animated) {
                animated = false
            }
        }
    }

    class SRENode(var i : Int, val state : State = State()) {

        private var next : SRENode? = null
        private var prev : SRENode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = SRENode(i + 1)
                next?.prev = this
            }
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSRENode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun getNext(dir : Int, cb : () -> Unit) : SRENode {
            var curr : SRENode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class SideRectEdge(var i : Int) {

        private val root : SRENode = SRENode(0)
        private var curr : SRENode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : SideRectEdgeView) {

        private val sre : SideRectEdge = SideRectEdge(0)
        private val animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(backColor)
            sre.draw(canvas, paint)
            animator.animate {
                sre.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            sre.startUpdating {
                animator.start()
            }
        }
    }
}