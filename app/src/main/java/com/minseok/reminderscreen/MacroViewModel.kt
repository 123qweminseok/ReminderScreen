// MacroViewModel.kt
package com.minseok.reminderscreen.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.minseok.reminderscreen.TodoItem
import com.minseok.reminderscreen.TodoViewModel
import com.minseok.reminderscreen.data.database.MacroDatabase
import com.minseok.reminderscreen.data.model.MacroTemplate
import com.minseok.reminderscreen.data.model.MacroItem
import com.minseok.reminderscreen.repository.MacroRepository
import kotlinx.coroutines.launch
import java.util.*

class MacroViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MacroRepository
    private val todoViewModel: TodoViewModel
    private val _macroTemplates = MutableLiveData<List<MacroTemplate>>()
    val macroTemplates: LiveData<List<MacroTemplate>> = _macroTemplates

    init {
        val macroDao = MacroDatabase.getInstance(application).macroDao()
        repository = MacroRepository(macroDao)
        todoViewModel = TodoViewModel.getInstance(application)
        loadMacroTemplates()
    }

    private fun loadMacroTemplates() {
        viewModelScope.launch {
            _macroTemplates.value = repository.getAllMacroTemplates()
        }
    }

    fun executeMacro(template: MacroTemplate) {
        viewModelScope.launch {
            val items = repository.getMacroItems(template.id)
            val currentDate = Calendar.getInstance()

            items.forEach { macroItem ->
                val todoItem = TodoItem(
                    content = macroItem.content,
                    date = currentDate.time,
                    time = macroItem.timeOffset?.let { offset ->
                        currentDate.apply {
                            add(Calendar.MILLISECOND, offset.toInt())
                        }.time
                    }
                )
                todoViewModel.addTodoItem(todoItem)
            }
        }
    }
    fun deleteMacroTemplate(template: MacroTemplate) {
        viewModelScope.launch {
            repository.deleteMacroTemplate(template)
            loadMacroTemplates()  // 목록 갱신
        }
    }
    fun saveMacroTemplate(name: String, description: String, items: List<String>, timeOffsets: List<MacroItem>) {
        viewModelScope.launch {
            val template = MacroTemplate(name = name, description = description)
            val macroItems = items.mapIndexed { index, content ->
                MacroItem(
                    content = content,
                    macroTemplateId = 0,
                    timeOffset = timeOffsets.getOrNull(index)?.timeOffset
                )
            }
            repository.saveMacroTemplate(template, macroItems)
            loadMacroTemplates()
        }
    }
    fun getMacroItems(templateId: Long, callback: (List<MacroItem>) -> Unit) {
        viewModelScope.launch {
            val items = repository.getMacroItems(templateId)
            callback(items)
        }
    }



}