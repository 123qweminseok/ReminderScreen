package com.minseok.reminderscreen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class LockScreenActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private lateinit var currentDate: Calendar
    private lateinit var tvTodoCount: TextView
    private lateinit var tvMotivationalQuote: TextView
    private var currentTodoCountIndex = 0
    private var currentMotivationalQuoteIndex = 1
    private var filteredItemCount = 0
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var gestureDetector: GestureDetectorCompat

    private val quotes = mutableListOf(
        "오늘 일정 %d개",
        "오늘 총 연료 갯수: %d개",
        "오늘 총 작업 갯수: %d개",
        "나를 발전하는 행동: %d개",
        "나를 강화하는 시간: %d개",
        "오늘의 목표: %d개 달성하기",
        "작은 진전이 모여 큰 변화를 만듭니다: %d개의 작은 진전",
        "하루에 %d개의 도전, 당신은 할 수 있습니다!"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_lock_screen)

        viewModel = ViewModelProvider(this).get(TodoViewModel::class.java)
        currentDate = Calendar.getInstance()
        sharedPreferences = getSharedPreferences("CustomQuotes", Context.MODE_PRIVATE)
        gestureDetector = GestureDetectorCompat(this, this)

        tvTodoCount = findViewById(R.id.tvTodoCount)
        tvMotivationalQuote = findViewById(R.id.tvMotivationalQuote)

        setupRecyclerView()
        setupObservers()
        setupUI()
        updateDateDisplay()
        loadTodoItems()
        setupQuoteClickListeners()
        loadCustomQuotes()
        applyBackgroundColor()

    }
    private fun applyBackgroundColor() {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val backgroundColor = sharedPreferences.getInt("backgroundColor", 0)
        if (backgroundColor != 0) {
            findViewById<View>(android.R.id.content).setBackgroundColor(backgroundColor)
        }
    }

    private fun setupQuoteClickListeners() {
        tvTodoCount.setOnClickListener {
            showQuoteSelectionDialog(true)
        }

        tvMotivationalQuote.setOnClickListener {
            showQuoteSelectionDialog(false)
        }

        tvTodoCount.setOnLongClickListener {
            showEditQuoteDialog(true)
            true
        }

        tvMotivationalQuote.setOnLongClickListener {
            showEditQuoteDialog(false)
            true
        }
    }

    private fun showEditQuoteDialog(isTodoCount: Boolean) {
        val index = if (isTodoCount) currentTodoCountIndex else currentMotivationalQuoteIndex
        val currentQuote = quotes[index]

        val input = EditText(this)
        input.setText(currentQuote)

        AlertDialog.Builder(this)
            .setTitle("명언 수정")
            .setView(input)
            .setPositiveButton("저장") { _, _ ->
                val newQuote = input.text.toString()
                if (newQuote.isNotEmpty()) {
                    quotes[index] = newQuote
                    updateQuotes()
                    saveCustomQuote(index, newQuote)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun saveCustomQuote(index: Int, quote: String) {
        sharedPreferences.edit().putString("quote_$index", quote).apply()
    }

    private fun loadCustomQuotes() {
        for (i in quotes.indices) {
            val savedQuote = sharedPreferences.getString("quote_$i", null)
            if (savedQuote != null) {
                quotes[i] = savedQuote
            }
        }
        updateQuotes()
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(::onTodoItemClick, ::onTodoItemLongClick)
        findViewById<RecyclerView>(R.id.rvTodoList).apply {
            layoutManager = LinearLayoutManager(this@LockScreenActivity)
            this.adapter = this@LockScreenActivity.adapter
        }

        val itemTouchHelper =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val todoItem = adapter.currentList[position]
                    viewModel.deleteTodoItem(todoItem)
                }
            })

        itemTouchHelper.attachToRecyclerView(findViewById<RecyclerView>(R.id.rvTodoList))
    }

    private fun showQuoteSelectionDialog(isTodoCount: Boolean) {
        val items = quotes.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("표시할 문구 선택")
            .setSingleChoiceItems(
                items,
                if (isTodoCount) currentTodoCountIndex else currentMotivationalQuoteIndex
            ) { dialog, which ->
                if (isTodoCount) {
                    currentTodoCountIndex = which
                } else {
                    currentMotivationalQuoteIndex = which
                }
                updateQuotes()
                dialog.dismiss()
            }
            .show()
    }

    private fun setupObservers() {
        viewModel.todoItems.observe(this) { items ->
            val filteredItems = items.filter { isSameDay(it.date, currentDate.time) }
            filteredItemCount = filteredItems.size
            adapter.submitList(filteredItems)
            updateQuotes()
        }
    }

    private fun updateQuotes() {
        tvTodoCount.text = String.format(quotes[currentTodoCountIndex], filteredItemCount)
        tvMotivationalQuote.text = String.format(quotes[currentMotivationalQuoteIndex], filteredItemCount)
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun setupUI() {
        findViewById<Button>(R.id.btnAddTodo).setOnClickListener {
            showAddTodoDialog()
        }

        findViewById<Button>(R.id.btnPrevDate).setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            updateDateDisplay()
            loadTodoItems()
        }

        findViewById<Button>(R.id.btnNextDate).setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, 1)
            updateDateDisplay()
            loadTodoItems()
        }
        findViewById<Button>(R.id.btnToMain).setOnClickListener {
            finishAndRemoveTask()
        }
        findViewById<TextView>(R.id.slideToUnlock).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("M월 d일(E)", Locale.KOREAN)
        findViewById<TextView>(R.id.tvDate).text = dateFormat.format(currentDate.time)
    }

    private fun loadTodoItems() {
        viewModel.loadTodoItems(currentDate.time)
    }

    private fun showAddTodoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_todo, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val btnSetDate = dialogView.findViewById<Button>(R.id.btnSetDate)
        val btnSetTime = dialogView.findViewById<Button>(R.id.btnSetTime)

        var selectedDate = currentDate.clone() as Calendar
        var selectedTime: Date? = null

        AlertDialog.Builder(this)
            .setTitle("할 일 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val content = etContent.text.toString()
                if (content.isNotEmpty()) {
                    val todoItem = TodoItem(content = content, date = selectedDate.time, time = selectedTime)
                    viewModel.addTodoItem(todoItem)

                    // 알림 설정 추가
                    if (selectedTime != null) {
                        NotificationHelper(this).scheduleNotification(todoItem)
                    }

                    loadTodoItems()
                }
            }
            .setNegativeButton("취소", null)
            .show()



        btnSetDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    btnSetDate.text = "${year}-${month + 1}-${dayOfMonth}"
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSetTime.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    selectedDate.set(Calendar.MINUTE, minute)
                    selectedTime = selectedDate.time
                    btnSetTime.text = String.format("%02d:%02d", hourOfDay, minute)
                },
                selectedDate.get(Calendar.HOUR_OF_DAY),
                selectedDate.get(Calendar.MINUTE),
                false
            ).show()
        }

        AlertDialog.Builder(this)
            .setTitle("할 일 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val content = etContent.text.toString()
                if (content.isNotEmpty()) {
                    val todoItem = TodoItem(content = content, date = selectedDate.time, time = selectedTime)
                    viewModel.addTodoItem(todoItem)
                    loadTodoItems()  // 할 일 추가 후 목록을 다시 로드
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun onTodoItemClick(todoItem: TodoItem) {
        todoItem.isCompleted = !todoItem.isCompleted
        if (!todoItem.isCompleted) {
            // 미완료 상태로 변경 시 생성 시간을 현재 시간으로 갱신하여 최상단으로 이동
            todoItem.createdAt = System.currentTimeMillis()
        } else {
            // 완료 상태로 변경 시 생성 시간을 0으로 설정하여 최하단으로 이동
            todoItem.createdAt = 0
        }
        viewModel.updateTodoItem(todoItem)
        loadTodoItems()  // 할 일 업데이트 후 목록을 다시 로드

        // 변경된 항목을 즉시 어댑터에 반영
        val currentList = adapter.currentList.toMutableList()
        val index = currentList.indexOfFirst { it.id == todoItem.id }
        if (index != -1) {
            currentList[index] = todoItem
            adapter.submitList(currentList)
        }
    }

    private fun onTodoItemLongClick(todoItem: TodoItem) {
        AlertDialog.Builder(this)
            .setTitle("할 일 삭제")
            .setMessage("이 할 일을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteTodoItem(todoItem)
                loadTodoItems()  // 할 일 삭제 후 목록을 다시 로드
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // GestureDetector.OnGestureListener 메서드 구현
    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
    override fun onLongPress(e: MotionEvent) {}
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null || e2 == null) return false
        val diffY = e2.y - e1.y
        if (abs(diffY) > 100 && abs(velocityY) > 100) {
            if (diffY > 0) {
                // 아래로 스와이프
                finishAndRemoveTask()
                return true
            }
        }
        return false
    }



}