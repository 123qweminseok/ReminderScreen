package com.minseok.reminderscreen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private var currentDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(TodoViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupUI()
        startService(Intent(this, LockScreenService::class.java))
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(::onTodoItemClick, ::onTodoItemLongClick)
        findViewById<RecyclerView>(R.id.rvTodoList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupObservers() {
        viewModel.todoItems.observe(this) { items ->
            adapter.submitList(items.filter { isSameDay(it.date, currentDate.time) })
        }
    }

    private fun setupUI() {
        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            showAddTodoDialog()
        }

        updateDateDisplay()

        findViewById<ImageButton>(R.id.btnPrevDate).setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            updateDateDisplay()
            refreshTodoList()
        }

        findViewById<ImageButton>(R.id.btnNextDate).setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            updateDateDisplay()
            refreshTodoList()
        }
    }

    private fun updateDateDisplay() {
        findViewById<TextView>(R.id.tvCurrentDate).text = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault()).format(currentDate.time)
    }

    private fun refreshTodoList() {
        viewModel.loadTodoItems(currentDate.time)
    }

    private fun showAddTodoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_todo, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val btnSetDate = dialogView.findViewById<Button>(R.id.btnSetDate)
        val btnSetTime = dialogView.findViewById<Button>(R.id.btnSetTime)

        var selectedDate: Date = currentDate.time
        var selectedTime: Date? = null

        btnSetDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                currentDate.set(year, month, dayOfMonth)
                selectedDate = currentDate.time
                btnSetDate.text = "${year}-${month+1}-${dayOfMonth}"
            }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSetTime.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                currentDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                currentDate.set(Calendar.MINUTE, minute)
                selectedTime = currentDate.time
                btnSetTime.text = String.format("%02d:%02d", hourOfDay, minute)
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(this)
            .setTitle("할 일 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val content = etContent.text.toString()
                if (content.isNotEmpty()) {
                    val todoItem = TodoItem(content = content, date = selectedDate, time = selectedTime)
                    viewModel.addTodoItem(todoItem)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun onTodoItemClick(todoItem: TodoItem) {
        showEditTodoDialog(todoItem)
    }

    private fun onTodoItemLongClick(todoItem: TodoItem) {
        AlertDialog.Builder(this)
            .setTitle("할 일 삭제")
            .setMessage("이 할 일을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteTodoItem(todoItem)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditTodoDialog(todoItem: TodoItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_todo, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val btnSetDate = dialogView.findViewById<Button>(R.id.btnSetDate)
        val btnSetTime = dialogView.findViewById<Button>(R.id.btnSetTime)

        etContent.setText(todoItem.content)
        btnSetDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(todoItem.date)
        btnSetTime.text = todoItem.time?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "시간 미설정"

        var selectedDate: Date = todoItem.date
        var selectedTime: Date? = todoItem.time

        btnSetDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                btnSetDate.text = "${year}-${month+1}-${dayOfMonth}"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnSetTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedTime ?: Date()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                selectedTime = calendar.time
                btnSetTime.text = String.format("%02d:%02d", hourOfDay, minute)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(this)
            .setTitle("할 일 수정")
            .setView(dialogView)
            .setPositiveButton("수정") { _, _ ->
                val content = etContent.text.toString()
                if (content.isNotEmpty()) {
                    todoItem.content = content
                    todoItem.date = selectedDate
                    todoItem.time = selectedTime
                    viewModel.updateTodoItem(todoItem)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
}