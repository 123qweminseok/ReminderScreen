// MacroTemplate.kt
package com.minseok.reminderscreen.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macro_templates")
data class MacroTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String
)