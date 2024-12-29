// MacroDao.kt
package com.minseok.reminderscreen.data.dao

import androidx.room.*
import com.minseok.reminderscreen.data.model.MacroTemplate
import com.minseok.reminderscreen.data.model.MacroItem

@Dao
interface MacroDao {
    @Query("SELECT * FROM macro_templates")
    suspend fun getAllMacroTemplates(): List<MacroTemplate>

    @Query("SELECT * FROM macro_items WHERE macroTemplateId = :templateId")
    suspend fun getMacroItems(templateId: Long): List<MacroItem>

    @Insert
    suspend fun insertMacroTemplate(template: MacroTemplate): Long

    @Insert
    suspend fun insertMacroItems(items: List<MacroItem>)

    @Update
    suspend fun updateMacroTemplate(template: MacroTemplate)

    @Delete
    suspend fun deleteMacroTemplate(template: MacroTemplate)

    @Delete
    suspend fun deleteMacroItem(item: MacroItem)
}