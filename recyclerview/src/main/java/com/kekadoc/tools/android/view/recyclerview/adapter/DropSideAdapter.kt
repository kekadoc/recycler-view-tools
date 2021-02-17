package com.kekadoc.tools.android.view.recyclerview.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kekadoc.tools.android.animation.interpolation.BounceInterpolator
import com.kekadoc.tools.android.view.recyclerview.R
import java.util.*

abstract class DropSideAdapter<T, VH : DropSideAdapter.ViewHolder<T>> : ListItemsAdapter<T, VH> {

    companion object {
        private const val TAG = "DropSideView-TAG"
        var animationCollapseDuration = 350L
        var animationExpandDuration = 350L
    }

    enum class Type {
        DROP_LEFT,
        DROP_RIGHT
    }

    var isSingleExpand = true

    private val listener = ItemViewEventListener()
    private val expandedViews: MutableList<DropSideView> = ArrayList()

    constructor(diffCallback: DiffUtil.ItemCallback<T>) : super(diffCallback)
    constructor(config: AsyncDifferConfig<T>) : super(config)

    protected abstract fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): VH

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val viewHolder: VH = onCreateViewHolder(parent.context, parent, viewType)
        viewHolder.view.setEventListener(listener)
        return viewHolder
    }

    private inner class ItemViewEventListener : DropSideView.EventListener {
        override fun onCollapse(itemView: DropSideView) {
            expandedViews.remove(itemView)
        }
        override fun onExpand(itemView: DropSideView) {
            if (isSingleExpand) {
                for (expandedView in expandedViews) expandedView.collapse(true)
            }
            expandedViews.add(itemView)
        }
    }

    @Suppress("LeakingThis")
    abstract class ViewHolder<T> (context: Context, type: Type) : ListItemsAdapter.ViewHolder<T>(DropSideView(context, type)) {

        init {
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            view.setTopContentView(view = onCreateTopContent(context, getTopContentView(), type))
            view.setBackContentView(view = onCreateBackContent(context, getBackContentView(), type))
        }

        protected abstract fun onCreateTopContent(context: Context, parent: MaterialCardView, type: Type): View
        protected abstract fun onCreateBackContent(context: Context, parent: FrameLayout, type: Type): View

        abstract override fun onItemChange(oldItem: T?, newItem: T?)

        protected open fun onCollapse() {}
        protected open fun onExpand() {}

        protected fun getBackContentView() = view.backContentView
        protected fun getTopContentView() = view.topContentView

        final override fun getView(): DropSideView {
            return super.getView() as DropSideView
        }

    }

    @SuppressLint("ViewConstructor")
    class DropSideView internal constructor(context: Context, var type: Type = Type.DROP_LEFT) : FrameLayout(context, null, 0) {

        interface EventListener {
            fun onCollapse(itemView: DropSideView)
            fun onExpand(itemView: DropSideView)
        }

        private var expanded = false

        var backContentView: FrameLayout
            private set
        var topContentView: MaterialCardView
            private set

        private var listener: EventListener? = null

        init {

            val res = when(type) {
                Type.DROP_LEFT -> R.layout.drop_side_view_item_left
                Type.DROP_RIGHT -> R.layout.drop_side_view_item_right
            }
            inflate(context, res, this)

            clipChildren = false
            clipToPadding = false
            backContentView = findViewById(R.id.frameLayout_backView)
            topContentView = findViewById<MaterialCardView>(R.id.materialCardView_contentView).apply {
                setOnClickListener { switchCollapse(true) }
            }

        }

        fun expand(animate: Boolean) {
            var change = 0
            if (type == Type.DROP_LEFT) change = -backContentView.width
            if (type == Type.DROP_RIGHT) change = backContentView.width
            if (animate) topContentView.animate()
                .translationX(change.toFloat())
                .setDuration(animationExpandDuration)
                .setInterpolator(OvershootInterpolator())
                .start() else topContentView.translationX = change.toFloat()
            expanded = true
            listener?.onExpand(this)
        }
        fun collapse(animate: Boolean) {
            if (animate) topContentView.animate()
                .translationX(0f)
                .setDuration(animationCollapseDuration)
                .setInterpolator(BounceInterpolator(1))
                .start() else topContentView.translationX = 0f
            expanded = false
            listener?.onCollapse(this)
        }
        fun switchCollapse(animate: Boolean) {
            if (expanded) collapse(animate) else expand(animate)
        }

        fun setBackContentView(view: View?) {
            backContentView.removeAllViews()
            backContentView.addView(view)
        }
        fun setTopContentView(view: View?) {
            topContentView.removeAllViews()
            topContentView.addView(view)
        }

        fun setEventListener(listener: EventListener?) {
            this.listener = listener
        }

    }

}