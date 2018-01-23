package com.igalata.bubblepicker.model

import com.igalata.bubblepicker.physics.CircleBody

/**
 * Created by rcerciello on 22/01/2018.
 */

data class PercentageItemsModel(val id: String, val percentage: Int) {

    var _id: String = id
    get() = _id

    var _percentage: Int = percentage
    get() = _percentage


}

