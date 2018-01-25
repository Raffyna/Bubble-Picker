package com.igalata.bubblepicker.physics

import android.util.Log
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import java.util.*

/**
 * Created by irinagalata on 1/26/17.
 */
class CircleBody( private val world: World, var position: Vec2, var radius: Float, var increasedRadius: Float, var density: Float) {

    var isIncreasing = false

    var isDecreasing = false

    var toBeIncreased = false

    var toBeDecreased = false

    val finished: Boolean
        get() = !toBeIncreased && !toBeDecreased && !isIncreasing && !isDecreasing

    val isBusy: Boolean
        get() = isIncreasing || isDecreasing

    lateinit var physicalBody: Body

    var increasedAmount = 0f
    var oldRadius = 0f

    var increased = false

    var isVisible = true

    private val margin = 0.01f
    private val damping = 25f
    private val shape: CircleShape
        get() = CircleShape().apply {
            m_radius = radius + margin
            m_p.setZero()
        }

    private val fixture: FixtureDef
        get() = FixtureDef().apply {
            this.shape = this@CircleBody.shape
            this.density = this@CircleBody.density
        }

    private val bodyDef: BodyDef
        get() = BodyDef().apply {
            type = BodyType.DYNAMIC
            this.position = this@CircleBody.position
        }

    init {
        while (true) {
            if (world.isLocked.not()) {
                initializeBody()
                break
            }
        }
    }

    private fun initializeBody() {
        physicalBody = world.createBody(bodyDef).apply {
            createFixture(fixture)
            linearDamping = damping
        }
    }

    /**
     * Increase or decrease the bubble
     */
    fun resize(step: Float, bubblesNumber: Int, canZoom: Boolean, numberBubbleActive:Int ) {
        if (toBeDecreased)
            decrease(step, bubblesNumber, canZoom, numberBubbleActive)
        else if (toBeIncreased)
            increase(step, bubblesNumber, canZoom, numberBubbleActive)
    }

    fun decrease(step: Float, bubblesNumber: Int, canZoom: Boolean, numberBubbleActive: Int) {
        isDecreasing = true
        if (radius > 0.1) {
            increasedAmount +=step
            radius = (oldRadius-((bubblesNumber - 1) / 100f))*(increasedAmount) + oldRadius*(1f-increasedAmount)

            reset()
            if(increasedAmount >= 1f){
                increased = false
                increasedAmount=0f
                clear()
            }
        }else
        {
            clear()
        }
    }

    fun resizeStart(){
        increasedAmount = 0f
        oldRadius = radius
    }

    fun increase(step: Float, bubblesNumber: Int, canZoom: Boolean, numberBubbleActive:Int) {

        if (canZoom) {

            isIncreasing = true
            increasedAmount += step
            radius = (oldRadius+((bubblesNumber - 1) * (numberBubbleActive-1)) / 100f)*(increasedAmount) + oldRadius*(1f-increasedAmount)

            reset()
            if(increasedAmount >= 1f) {
                increased = true
                increasedAmount = 0f
                clear()
            }
        }
    }

    private fun reset() {
        physicalBody.fixtureList?.shape?.m_radius = radius + margin
    }

    fun defineState(tobeIncrease: Boolean) {
        if (tobeIncrease) {
//            toBeIncreased = !increased
//            toBeDecreased = increased
            toBeIncreased = true
            toBeDecreased = false
        } else {
            toBeIncreased = false
            toBeDecreased = true
        }

    }

    fun defineDecreasedState() {
        toBeIncreased = false
        toBeDecreased = true
    }

    private fun clear() {
        toBeIncreased = false
        toBeDecreased = false
        isIncreasing = false
        isDecreasing = false
    }

}