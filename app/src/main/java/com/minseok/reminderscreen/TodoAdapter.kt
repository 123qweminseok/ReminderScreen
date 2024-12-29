package com.minseok.reminderscreen

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private val onItemClick: (TodoItem) -> Unit,
    private val onItemLongClick: (TodoItem) -> Unit,
    private val onItemCheckChanged: (TodoItem) -> Unit = {}
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
        private val checkBox: CheckBox = view.findViewById(R.id.cbCompleted)
        private val checkMark: ImageView = view.findViewById(R.id.checkMark)
        fun bind(item: TodoItem) {
            tvContent.text = item.content
            tvTime.text = item.time?.let { formatTime(it) } ?: ""

            checkBox.isChecked = item.isCompleted
            updateItemAppearance(item)

            checkBox.setOnClickListener {
                val updatedItem = item.copy().apply {
                    isCompleted = checkBox.isChecked
                    createdAt = if (isCompleted) 0 else System.currentTimeMillis()
                }
                onItemCheckChanged(updatedItem)
                updateItemAppearance(updatedItem)
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }

            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }



        private fun updateItemAppearance(item: TodoItem) {
            if (item.isCompleted) {
                // 텍스트 스타일 변경
                tvContent.apply {
                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                }

                // 체크마크 애니메이션과 함께 표시
                checkMark.apply {
                    visibility = View.VISIBLE
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
            } else {
                // 미완료 상태로 복원
                tvContent.apply {
                    paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    setTextColor(ContextCompat.getColor(context, android.R.color.black))
                }
                checkMark.visibility = View.INVISIBLE
            }
        }

        private fun formatTime(date: Date): String {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem.content == newItem.content &&
                    oldItem.isCompleted == newItem.isCompleted &&
                    oldItem.time == newItem.time &&
                    oldItem.date == newItem.date
        }
    }
}