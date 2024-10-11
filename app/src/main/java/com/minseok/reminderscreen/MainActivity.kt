package com.minseok.reminderscreen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.work.Constraints
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener


class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private var currentDate = Calendar.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(TodoViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupUI()
        startService(Intent(this, LockScreenService::class.java))
        setupWorkManager()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)

    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_background -> {
                showColorPickerDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun showColorPickerDialog() {
        val colorPickerDialog = ColorPickerDialog.Builder(this)
            .setTitle("배경색 선택")
            .setPositiveButton("선택", ColorEnvelopeListener { envelope, _ ->
                val color = envelope.color
                saveBackgroundColor(color)
            })
            .setNegativeButton("취소") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .setBottomSpace(12)

        colorPickerDialog.show()
    }

    private fun saveBackgroundColor(color: Int) {
        sharedPreferences.edit().putInt("backgroundColor", color).apply()
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(::onTodoItemClick, ::onTodoItemLongClick)
        val recyclerView = findViewById<RecyclerView>(R.id.rvTodoList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val todoItem = adapter.currentList[position]
                todoItem.isCompleted = true
                viewModel.updateTodoItem(todoItem)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.GREEN)
                background.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
                )
                background.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }











    private fun setupObservers() {
        viewModel.todoItems.observe(this) { items ->
            val filteredItems = items.filter { isSameDay(it.date, currentDate.time) }
            adapter.submitList(filteredItems)
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
    private fun setupWorkManager() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<LockScreenWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "LockScreenWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }


}