package com.minseok.reminderscreen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class TodoViewModel private constructor(application: Application) : AndroidViewModel(application) {
    private val repository: TodoRepository = TodoRepository(application)
    private val _todoItems = MutableLiveData<List<TodoItem>>()
    val todoItems: LiveData<List<TodoItem>> = _todoItems
    private val notificationManager = LockScreenNotificationManager(application)

    companion object {
        @Volatile
        private var instance: TodoViewModel? = null

        fun getInstance(application: Application): TodoViewModel {
            return instance ?: synchronized(this) {
                instance ?: TodoViewModel(application).also { instance = it }
            }
        }
    }

    init {
        loadAllTodoItems()
    }

    fun getTodoItemsByDate(date: Date) = runBlocking {
        val items = repository.getTodoItemsByDate(date)
        sortTodoItems(items)  // 정렬된 결과 반환
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

    fun getTodoItemsForDate(date: LocalDate): List<TodoItem> {
        val javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        return getTodoItemsByDate(javaDate)
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
