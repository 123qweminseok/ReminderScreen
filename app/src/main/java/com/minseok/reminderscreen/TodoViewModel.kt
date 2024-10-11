package com.minseok.reminderscreen

import android.app.Application
import android.util.Log
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
        loadAllTodoItems()
    }

    private fun loadAllTodoItems() {
        viewModelScope.launch {
            try {
                val items = repository.getAllTodoItems()
                _todoItems.value = sortTodoItems(items)
                updateNotification(items)
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error loading all todo items", e)
            }
        }
    }

    fun loadTodoItems(date: Date) {
        viewModelScope.launch {
            try {
                val items = repository.getTodoItemsByDate(date)
                _todoItems.value = sortTodoItems(items)
                updateNotification(items)
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error loading todo items for date: $date", e)
            }
        }
    }

    fun addTodoItem(todoItem: TodoItem) {
        viewModelScope.launch {
            try {
                repository.insertTodoItem(todoItem)
                loadAllTodoItems()
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error adding todo item", e)
            }
        }
    }

    fun updateTodoItem(todoItem: TodoItem) {
        viewModelScope.launch {
            try {
                repository.updateTodoItem(todoItem)
                loadAllTodoItems()
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error updating todo item", e)
            }
        }
    }

    fun deleteTodoItem(todoItem: TodoItem) {
        viewModelScope.launch {
            try {
                repository.deleteTodoItem(todoItem)
                loadAllTodoItems()
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Error deleting todo item", e)
            }
        }
    }

    private fun updateNotification(items: List<TodoItem>) {
        val incompleteItems = items.filter { !it.isCompleted }
        notificationManager.showTodoNotification(incompleteItems)
    }

    private fun sortTodoItems(items: List<TodoItem>): List<TodoItem> {
        return items.sortedWith(
            compareBy<TodoItem> { it.isCompleted }
                .thenByDescending { it.createdAt }
        )

    }
}