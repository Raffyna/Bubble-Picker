package com.igalata.bubblepicker.physics

import android.util.Log
import com.igalata.bubblepicker.rendering.MyItem
import com.igalata.bubblepicker.sqr
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
import java.util.*


/**
 * Created by irinagalata on 1/26/17.
 */
object Engine {

    /**
     * List of selected bubble
     */
    val selectedBodies: List<CircleBody>
        get() = bodies.filter { it.increased || it.toBeIncreased || it.isIncreasing }


    var oldTime = 0L
    var maxSelectedCount: Int? = null
    var radius = 50
        set(value) {
            field = value
            bubbleRadius = interpolate(0.1f, 0.25f, value / 100f)
            gravity = interpolate(20f, 80f, value / 100f)
            standardIncreasedGravity = interpolate(500f, 800f, value / 100f)
        }
    var centerImmediately = false
    private var standardIncreasedGravity = interpolate(500f, 800f, 0.5f)
    private var bubbleRadius = 0.17f

    private val world = World(Vec2(0f, 0f), false)
    private val step = 0.002f

    /**
     * All bubbles
     */
    private val bodies: ArrayList<CircleBody> = ArrayList()

    private var borders: ArrayList<Border> = ArrayList()
    private val resizeDuration = 0.25f //fabricio seconds
    private var scaleX = 0f
    private var scaleY = 0f
    private var touch = false
    private var gravity = 8f
    private var increasedGravity = 55f
    private var gravityCenter = Vec2(0f, 0f)
    private val currentGravity: Float
        get() = if (touch) increasedGravity else gravity
    private val toBeIncrease = ArrayList<MyItem>()

    private var toBeDescrease = ArrayList<MyItem>()

    private val startX
        get() = if (centerImmediately) 0.5f else 1.2f
    private var stepsCount = 0

    private var canZoom = true
    private var numberBubbleActive = 0

    fun build(bodiesCount: Int, scaleX: Float, scaleY: Float): List<CircleBody> {
        val density = interpolate(0.8f, 0.2f, radius / 100f)
        for (i in 0 until bodiesCount) {
            //The bubbles will show in the center of view
            val x = if (Random().nextBoolean()) -startX else startX
            val y = if (Random().nextBoolean()) -0.5f / scaleY else 0.5f / scaleY
//            bodies.add(CircleBody(world, Vec2(x, y), bubbleRadius * scaleX, (bubbleRadius * scaleX) * 1.3f, density))
//            bodies.add(CircleBody(world, Vec2(x, y), bubbleRadius * scaleX, 0.05f, density)) //TODO set increasedRadius formula

            bubbleRadius = (1f / bodiesCount) //Set the bubbleRadius on the base of the number of bubbles
            Log.d("Raffy", " ===================== ")
            Log.d("Raffy", "BubbleRadius  => " + bubbleRadius * 100)

            bodies.add(CircleBody(world, Vec2(x, y), bubbleRadius, 0.05f, density)) //TODO set increasedRadius formula
        }
        this.scaleX = scaleX
        this.scaleY = scaleY
        createBorders()

        return bodies
    }

    fun move() {

        val newTime = System.currentTimeMillis()
        if (oldTime == 0L) oldTime = newTime

        val delta = (newTime-oldTime)/1000f
        oldTime = newTime

        toBeIncrease.forEach { it.circleBody.resize(delta/ resizeDuration, bodies.size, canZoom, numberBubbleActive) }
        toBeDescrease.forEach { it.circleBody.resize(delta/ resizeDuration, bodies.size, canZoom, numberBubbleActive) }


        world.step(if (centerImmediately) 0.035f else step, 11, 11)
        bodies.forEach { move(it) }
        toBeIncrease.removeAll(toBeIncrease.filter { it.circleBody.finished })
        toBeDescrease.removeAll(toBeDescrease.filter { it.circleBody.finished })

        stepsCount++
        if (stepsCount >= 10) {
            centerImmediately = false
        }


    }

    fun getBodies(): ArrayList<CircleBody> {
        return bodies
    }


    fun swipe(x: Float, y: Float) {
        if (Math.abs(gravityCenter.x) < 2) gravityCenter.x += -x
        if (Math.abs(gravityCenter.y) < 0.5f / scaleY) gravityCenter.y += y
        increasedGravity = standardIncreasedGravity * Math.abs(x * 13) * Math.abs(y * 13)
        touch = true
    }

    fun release() {
        gravityCenter.setZero()
        touch = false
        increasedGravity = standardIncreasedGravity
    }

    fun clear() {
        borders.forEach { world.destroyBody(it.itemBody) }
        bodies.forEach { world.destroyBody(it.physicalBody) }
        borders.clear()
        bodies.clear()
    }

    fun resize(item: MyItem, allItems: ArrayList<MyItem>): Boolean {

        if (selectedBodies.size >= maxSelectedCount ?: bodies.size && !item.circleBody.increased) return false

        if (item.circleBody.isBusy) return false

        item.circleBody.defineState(true)

        var otherItems = ArrayList<MyItem>()

        for (circle: MyItem in allItems) {
            if (circle.circleBody != item.circleBody) {
                circle.circleBody.defineDecreasedState()

                otherItems.add(circle)
            }
        }
        numberBubbleActive = allItems.size
        canZoom = true
        otherItems
                .filter {
                    it.radius <= 0.1f //If there is a bubble with radius == 0  the click is blocked
                }
                .forEach { canZoom = false; numberBubbleActive-- }

        otherItems
                .filter {
                    it.radius > 0.1f && it.radius < bubbleRadius * (allItems.size - 1)//If there is a bubble with radius == 0  the click is blocked
                }
                .forEach { canZoom = true }


        if (item.radius <= 0.1) {
            canZoom = true
        }

        for(circle in otherItems){
            circle.circleBody.resizeStart()
        }

        toBeDescrease = otherItems
        item.circleBody.resizeStart()
        toBeIncrease.add(item)

        return true
    }

    private fun createBorders() {
        borders = arrayListOf(
                Border(world, Vec2(0f, 0.5f / scaleY), Border.HORIZONTAL),
                Border(world, Vec2(0f, -0.5f / scaleY), Border.HORIZONTAL)
        )
    }

    private fun move(body: CircleBody) {
        body.physicalBody.apply {
            body.isVisible = centerImmediately.not()
            val direction = gravityCenter.sub(position)
            val distance = direction.length()
            val gravity = if (body.increased) 1.3f * currentGravity else currentGravity
            if (distance > step * 200) {
                applyForce(direction.mul(gravity / distance.sqr()), position)
            }
        }
    }

    private fun interpolate(start: Float, end: Float, f: Float) = start + f * (end - start)

}