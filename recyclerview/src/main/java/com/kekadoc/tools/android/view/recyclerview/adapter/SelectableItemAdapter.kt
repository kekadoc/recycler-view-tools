package com.kekadoc.tools.android.view.recyclerview.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.kekadoc.tools.data.state.DataStatesCollector
import com.kekadoc.tools.data.state.StateKeeper
import com.kekadoc.tools.observer.Observing
import java.util.*

abstract class SelectableItemAdapter<T, VH : SelectableItemAdapter.ViewHolder<T>> : ListItemsAdapter<T, VH> {

    enum class State {
        FOCUSED, HIDED, NORMAL
    }

    private val focusability = Focusability()

    private val holderByView: MutableMap<View, VH> = LinkedHashMap()
    private val itemByHolder: MutableMap<VH, T?> = LinkedHashMap()

    constructor(diffCallback: DiffUtil.ItemCallback<T>) : super(diffCallback)
    constructor(config: AsyncDifferConfig<T>) : super(config)

    fun setMaxSelected(max: Int) {
        focusability.maxFocusable = max
    }
    /**
     *
     * Count Selected
     */
    fun getCountFocused(): Int = focusability.countFocused
    /**
     *
     * All Selected
     */
    fun getFocusedItems(): Collection<T> {
        val focused: MutableCollection<T> = ArrayList()
        for (keeper in focusability.focusable) focused.add(keeper.data)
        return focused
    }

    /**
     *
     */
    protected abstract fun createHolder(parent: ViewGroup, viewType: Int): VH

    private fun findHolder(view: View): VH? {
        return holderByView[view]
    }
    private fun findItem(holder: VH): T? {
        return itemByHolder[holder]
    }

    protected fun onClicked(vh: VH, item: T?) {}
    protected fun onFocused(vh: VH, item: T?) {}
    protected fun onUnfocused(vh: VH, item: T?) {}


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = createHolder(parent, viewType)
        holderByView[view.requireView()] = view
        view.requireView().layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
        view.view.setOnClickListener {
            focusability.invokeAction(view.item!!)
        }
        return view
    }
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.setStateKeeper(focusability.getStateKeeper(item))
        itemByHolder[holder] = item
    }
    override fun onCurrentListChanged(previousList: List<T>, currentList: List<T>) {
        focusability.clear()
        for (item in currentList) {
            focusability.add(item, State.NORMAL)
        }
    }


    private inner class StateKeeperImpl(t: T, initState: State) : StateKeeper.Default<T, State>(t, initState)
    private inner class Focusability : DataStatesCollector<T, State, StateKeeperImpl>() {

        internal val focusable: MutableSet<StateKeeperImpl> = HashSet()

        var maxFocusable = 1

        val countFocused: Int
            get() = focusable.size

        private fun hideAllNotFocused() {
            for (keeper in getAll()) {
                if (keeper.state == State.NORMAL) keeper.state = State.HIDED
            }
        }
        private fun resetAll() {
            for (keeper in getAll()) keeper.state = State.NORMAL
        }

        private fun onFocused() {
            hideAllNotFocused()
        }
        private fun onUnfocused() {
            resetAll()
        }

        fun invokeAction(data: T) {
            val keeper = Objects.requireNonNull(getStateKeeper(data))!!
            if (keeper.state == State.FOCUSED) keeper.state = State.NORMAL else {
                if (countFocused < maxFocusable) keeper.state =
                    State.FOCUSED else if (isFocused() && maxFocusable == 1) {
                    val focused = focusable.iterator().next()
                    keeper.state = State.FOCUSED
                    focused.state = State.NORMAL
                }
            }
        }

        fun isFocused(): Boolean = focusable.size > 0

        private fun addFocus(keeper: StateKeeperImpl) {
            val focused = isFocused()
            focusable.add(keeper)
            if (!focused && isFocused()) onFocused()
        }

        private fun removeFocus(keeper: StateKeeperImpl) {
            val focused = isFocused()
            focusable.remove(keeper)
            if (focused && !isFocused()) onUnfocused()
        }

        override fun onDataStateChange(keeper: StateKeeperImpl, oldState: State, newState: State) {
            super.onDataStateChange(keeper, oldState, newState)
            if (newState == State.FOCUSED) addFocus(keeper) else if (oldState == State.FOCUSED) removeFocus(
                keeper
            )
            if (isFocused()) hideAllNotFocused() else resetAll()
        }
        override fun onCreateStateKeeper(data: T, state: State): StateKeeperImpl {
            return StateKeeperImpl(data, state)
        }

    }

    abstract class ViewHolder<T>(itemView: View) : ListItemsAdapter.ViewHolder<T>(itemView) {
        var state: State? = null
            private set(value) {
                field = value
                when (state) {
                    State.FOCUSED -> onFocused()
                    State.HIDED -> onHided()
                    State.NORMAL -> onNormal()
                }
            }
        private var stateKeeper: StateKeeper<T, State>? = null
        private var observing: Observing? = null

        fun setStateKeeper(stateKeeper: StateKeeper<T, State>?) {
            this.stateKeeper = stateKeeper
            if (observing != null) observing!!.remove()
            observing = this.stateKeeper!!.observe { _: State?, newState: State ->
                this.state = newState
            }
            item = stateKeeper?.data
        }

        protected abstract fun onFocused()
        protected abstract fun onHided()
        protected abstract fun onNormal()

    }

}