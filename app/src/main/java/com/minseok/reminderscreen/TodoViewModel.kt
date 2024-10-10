package com.minseok.reminderscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Date

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TodoRepository = TodoRepository(application)
    private val _todoItems = MutableLiveData<List<TodoItem>>()
    val todoItems: LiveData<List<TodoItem>> = _todoItems
    private val notificationManager = LockScreenNotificationManager(application)

    init {
        loadTodoItems(Date())
    }

    fun loadTodoItems(date: Date) {
        viewModelScope.launch {
            val items = repository.getTodoItemsByDate(date)
            _todoItems.value = items
            notificationManager.showTodoNotification(items)
        }
    }
    fun addTodoItem(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.insertTodoItem(todoItem)
            loadTodoItems(todoItem.date)
        }
    }

    fun updateTodoItem(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.updateTodoItem(todoItem)
            loadTodoItems(todoItem.date)
        }
    }

    fun deleteTodoItem(todoItem: TodoItem) {
        viewModelScope.launch {
            repository.deleteTodoItem(todoItem)
            loadTodoItems(todoItem.date)
        }
    }
}