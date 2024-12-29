// MacroItem.kt
package com.minseok.reminderscreen.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "macro_items",
    foreignKeys = [
        ForeignKey(
            entity = MacroTemplate::class,
            parentColumns = ["id"],
            childColumns = ["macroTemplateId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MacroItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val macroTemplateId: Long,
    val content: String,
    val timeOffset: Long? = null
)