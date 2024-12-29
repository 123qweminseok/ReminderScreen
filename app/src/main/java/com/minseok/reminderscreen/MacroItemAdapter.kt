// MacroItemAdapter.kt
package com.minseok.reminderscreen.ui.macro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.minseok.reminderscreen.R
import com.minseok.reminderscreen.data.model.MacroItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MacroItemAdapter(
    private val onItemClick: (MacroItem) -> Unit,
    private val onDeleteClick: (MacroItem) -> Unit  // 삭제 콜백 변경

) : RecyclerView.Adapter<MacroItemAdapter.MacroItemViewHolder>() {

    private var items = mutableListOf<MacroItem>()  // MutableList로 변경

    inner class MacroItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvContent: TextView = view.findViewById(R.id.tvMacroItemContent)
        private val tvTime: TextView = view.findViewById(R.id.tvMacroItemTime)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        fun bind(item: MacroItem) {
            // 시간이 포함된 텍스트 표시
            item.timeOffset?.let { offset ->
                val time = Calendar.getInstance().apply {
                    add(Calendar.MILLISECOND, offset.toInt())
                }.time
                tvContent.text = item.content
                tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(time)
                tvTime.visibility = View.VISIBLE
            } ?: run {
                tvContent.text = item.content
                tvTime.visibility = View.GONE
            }
            btnDelete.setOnClickListener {
                onDeleteClick(item)  // 삭제할 아이템 전달
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MacroItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_macro_todo, parent, false)
        return MacroItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MacroItemViewHolder, position: Int) {
        holder.bind(items[position])
    }


    override fun getItemCount() = items.size

    fun submitList(newItems: List<MacroItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    fun removeItem(item: MacroItem) {
        val position = items.indexOf(item)
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

}