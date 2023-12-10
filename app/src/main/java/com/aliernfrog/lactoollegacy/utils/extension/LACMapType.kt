package com.aliernfrog.lactoollegacy.utils.extension

import android.content.Context
import com.aliernfrog.laclib.enum.LACMapType
import com.aliernfrog.lactoollegacy.R

fun LACMapType.getName(context: Context): String {
    return context.getString(
        when(this.index) {
            1 -> R.string.mapEdit_type_1
            2 -> R.string.mapEdit_type_2
            3 -> R.string.mapEdit_type_3
            4 -> R.string.mapEdit_type_4
            5 -> R.string.mapEdit_type_5
            else -> R.string.mapEdit_type_0
        }
    )
}