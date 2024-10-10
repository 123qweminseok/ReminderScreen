package com.minseok.reminderscreen

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class TodoRepository(context: Context) {
    private val todoDao: TodoDao = TodoDatabase.getInstance(context).todoDao()

    suspend fun getTodoItemsByDate(date: Date): List<TodoItem> = withContext(Dispatchers.IO) {
        todoDao.getTodoItemsByDate(date)
    }

    suspend fun insertTodoItem(todoItem: TodoItem) = withContext(Dispatchers.IO) {
        todoDao.insert(todoItem)
    }

    suspend fun updateTodoItem(todoItem: TodoItem) = withContext(Dispatchers.IO) {
        todoDao.update(todoItem)
    }

    suspend fun deleteTodoItem(todoItem: TodoItem) = withContext(Dispatchers.IO) {
        todoDao.delete(todoItem)
    }
}