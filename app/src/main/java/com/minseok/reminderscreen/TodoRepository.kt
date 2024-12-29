package com.minseok.reminderscreen

import android.content.Context
import java.util.Date

class TodoRepository(context: Context) {
    private val todoDao: TodoDao = TodoDatabase.getInstance(context).todoDao()

    suspend fun getAllTodoItems(): List<TodoItem> {
        return todoDao.getAllTodoItems()
    }
    suspend fun getTodoItemsByDate(date: Date): List<TodoItem> {
        return todoDao.getTodoItemsByDate(date)
    }

    suspend fun insertTodoItem(todoItem: TodoItem) {
        todoDao.insertTodoItem(todoItem)
    }

    suspend fun updateTodoItem(todoItem: TodoItem) {
        todoDao.updateTodoItem(todoItem)
    }

    suspend fun deleteTodoItem(todoItem: TodoItem) {
        todoDao.deleteTodoItem(todoItem)
    }

    suspend fun getTodoItemById(id: Long): TodoItem? {  // Long 타입으로 변경
        return todoDao.getTodoItemById(id)
    }

}