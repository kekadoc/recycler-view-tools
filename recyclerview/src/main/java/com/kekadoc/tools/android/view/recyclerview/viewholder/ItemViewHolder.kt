package com.kekadoc.tools.android.view.recyclerview.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.kekadoc.tools.android.view.ItemViewHolder

abstract class ItemViewHolder<T> constructor(itemView: View) : RecyclerView.ViewHolder(itemView), ItemViewHolder<T> {

    override var item: T? = null
        set(value) {
            val old = field
            field = value
            onItemChange(old, value)
        }

    override fun getView(): View {
        return itemView
    }

    protected abstract fun onItemChange(oldItem: T?, newItem: T?)

}