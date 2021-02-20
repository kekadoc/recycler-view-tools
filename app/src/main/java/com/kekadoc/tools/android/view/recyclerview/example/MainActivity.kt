package com.kekadoc.tools.android.view.recyclerview.example

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.snackbar.Snackbar
import com.kekadoc.tools.android.AndroidUtils
import com.kekadoc.tools.android.ThemeColor
import com.kekadoc.tools.android.animation.alpha
import com.kekadoc.tools.android.animation.animator
import com.kekadoc.tools.android.animation.scale
import com.kekadoc.tools.android.dpToPx
import com.kekadoc.tools.android.graph.ColorUtils
import com.kekadoc.tools.android.shaper.roundAllCorners
import com.kekadoc.tools.android.shaper.shapedDrawable
import com.kekadoc.tools.android.themeColor
import com.kekadoc.tools.android.view.recyclerview.SpacesItemDecoration
import com.kekadoc.tools.android.view.recyclerview.adapter.*
import com.kekadoc.tools.android.view.recyclerview.notifyData

fun <T> arrayListOf(builder: MutableList<T>.() -> Unit): MutableList<T> {
    val list = arrayListOf<T>()
    builder.invoke(list)
    return list
}

private const val TAG: String = "RecyclerViewAdapters-TAG"

class MainActivity : AppCompatActivity() {

    private fun getListData(): List<Data> {
        return arrayListOf {
            for (i in 0..20) add(Data("Data #$i", ColorUtils.getRandomColor(), i))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            addItemDecoration(SpacesItemDecoration(dpToPx(16f).toInt()))
        }
        val dropSideAdapter = DropSideAdapterImpl()
        val selectableItemAdapter = SelectableAdapterImpl()
        val expandableContentAdapter = ExpandableContentAdapterImpl()
        val expandableItemsAdapter = ExpandableItemsAdapterImpl(this)
        val defAdapter = DefAdapter()

        val currentAdapter = expandableItemsAdapter.apply {
            recyclerView.adapter = this
            notifyData(getListData())
        }
    }

}

data class Data(val name: String, @ColorInt val color: Int, val type: Int)

class DiffCallback : DiffUtil.ItemCallback<Data>() {
    override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
        return oldItem === newItem
    }
    override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
        return oldItem == newItem
    }
}

class DropSideViewHolderImpl(context: Context, type: DropSideAdapter.Type) : DropSideAdapter.ViewHolder<Data>(context, type) {

    companion object {
        private const val TAG: String = "DropSideViewHolderImpl-TAG"
    }

    lateinit var textView: TextView
    lateinit var button: Button

    override fun onCreateTopContent(
        context: Context,
        parent: MaterialCardView,
        type: DropSideAdapter.Type
    ): View {
        parent.apply {
            elevation = context.dpToPx(6f)
            radius = context.dpToPx(16f)
            strokeWidth = context.dpToPx(4f).toInt()
        }
        return LayoutInflater.from(context).inflate(R.layout.drop_side_item_content, parent, false).apply {
            textView = findViewById(R.id.textView)
        }
    }
    override fun onCreateBackContent(
        context: Context,
        parent: FrameLayout,
        type: DropSideAdapter.Type
    ): View {
        return LayoutInflater.from(context).inflate(
            R.layout.drop_side_item_background,
            parent,
            false
        ).apply {
            button = findViewById(R.id.button)
        }
    }
    override fun onItemChange(oldItem: Data?, newItem: Data?) {
        val name = newItem?.name ?: "Null"
        textView.text = name
        button.setOnClickListener {
            Snackbar.make(it, name, Snackbar.LENGTH_LONG).show()
        }
        getTopContentView().apply {
            strokeColor = newItem?.color ?: Color.TRANSPARENT
        }
    }

}
class DropSideAdapterImpl : DropSideAdapter<Data, DropSideViewHolderImpl>(DiffCallback()) {
    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): DropSideViewHolderImpl {
        return DropSideViewHolderImpl(
            context,
            if (viewType == 0) Type.DROP_LEFT else Type.DROP_RIGHT
        )
    }
    override fun getItemViewType(position: Int): Int {
        return if (position % 2 == 0) 0 else 1
    }
}


class SelectableHolder(itemView: View) : SelectableItemAdapter.ViewHolder<Data>(itemView) {

    init {
        val drawable = shapedDrawable(itemView.context) {
            shape {
                roundAllCorners(dpToPx(16f))
            }
            setTint(Color.BLUE)
            setElevation(dpToPx(8f))
            setRippleColor(Color.RED)
        }
        itemView.background = drawable
    }

    override fun onFocused() {
        if (!view.isShown) {
            view.apply {
                scaleX = 1.05f
                scaleY = 1.05f
                alpha = 1.0f
            }
        } else itemView.animator {
            set {
                play(
                    scale(toX = 1.05f, toY = 1.05f) { duration(300L) }
                ).with(
                    alpha(to = 1.0f) { duration(300L) }
                )
            }
        }.start()
    }
    override fun onHided() {
        if (!view.isShown) {
            view.apply {
                scaleX = 0.95f
                scaleY = 0.95f
                alpha = 0.7f
            }
        } else itemView.animator {
            set {
                play(
                    scale(toX = 0.95f, toY = 0.95f) { duration(300L) }
                ).with(
                    alpha(to = 0.7f) { duration(300L) }
                )
            }
        }.start()
    }
    override fun onNormal() {
        if (!view.isShown) {
            view.apply {
                scaleX = 1.0f
                scaleY = 1.0f
                alpha = 1.0f
            }
        } else itemView.animator {
            set {
                play(
                    scale(toX = 1.0f, toY = 1.0f) { duration(300L) }
                ).with(
                    alpha(to = 1f) { duration(300L) }
                )
            }
        }.start()
    }

    override fun onItemChange(oldItem: Data?, newItem: Data?) {
        view.findViewById<TextView>(R.id.textView)?.text = newItem?.name
    }

}
class SelectableAdapterImpl : SelectableItemAdapter<Data, SelectableHolder>(DiffCallback()) {
    override fun createHolder(parent: ViewGroup, viewType: Int): SelectableHolder {
        return SelectableHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.drop_side_item_content,
                parent,
                false
            )
        )
    }
}


class ExpandableContentViewHolderImpl(context: Context) : ExpandableContentAdapter.ViewHolder<Data>(context) {

    lateinit var textView: TextView
    lateinit var button: Button

    override fun onCreateTopContent(context: Context, parent: FrameLayout): View {
        parent.apply {
            elevation = context.dpToPx(6f)
            background = shapedDrawable(context) {
                shape {
                    setBottomLeftCorner(CornerFamily.CUT, dpToPx(16f))
                    setBottomRightCorner(CornerFamily.CUT, dpToPx(16f))
                }
                setTint(themeColor(ThemeColor.PRIMARY))
                setElevation(dpToPx(4f))
                setRippleColor(themeColor(ThemeColor.RIPPLE))
            }
            //radius = context.dpToPx(16f)
            //strokeWidth = context.dpToPx(4f).toInt()
        }
        return LayoutInflater.from(context).inflate(R.layout.drop_side_item_content, parent, false).apply {
            textView = findViewById(R.id.textView)
        }
    }

    override fun onCreateBackContent(context: Context, parent: FrameLayout): View {
        return LayoutInflater.from(context).inflate(
                R.layout.drop_side_item_background,
                parent,
                false
        ).apply {
            button = findViewById(R.id.button)
        }
    }

    override fun onItemChange(oldItem: Data?, newItem: Data?) {
        val name = newItem?.name ?: "Null"
        textView.text = name
        button.setOnClickListener {
            Snackbar.make(it, name, Snackbar.LENGTH_LONG).show()
        }
       /* getTopContentView().apply {
            strokeColor = newItem?.color ?: Color.TRANSPARENT
        }*/
    }

}
class ExpandableContentAdapterImpl : ExpandableContentAdapter<Data, ExpandableContentViewHolderImpl>(DiffCallback()) {

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): ExpandableContentViewHolderImpl {
        return ExpandableContentViewHolderImpl(context)
    }
}

class GroupHolder(context: Context) : ExpandableItemsAdapter.GroupViewHolder<Data>(context)   {
    override fun getContentView(context: Context, viewGroup: MaterialCardView): View {
        return LayoutInflater.from(context).inflate(R.layout.drop_side_item_content, viewGroup, false).apply {
            /*background = shapedDrawable {
                setTint(Color.WHITE)
                setElevation(dpToPx(4f))
                shape {
                    roundAllCorners(dpToPx(16f))
                }
                setRippleColor(Color.RED)
            }*/
        }
    }
    override fun onItemChange(oldItem: Data?, newItem: Data?) {
        //view.findViewById<TextView>(R.id.textView).text = newItem?.name
    }
}
class ChildHolder(itemView: View) : ListItemsAdapter.ViewHolder<String>(itemView) {
    override fun onItemChange(oldItem: String?, newItem: String?) {
        view.findViewById<Button>(R.id.button).apply {
            text = newItem
            setOnClickListener {

            }
        }
    }
}

class ExpandableItemsAdapterImpl(context: Context) : ExpandableItemsAdapter<Data, String, GroupHolder, ChildHolder>(DiffCallback()) {

    init {
        iconStateHelper = IconStateHelper(createExpandDrawable(context), IconStateHelper.IconType.LEFT)
    }

    override fun onCreateGroupViewHolder(context: Context, parent: ViewGroup, viewType: Int): GroupHolder {
        return GroupHolder(context)
    }
    override fun onCreateChildViewHolder(context: Context, parent: ViewGroup, viewType: Int): ChildHolder {
        val childView = LayoutInflater.from(context).inflate(R.layout.drop_side_item_background, parent, false)
        return ChildHolder(childView)
    }

    override fun getChild(groupIndex: Int, childIndex: Int): String {
        return getItem(groupIndex).name + "$" + childIndex
    }
    override fun getChildCount(groupIndex: Int): Int {
        if (groupIndex % 2 == 0) return 0
        return 3
    }

}



class DefViewHolder(context: Context) : RecyclerView.ViewHolder(TextView(context).apply {
    text = "Text"
}) {

}
class DefAdapter : RecyclerView.Adapter<DefViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DefViewHolder {
        return DefViewHolder(parent.context)
    }
    override fun onBindViewHolder(holder: DefViewHolder, position: Int) {
        holder.itemView.setBackgroundColor(Color.RED)
    }
    override fun getItemCount(): Int {
        return 10
    }
}