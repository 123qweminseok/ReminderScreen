package com.minseok.reminderscreen

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private val onItemClick: (TodoItem) -> Unit,
    private val onItemLongClick: (TodoItem) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvContent: TextView = view.findViewById(R.id.tvContent)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(item: TodoItem) {
            tvContent.text = item.content
            tvTime.text = item.time?.let { formatTime(it) } ?: ""
            updateItemAppearance(item)

            itemView.setOnClickListener {
                onItemClick(item)
                updateItemAppearance(item)
            }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        private fun updateItemAppearance(item: TodoItem) {
            if (item.isCompleted) {
                tvContent.paintFlags = tvContent.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemView.background = ContextCompat.getDrawable(itemView.context, R.drawable.rounded_background_completed)
            } else {
                tvContent.paintFlags = tvContent.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemView.background = ContextCompat.getDrawable(itemView.context, R.drawable.rounded_background)
            }
        }


        private fun formatTime(date: Date): String {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
    }
}

class TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
    override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
        return oldItem == newItem
    }
}