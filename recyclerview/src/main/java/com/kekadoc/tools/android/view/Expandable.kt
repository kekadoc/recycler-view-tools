package com.kekadoc.tools.android.view

interface Expandable {
    fun isExpanded(): Boolean
    fun expand()
    fun collapse()
    fun toggle() {
        if (isExpanded()) collapse()
        else expand()
    }
}