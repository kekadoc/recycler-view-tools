package com.kekadoc.tools.android.view.recyclerview

import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kekadoc.tools.android.AndroidUtils.isUiThread
import com.kekadoc.tools.android.AndroidUtils.runInMainThread
import com.kekadoc.tools.data.ListDataProvider

fun <D, VH : RecyclerView.ViewHolder> ListAdapter<D, VH>.notifyData(data: ListDataProvider<D>?, callback: Runnable? = null) {
    submitList(data?.getListData(), callback)
}
fun <D, VH : RecyclerView.ViewHolder> ListAdapter<D, VH>.notifyData(data: List<D>?, callback: Runnable? = null) {
    submitList(data, callback)
}

object RecyclerViewUtils {

    @JvmStatic
    fun <T, VH : RecyclerView.ViewHolder?> notifyOnUiThread(
        recyclerView: RecyclerView? = null,
        adapter: ListAdapter<T, VH>?,
        data: List<T>?,
        callback: Runnable? = null
    ) {
        if (adapter == null) {
            callback?.run()
            return
        }
        if (isUiThread()) adapter.submitList(data, callback) else {
            val submit = Runnable { adapter.submitList(data) }
            recyclerView?.post(submit) ?: runInMainThread(submit)
        }
    }

    @JvmStatic
    fun <T, VH : RecyclerView.ViewHolder?> notifyOnUiThread(
        recyclerView: RecyclerView? = null,
        adapter: ListAdapter<T, VH>?,
        data: ListDataProvider<T>?,
        callback: Runnable? = null
    ) {
        val d = data?.getListData()
        notifyOnUiThread(recyclerView, adapter, d, callback)
    }

    @JvmStatic
    fun <T, VH : RecyclerView.ViewHolder?> notify(
        adapter: ListAdapter<T, VH>?,
        data: List<T>?,
        callback: Runnable?
    ) {
        if (adapter == null) {
            callback?.run()
            return
        }
        adapter.submitList(data, callback)
    }

    @JvmStatic
    fun <T, VH : RecyclerView.ViewHolder?> notify(
        adapter: ListAdapter<T, VH>?,
        data: ListDataProvider<T>?,
        callback: Runnable?
    ) {
        val d = data?.getListData()
        notify(adapter, d, callback)
    }

}