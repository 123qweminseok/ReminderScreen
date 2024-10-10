package com.minseok.reminderscreen

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var content: String,
    var isCompleted: Boolean = false,
    var date: Date,
    var time: Date? = null
)