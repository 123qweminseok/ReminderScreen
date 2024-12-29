// MacroRepository.kt
package com.minseok.reminderscreen.repository

import com.minseok.reminderscreen.data.dao.MacroDao
import com.minseok.reminderscreen.data.model.MacroTemplate
import com.minseok.reminderscreen.data.model.MacroItem

class MacroRepository(private val macroDao: MacroDao) {
    suspend fun getAllMacroTemplates() = macroDao.getAllMacroTemplates()

    suspend fun getMacroItems(templateId: Long) = macroDao.getMacroItems(templateId)

    suspend fun saveMacroTemplate(template: MacroTemplate, items: List<MacroItem>) {
        val templateId = macroDao.insertMacroTemplate(template)
        val macroItems = items.map { it.copy(macroTemplateId = templateId) }
        macroDao.insertMacroItems(macroItems)
    }

    suspend fun deleteMacroTemplate(template: MacroTemplate) {
        macroDao.deleteMacroTemplate(template)
    }
}