// AddMacroDialog.kt
package com.minseok.reminderscreen.ui.dialog

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.minseok.reminderscreen.R
import com.minseok.reminderscreen.viewmodel.MacroViewModel
import com.google.android.material.button.MaterialButton
import com.minseok.reminderscreen.data.model.MacroItem
import com.minseok.reminderscreen.ui.macro.MacroItemAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddMacroDialog : DialogFragment() {
    private lateinit var viewModel: MacroViewModel
    private val todoItems = mutableListOf<String>()
    private lateinit var adapter: MacroItemAdapter
    private val macroItems = mutableListOf<MacroItem>()  // 추가

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_macro, null)

        viewModel = ViewModelProvider(requireActivity())[MacroViewModel::class.java]

        val etName = view.findViewById<EditText>(R.id.etMacroName)
        val etDescription = view.findViewById<EditText>(R.id.etMacroDescription)
        val rvItems = view.findViewById<RecyclerView>(R.id.rvMacroItems)
        val btnAddItem = view.findViewById<MaterialButton>(R.id.btnAddItem)

        if (viewModel == null) {
            // 에러 처리
            return super.onCreateDialog(savedInstanceState)
        }

        setupRecyclerView(rvItems)
        setupAddItemButton(btnAddItem)

        return AlertDialog.Builder(requireContext())
            .setTitle("매크로 추가")
            .setView(view)
            .setPositiveButton("저장") { _, _ ->
                val name = etName.text.toString()
                val description = etDescription.text.toString()
                viewModel.saveMacroTemplate(name, description, todoItems, macroItems)  // macroItems 전달
            }
            .setNegativeButton("취소", null)
            .create()
}
    private fun setupRecyclerView(recyclerView: RecyclerView) {
        adapter = MacroItemAdapter(
            onItemClick = { /* 필요시 아이템 클릭 처리 */ },
            onDeleteClick = { itemToDelete ->
                // 아이템 삭제 처리
                macroItems.remove(itemToDelete)
                todoItems.remove(itemToDelete.content)
                adapter.removeItem(itemToDelete)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    private fun setupAddItemButton(button: MaterialButton) {
        button.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun showAddItemDialog() {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_macro_item, null)
        val etContent = view.findViewById<EditText>(R.id.etItemContent)
        val etTime = view.findViewById<EditText>(R.id.etItemTime)

        var selectedTimeOffset: Long? = null

        etTime.setOnClickListener {
            val currentTime = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    selectedTimeOffset = selectedCal.timeInMillis - currentTime.timeInMillis
                    etTime.setText(String.format("%02d:%02d", hourOfDay, minute))
                },
                currentTime.get(Calendar.HOUR_OF_DAY),
                currentTime.get(Calendar.MINUTE),
                false
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("할 일 추가")
            .setView(view)
            .setPositiveButton("추가") { _, _ ->
                val content = etContent.text.toString()
                if (content.isNotEmpty()) {
                    // 새로운 아이템 생성
                    val newItem = MacroItem(
                        content = content,
                        macroTemplateId = 0,
                        timeOffset = selectedTimeOffset
                    )

                    // 리스트에 추가
                    macroItems.add(newItem)
                    todoItems.add(content)

                    // 어댑터 업데이트
                    adapter.submitList(macroItems)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }


}