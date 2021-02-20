package com.kekadoc.tools.android.view.recyclerview.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kekadoc.tools.android.lifecycle.addObserving
import com.kekadoc.tools.android.view.ViewUtils.findAllViews
import com.kekadoc.tools.android.view.recyclerview.viewholder.ItemViewHolder
import com.kekadoc.tools.observer.Observing
import java.util.*

fun <T, VH : ListItemsAdapter.ViewHolder<T>> listItemsAdapterOf(
    onCreateViewHolder: (parent: ViewGroup, viewType: Int) -> VH,
    onBindViewHolder: ((holder: VH, position: Int) -> Unit)? = null,
    diffCallback: DiffUtil.ItemCallback<T> = DefaultDiffUtilCallback()
) {
    val adapter = object : ListItemsAdapter<T, VH>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return onCreateViewHolder.invoke(parent, viewType)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            if (onBindViewHolder != null) onBindViewHolder.invoke(holder, position)
            else super.onBindViewHolder(holder, position)
        }
    }
}

abstract class ListItemsAdapter<T, VH : ListItemsAdapter.ViewHolder<T>> : ListAdapter<T, VH> {

    var recyclerView: RecyclerView? = null
    var lifecycleObserver: LifecycleObserver? = null
    var lifecycleObserving: Observing? = null

    constructor(diffCallback: DiffUtil.ItemCallback<T>) : super(diffCallback)
    constructor(config: AsyncDifferConfig<T>) : super(config)

    fun attachOnLifecycle(lifecycleOwner: LifecycleOwner): Observing {
        return attachOnLifecycle(lifecycleOwner.lifecycle)
    }
    fun attachOnLifecycle(lifecycle: Lifecycle): Observing {
        if (lifecycleObserver == null)
            lifecycleObserver = LifecycleEventObserver { source, event -> onLifecycleEvent(
                source,
                event
            ) }
        this.lifecycleObserving = lifecycle.addObserving(lifecycleObserver!!)
        return lifecycleObserving!!
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.item = getItem(position)
    }

    protected open fun onLifecycleEvent(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            lifecycleObserving?.remove()
            lifecycleObserver = null
            lifecycleObserving = null
        }
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        holder.invokeAttach()
    }
    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        holder.invokeDetach()
    }
    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.invokeRecycle()
    }
    override fun onFailedToRecycleView(holder: VH): Boolean {
        return holder.invokeFailedToRecycle()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    abstract class ViewHolder <T> (itemView: View) : ItemViewHolder<T>(itemView) {

        protected fun findAllViews() {
            view.findAllViews()
        }

        /**
         * @see [RecyclerView.Adapter.onViewAttachedToWindow]
         */
        protected open fun onAttach() {}
        /**
         * @see [RecyclerView.Adapter.onViewDetachedFromWindow]
         */
        protected open fun onDetach() {}
        /**
         * @see [RecyclerView.Adapter.onViewRecycled]
         */
        protected open fun onRecycled() {
            item = null
        }
        /**
         * @see [RecyclerView.Adapter.onFailedToRecycleView]
         */
        protected open fun onFailedToRecycle(): Boolean {
            item = null
            return false
        }

        internal fun invokeAttach() {
            onAttach()
        }
        internal fun invokeDetach() {
            onDetach()
        }
        internal fun invokeRecycle() {
            onRecycled()
        }
        internal fun invokeFailedToRecycle(): Boolean {
            return onFailedToRecycle()
        }

    }

}

abstract class InflatableListAdapter<T, VH : ListItemsAdapter.ViewHolder<T>> : ListItemsAdapter<T, VH> {

    @Transient
    private var inflater: LayoutInflater? = null

    constructor(diffCallback: DiffUtil.ItemCallback<T>) : super(diffCallback)
    constructor(config: AsyncDifferConfig<T>) : super(config)

    fun getInflater(context: Context): LayoutInflater {
        if (inflater == null) inflater = LayoutInflater.from(context)
        return inflater!!
    }

}

class DefaultDiffUtilCallback <T> : DiffUtil.ItemCallback<T>() {

    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
        return oldItem === newItem
    }
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
        return Objects.equals(oldItem, newItem)
    }

}