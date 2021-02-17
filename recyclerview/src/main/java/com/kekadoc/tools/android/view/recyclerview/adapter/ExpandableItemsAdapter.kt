package com.kekadoc.tools.android.view.recyclerview.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.card.MaterialCardView
import com.kekadoc.tools.android.AndroidUtils.ThemeColor
import com.kekadoc.tools.android.AndroidUtils.dpToPx
import com.kekadoc.tools.android.AndroidUtils.getDimension
import com.kekadoc.tools.android.AndroidUtils.getDrawable
import com.kekadoc.tools.android.view.Expandable
import com.kekadoc.tools.android.view.ViewUtils.dpToPx
import com.kekadoc.tools.android.view.recyclerview.R
import com.kekadoc.tools.android.view.recyclerview.viewholder.ItemViewHolder
import java.util.*

abstract class ExpandableItemsAdapter<
        Group,
        Child,
        GVH : ExpandableItemsAdapter.GroupViewHolder<Group>,
        CVH : ItemViewHolder<Child>
        > : ListItemsAdapter<Group, GVH> {

    companion object {
        private const val TAG: String = "ExpItemsAdapter-TAG"

        const val STATE_EMPTY = android.R.attr.state_empty
        const val STATE_EXPANDED = android.R.attr.state_expanded

        @JvmStatic fun createExpandDrawable(context: Context): Drawable {
            val st = StateListDrawable()
            val expand_less = getDrawable(context, R.drawable.expand_less_black_48x48)
            val expand_more = getDrawable(context, R.drawable.expand_more_black_48x48)
            val expand_empty = getDrawable(context, R.drawable.remove_black_48x48)
            expand_less!!.setTint(ThemeColor.ON_SURFACE.color)
            expand_more!!.setTint(ThemeColor.ON_SURFACE.color)
            expand_empty!!.setTint(ThemeColor.ON_SURFACE.color)
            st.addState(intArrayOf(STATE_EXPANDED, -STATE_EMPTY), expand_less)
            st.addState(intArrayOf(-STATE_EXPANDED, -STATE_EMPTY), expand_more)
            st.addState(intArrayOf(STATE_EMPTY), expand_empty)
            return st
        }
    }

    var isSingleExpand = true
    private val expandedViews: MutableList<GroupViewHolder<Group>> = ArrayList()
    var iconStateHelper: IconStateHelper? = null

    constructor(diffCallback: DiffUtil.ItemCallback<Group>) : super(diffCallback)
    constructor(config: AsyncDifferConfig<Group>) : super(config)


    abstract fun onCreateGroupViewHolder(context: Context, parent: ViewGroup, viewType: Int): GVH
    abstract fun onCreateChildViewHolder(context: Context, parent: ViewGroup, viewType: Int): CVH

    protected abstract fun getChildCount(groupIndex: Int): Int
    protected abstract fun getChild(groupIndex: Int, childIndex: Int): Child

    protected open fun getChildViewType(groupPosition: Int, position: Int): Int {
        return 0
    }

    protected open fun onCollapsed(holder: GVH) {}
    protected open fun onExpanded(holder: GVH) {}
    protected open fun isExpandable(holder: GVH): Boolean {
        return true
    }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GVH {
        val vh = onCreateGroupViewHolder(parent.context, parent, viewType)
        vh.setChildAdapter(object : ChildAdapter<CVH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CVH {
                Log.e(TAG, "onCreateViewHolder: ")
                return onCreateChildViewHolder(parent.context, parent, viewType)
            }

            override fun onBindViewHolder(holder: CVH, position: Int) {
                holder.item = getChild(groupIndex, position)
            }
        })
        vh.setOperator(object : ExpandableContentItemView.Events {
            override fun isExpandable(): Boolean {
                return this@ExpandableItemsAdapter.isExpandable(vh) && vh.adapter?.itemCount != 0
            }

            override fun onExpanded() {
                if (isSingleExpand) expandedViews.forEach { it.view.collapse() }
                expandedViews.add(vh)
                this@ExpandableItemsAdapter.onExpanded(vh)
            }

            override fun onCollapsed() {
                expandedViews.remove(vh)
                this@ExpandableItemsAdapter.onCollapsed(vh)
            }
        })
        vh.view.groupView.setIconStateHelper(iconStateHelper)
        return vh
    }
    final override fun onBindViewHolder(holder: GVH, position: Int) {
        super.onBindViewHolder(holder, position)
        val childCount = getChildCount(position)
        holder.adapter?.setData(position, childCount)
        if (childCount == 0) holder.view.groupView.setIconState(STATE_EMPTY)
    }

    open class IconStateHelper(val icon: Drawable?, val iconType: IconType = IconType.NONE) {

        enum class IconType {
            LEFT, RIGHT, NONE
        }

        open fun getIconBounds(view: View): Rect {
            val type = iconType
            val w = view.measuredWidth
            val h = view.measuredHeight
            var l = 0
            var t = 0
            var r = 0
            var b = 0
            val s = getIconSize(view)
            if (type == IconType.LEFT) {
                l = (s / 2f).toInt()
                t = ((h - s) / 2f).toInt()
                r = l + s
                b = h - t
            }
            if (type == IconType.RIGHT) {
                l = w - s * 2
                t = ((h - s) / 2f).toInt()
                r = (w - s / 2f).toInt()
                b = h - t
            }
            return Rect(l, t, r, b)
        }

        open fun getIconSize(view: View): Int {
            return getDimension(view.context, R.dimen.size_icon_medium).toInt()
        }

        open fun decorView(view: MaterialCardView) {
            val iconSize = getIconSize(view)
            view.setPadding(iconSize * 2, 0, (iconSize * 0.5f).toInt(), 0)
        }

    }

    @Suppress("LeakingThis")
    abstract class GroupViewHolder<T>(context: Context) : ViewHolder<T>(ExpandableContentItemView(context)) {

        internal var adapter: ChildAdapter<*>? = null

        init {
            view.apply {
                setGroupView(getContentView(context, view.groupView))
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }

        internal fun setChildAdapter(adapter: ChildAdapter<*>) {
            this.adapter = adapter
            view.setAdapter(adapter)
        }

        protected abstract fun getContentView(context: Context, viewGroup: MaterialCardView): View

        internal fun setOperator(operator: ExpandableContentItemView.Events) {
            view.operator = operator
        }

        override fun getView(): ExpandableContentItemView {
            return super.getView() as ExpandableContentItemView
        }

    }

    internal class GroupView : MaterialCardView {

        private var iconState: Drawable? = null
        private var iconStateHelper: IconStateHelper? = null

        constructor(context: Context) : super(context)
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
        constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

        init {
            clipChildren = false
            clipToPadding = false
            cardElevation = dpToPx(4f)
        }

        fun setIconStateHelper(helper: IconStateHelper?) {
            this.iconStateHelper = helper
            iconState = if (helper == null) null else helper.icon?.constantState!!.newDrawable().mutate()
        }

        override fun onAttachedToWindow() {
            iconStateHelper?.decorView(this)
            super.onAttachedToWindow()
        }

        override fun dispatchDraw(canvas: Canvas) {
            super.dispatchDraw(canvas)
            iconStateHelper?.let {
                if (iconState == null || it.iconType == IconStateHelper.IconType.NONE) return
                val bounds = it.getIconBounds(this)
                iconState!!.bounds = bounds
                iconState!!.setTint(Color.RED)
                iconState!!.draw(canvas)
            }

        }

        fun setIconState(vararg state: Int) {
            iconState?.let {
                it.state = state
            }
        }

    }

    internal abstract class ChildAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

        protected var groupIndex: Int = -1
        protected var childCount = 0

        fun setData(groupIndex: Int = this.groupIndex, childCount: Int) {
            this.groupIndex = groupIndex
            this.childCount = childCount
            notifyDataSetChanged()
        }

        final override fun getItemCount(): Int {
            return childCount
        }

    }

    @SuppressLint("ViewConstructor")
    class ExpandableContentItemView internal constructor(context: Context) : LinearLayout(context), Expandable {

        interface Events {
            fun isExpandable(): Boolean
            fun onExpanded()
            fun onCollapsed()
        }

        internal lateinit var operator: Events

        internal val groupView: GroupView
        private val childStub: ViewStub
        private var recyclerView: RecyclerView? = null
        private var adapter: RecyclerView.Adapter<*>? = null
        private var expanded = false

        init {
            inflate(context, R.layout.expandable_recycler_view_item, this)
            groupView = findViewById(R.id.groupView)
            childStub = findViewById(R.id.ViewStub_child)

            elevation = dpToPx(context, 4f)
            clipChildren = false
            clipToPadding = false
            orientation = VERTICAL
        }

        fun setAdapter(adapter: RecyclerView.Adapter<*>) {
            this.adapter = adapter
            recyclerView?.adapter = adapter
        }
        fun invokeToggleAction() {
            if (expanded) collapse()
            else if (operator.isExpandable()) expand()
        }
        fun setGroupView(view: View) {
            Log.e(TAG, "setGroupView: " + view)
            groupView.removeAllViews()
            groupView.addView(view, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            groupView.setOnClickListener {
                invokeToggleAction()
            }
        }

        override fun isExpanded(): Boolean {
            return expanded
        }
        override fun collapse() {

            val transition: Transition = ChangeBounds()
            transition.duration = 200L
            TransitionManager.beginDelayedTransition((this.parent as ViewGroup), transition)

            recyclerView!!.visibility = GONE
            groupView.setIconState(-STATE_EXPANDED, -STATE_EMPTY)
            expanded = false
            operator.onCollapsed()
        }
        override fun expand() {
            val transition: Transition = ChangeBounds()
            transition.duration = 400L
            transition.interpolator = OvershootInterpolator()
            TransitionManager.beginDelayedTransition((this.parent as ViewGroup), transition)

            if (recyclerView == null) {
                childStub.inflate()
                recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
                    adapter = this@ExpandableContentItemView.adapter
                }
            }
            recyclerView!!.visibility = VISIBLE
            groupView.setIconState(STATE_EXPANDED, -STATE_EMPTY)
            expanded = true
            operator.onExpanded()
        }

    }

}