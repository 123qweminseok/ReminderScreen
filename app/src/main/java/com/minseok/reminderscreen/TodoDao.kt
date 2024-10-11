package com.minseok.reminderscreen

import androidx.room.*
import java.util.Date

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items")
    suspend fun getAllTodoItems(): List<TodoItem>

    @Query("SELECT * FROM todo_items WHERE date(date / 1000, 'unixepoch') = date(:date / 1000, 'unixepoch')")
    suspend fun getTodoItemsByDate(date: Date): List<TodoItem>

    @Insert
    suspend fun insertTodoItem(todoItem: TodoItem)

    @Update
    suspend fun updateTodoItem(todoItem: TodoItem)

    @Delete
    suspend fun deleteTodoItem(todoItem: TodoItem)
}