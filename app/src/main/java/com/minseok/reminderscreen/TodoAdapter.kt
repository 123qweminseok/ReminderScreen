package com.minseok.reminderscreen

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

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

        fun bind(item: TodoItem) {
            tvContent.text = item.content
            if (item.isCompleted) {
                tvContent.paintFlags = tvContent.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tvContent.paintFlags = tvContent.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
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