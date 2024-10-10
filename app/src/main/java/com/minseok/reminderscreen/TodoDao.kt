package com.minseok.reminderscreen

import androidx.room.*
import java.util.Date

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items WHERE date = :date")
    suspend fun getTodoItemsByDate(date: Date): List<TodoItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todoItem: TodoItem)

    @Update
    suspend fun update(todoItem: TodoItem)

    @Delete
    suspend fun delete(todoItem: TodoItem)
}