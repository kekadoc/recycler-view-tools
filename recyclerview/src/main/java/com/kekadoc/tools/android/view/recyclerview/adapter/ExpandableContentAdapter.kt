package com.kekadoc.tools.android.view.recyclerview.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.kekadoc.tools.android.AndroidUtils.ThemeColor
import com.kekadoc.tools.android.AndroidUtils.dpToPx
import com.kekadoc.tools.android.AndroidUtils.getThemeColor
import com.kekadoc.tools.android.view.Expandable
import com.kekadoc.tools.android.view.recyclerview.R
import java.util.*

abstract class ExpandableContentAdapter<T, VH : ExpandableContentAdapter.ViewHolder<T>> : ListItemsAdapter<T, VH> {

    var isMultiExpandable = false
    private val expanded: MutableSet<VH> = hashSetOf()

    constructor(diffCallback: DiffUtil.ItemCallback<T>) : super(diffCallback)
    constructor(config: AsyncDifferConfig<T>) : super(config)

    protected abstract fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): VH

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vh: VH = onCreateViewHolder(parent.context, parent, viewType)
        vh.events = object : ExpandableContentItemView.Events {
            override fun onExpanded() {
                this@ExpandableContentAdapter.onExpanded(vh, vh.item)
                if (!isMultiExpandable) expanded.forEach { it.collapse() }
                expanded.add(vh)
            }
            override fun onCollapsed() {
                this@ExpandableContentAdapter.onCollapsed(vh, vh.item)
                expanded.remove(vh)
            }
            override fun isExpandable(): Boolean {
                return this@ExpandableContentAdapter.isExpandable(vh, vh.item)
            }
        }
        return vh
    }

    protected open fun isExpandable(holder: VH, item: T?): Boolean {
        return true
    }
    protected open fun onExpanded(holder: VH, item: T?) {}
    protected open fun onCollapsed(holder: VH, item: T?) {}

    abstract class ViewHolder<T>(context: Context) : ListItemsAdapter.ViewHolder<T>(ExpandableContentItemView(context)), Expandable {

        internal var events: ExpandableContentItemView.Events? = null

        init {
            view.apply {
                layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                )
                setTopContentView(onCreateTopContent(context, topContentView))
                setBackContentView(onCreateBackContent(context, backContentView))
                events = object : ExpandableContentItemView.Events {
                    override fun onExpanded() {
                        this@ViewHolder.onExpanded()
                        this@ViewHolder.events?.onExpanded()
                    }
                    override fun onCollapsed() {
                        this@ViewHolder.onCollapsed()
                        this@ViewHolder.events?.onCollapsed()
                    }
                    override fun isExpandable(): Boolean {
                        return this@ViewHolder.isExpandable() && (this@ViewHolder.events?.isExpandable() ?: true)
                    }
                }
            }
        }

        protected abstract fun onCreateTopContent(context: Context, parent: FrameLayout): View
        protected abstract fun onCreateBackContent(context: Context, parent: FrameLayout): View

        protected open fun onCollapsed() {}
        protected open fun onExpanded() {}
        protected open fun isExpandable(): Boolean {
            return true
        }

        override fun isExpanded(): Boolean {
            return view.isExpanded()
        }
        final override fun expand() {
            view.expand()
        }
        final override fun collapse() {
            view.collapse()
        }
        final override fun toggle() {
            view.toggle()
        }

        final override fun getView(): ExpandableContentItemView {
            return super.getView() as ExpandableContentItemView
        }

    }

    class ExpandableContentItemView internal constructor(context: Context) : LinearLayout(context, null, 0, 0), Expandable {

        internal interface Events {
            fun onExpanded()
            fun onCollapsed()
            fun isExpandable(): Boolean
        }

        var backContentView: FrameLayout
        var topContentView: FrameLayout

        private var expanded = false
        internal var events: Events? = null

        init {
            inflate(context, R.layout.expandable_content_item_view, this)
            backContentView = findViewById(R.id.frameLayout_content)
            topContentView = findViewById(R.id.frameLayout_content_title)

            setBackgroundColor(getThemeColor(context, ThemeColor.SURFACE))
            elevation = dpToPx(context, 4f)
            clipChildren = false
            clipToPadding = false
            orientation = VERTICAL
        }

        fun invokeToggleAction() {
            if (expanded) collapse()
            else if (events?.isExpandable() != false) expand()
        }

        fun setBackContentView(view: View?) {
            backContentView.removeAllViews()
            backContentView.addView(view, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        fun setTopContentView(view: View?) {
            topContentView.removeAllViews()
            topContentView.addView(view, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            topContentView.setOnClickListener {
                invokeToggleAction()
            }
        }

        override fun isExpanded(): Boolean {
            return expanded
        }
        override fun collapse() {
            val transition: Transition = ChangeBounds()
            transition.duration = 200L
            val view = if (this.parent == null) this else (this.parent as ViewGroup)
            TransitionManager.beginDelayedTransition(view, transition)

            backContentView.visibility = GONE
            this.expanded = false
            events?.onCollapsed()
        }
        override fun expand() {
            val transition: Transition = ChangeBounds()
            transition.duration = 200L
            val view = if (this.parent == null) this else (this.parent as ViewGroup)
            TransitionManager.beginDelayedTransition(view, transition)

            backContentView.visibility = VISIBLE
            this.expanded = true
            events?.onExpanded()
        }

        companion object {
            private const val TAG = "ExpandableContentItemView-TAG"
        }

    }

}