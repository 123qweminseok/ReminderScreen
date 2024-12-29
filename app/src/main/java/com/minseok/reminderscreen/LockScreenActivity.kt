package com.minseok.reminderscreen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import android.graphics.Color

class LockScreenActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private lateinit var currentDate: Calendar
    private lateinit var tvTodoCount: TextView
//    private lateinit var tvMotivationalQuote: TextView
    private lateinit var rvTodoList: RecyclerView
    private var currentTodoCountIndex = 0
    private var currentMotivationalQuoteIndex = 1
    private var filteredItemCount = 0
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var appSettingsPreferences: SharedPreferences
    private lateinit var gestureDetector: GestureDetectorCompat
    private var initialY: Float = 0f
    private lateinit var slideToUnlockView: ConstraintLayout
    private val MIN_SLIDE_DISTANCE = 100

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
        slideToUnlockView = findViewById<ConstraintLayout>(R.id.slideToUnlock)

        viewModel = TodoViewModel.getInstance(application)
        currentDate = Calendar.getInstance()
        sharedPreferences = getSharedPreferences("CustomQuotes", Context.MODE_PRIVATE)
        appSettingsPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        gestureDetector = GestureDetectorCompat(this, this)

        tvTodoCount = findViewById(R.id.tvTodoCount)
//        tvMotivationalQuote = findViewById(R.id.tvMotivationalQuote)
        rvTodoList = findViewById(R.id.rvTodoList)

        setupRecyclerView()
        setupObservers()
        setupUI()
        updateDateDisplay()
        loadTodoItems()
        setupQuoteClickListeners()
        loadCustomQuotes()
        applyBackgroundColor()
        updateTodoListVisibility()
        setupSlideToUnlock()  // 여기에 추가



    }




    private fun setupSlideToUnlock() {
        var initialX = 0f
        val MIN_SWIPE_DISTANCE = 200
        var isDragging = false
        val unlockIcon = findViewById<ImageView>(R.id.unlockIcon)
        val slideView = findViewById<ConstraintLayout>(R.id.slideToUnlock)

        slideView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    isDragging = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        val deltaX = event.rawX - initialX
                        if (deltaX > 0) {
                            view.translationX = deltaX
                            // 아이콘 회전 애니메이션
                            unlockIcon.rotation = (deltaX / MIN_SWIPE_DISTANCE) * 360f
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        val deltaX = event.rawX - initialX
                        isDragging = false

                        if (deltaX > MIN_SWIPE_DISTANCE) {
                            // 성공적으로 슬라이드했을 때 애니메이션
                            view.animate()
                                .translationX(view.width.toFloat())
                                .setDuration(200)
                                .withEndAction {
                                    finishAndRemoveTask()
                                }
                                .start()
                        } else {
                            // 원위치로 돌아가는 애니메이션
                            view.animate()
                                .translationX(0f)
                                .setDuration(200)
                                .start()
                            unlockIcon.animate()
                                .rotation(0f)
                                .setDuration(200)
                                .start()
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }







    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.lock_screen_menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_menu) {
            val isChecked = appSettingsPreferences.getBoolean("show_todo_list", true)
            item.isChecked = isChecked
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateTodoListVisibility() {
        val showTodoList = appSettingsPreferences.getBoolean("show_todo_list", true)
        rvTodoList.visibility = if (showTodoList) View.VISIBLE else View.GONE
    }

    private fun applyBackgroundColor() {
        val backgroundColor = appSettingsPreferences.getInt("backgroundColor", 0)
        if (backgroundColor != 0) {
            findViewById<View>(R.id.rootLayout).setBackgroundColor(backgroundColor)
        }
    }

    private fun setupQuoteClickListeners() {
        tvTodoCount.setOnClickListener {
            showQuoteSelectionDialog(true)
        }

//        tvMotivationalQuote.setOnClickListener {
//            showQuoteSelectionDialog(false)
//        }

        tvTodoCount.setOnLongClickListener {
            showEditQuoteDialog(true)
            true
        }

//        tvMotivationalQuote.setOnLongClickListener {
//            showEditQuoteDialog(false)
//            true
//        }
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
        rvTodoList.apply {
            layoutManager = LinearLayoutManager(this@LockScreenActivity)
            adapter = this@LockScreenActivity.adapter
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val todoItem = adapter.currentList[position].copy(isCompleted = true)
                viewModel.updateTodoItem(todoItem)
                adapter.notifyItemChanged(position)
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
                if (!isCurrentlyActive) {
                    super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, false)
                    return
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        itemTouchHelper.attachToRecyclerView(rvTodoList)
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
            adapter.notifyDataSetChanged() // 갱신 추가
            updateQuotes()
            rvTodoList.visibility = View.VISIBLE // 명시적 가시성 설정
        }
    }
    private fun updateQuotes() {
        tvTodoCount.text = String.format(quotes[currentTodoCountIndex], filteredItemCount)
//        tvMotivationalQuote.text = String.format(quotes[currentMotivationalQuoteIndex], filteredItemCount)
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

        findViewById<ConstraintLayout>(R.id.slideToUnlock).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }
    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("M월 d일(E)", Locale.KOREAN)
        findViewById<TextView>(R.id.tvDate).text = dateFormat.format(currentDate.time)
    }

    private fun loadTodoItems() {
        viewModel.loadTodoItems(currentDate.time)
        updateTodoListVisibility() // 가시성 업데이트
        adapter.notifyDataSetChanged() // 리사이클러뷰 갱신 추가

    }








    private fun showAddTodoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_todo, null).also { view ->
            val etContent = view.findViewById<EditText>(R.id.etContent)
            val btnSetDate = view.findViewById<Button>(R.id.btnSetDate)
            val btnSetTime = view.findViewById<Button>(R.id.btnSetTime)

            // EditText 스타일링
            etContent.apply {
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F5F9FF"))
                    cornerRadius = 16f
                    setStroke(2, Color.parseColor("#E8F1FF"))
                }
                setPadding(40, 30, 40, 30)
                setTextColor(Color.BLACK)
                setHintTextColor(Color.GRAY)
                hint = "할 일을 입력하세요"
                textSize = 16f
            }

            // 날짜/시간 버튼 공통 스타일
            val buttonBackground = GradientDrawable().apply {
                setColor(Color.parseColor("#EDF5FF"))
                cornerRadius = 12f
                setStroke(2, Color.parseColor("#2196F3"))
            }

            var selectedDate = currentDate.clone() as Calendar
            var selectedTime: Date? = null

            // 날짜 선택 버튼 스타일링
            btnSetDate.apply {
                background = buttonBackground.constantState?.newDrawable()
                setPadding(40, 20, 40, 20)
                setTextColor(Color.parseColor("#2196F3"))
                textSize = 15f
                text = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                    .format(selectedDate.time)
                elevation = 4f
            }

            // 시간 선택 버튼 스타일링
            btnSetTime.apply {
                background = buttonBackground.constantState?.newDrawable()
                setPadding(40, 20, 40, 20)
                setTextColor(Color.parseColor("#2196F3"))
                textSize = 15f
                text = "시간 선택"
                elevation = 4f
            }

            btnSetDate.setOnClickListener {
                DatePickerDialog(
                    this,
                    R.style.CustomDatePickerDialog,
                    { _, year, month, dayOfMonth ->
                        selectedDate.set(year, month, dayOfMonth)
                        btnSetDate.text = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                            .format(selectedDate.time)
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            btnSetTime.setOnClickListener {
                TimePickerDialog(
                    this,
                    R.style.CustomTimePickerDialog,
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

            val dialog = AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                .setTitle("할 일 추가")
                .setPositiveButton("확인", null)  // 여기서는 null로 설정하고 나중에 재정의
                .setNegativeButton("취소", null)  // 여기서는 null로 설정하고 나중에 재정의
                .setView(view)
                .create()

            // 다이얼로그 배경 설정
            dialog.window?.apply {
                setBackgroundDrawable(GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 24f
                })
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.9).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            dialog.setOnShowListener {
                // 확인 버튼 스타일링
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    text = "확인"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#4CAF50"))  // 초록색
                        cornerRadius = 12f
                    }
                    setPadding(50, 25, 50, 25)
                    isAllCaps = false

                    setOnClickListener {
                        val content = etContent.text.toString()
                        if (content.isNotEmpty()) {
                            val todoItem = TodoItem(
                                content = content,
                                date = selectedDate.time,
                                time = selectedTime
                            )
                            viewModel.addTodoItem(todoItem)

                            if (selectedTime != null) {
                                NotificationHelper(this@LockScreenActivity)
                                    .scheduleNotification(todoItem)
                            }
                            dialog.dismiss()
                            loadTodoItems()
                        } else {
                            etContent.error = "내용을 입력해주세요"
                        }
                    }
                }




                // 취소 버튼 스타일링
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    text = "취소"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#FF5252"))
                        cornerRadius = 12f
                    }
                    setPadding(50, 25, 50, 25)
                    isAllCaps = false

                    setOnClickListener { dialog.dismiss() }
                }
            }

            dialog.show()

            // 다이얼로그 제목 스타일링
            dialog.findViewById<TextView>(android.R.id.title)?.apply {
                textSize = 20f
                setTextColor(Color.parseColor("#2196F3"))
                setPadding(40, 30, 40, 30)
            }
        }
    }




    private fun onTodoItemClick(todoItem: TodoItem) {
        todoItem.isCompleted = !todoItem.isCompleted
        if (!todoItem.isCompleted) {
            todoItem.createdAt = System.currentTimeMillis()
        } else {
            todoItem.createdAt = 0
        }
        viewModel.updateTodoItem(todoItem)
        loadTodoItems()

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
                loadTodoItems()
            }
            .setNegativeButton("취소", null)
            .show()
    }












    override fun onDown(e: MotionEvent): Boolean = true
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
    override fun onLongPress(e: MotionEvent) {}
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null || e2 == null) return false
        val diffY = e2.y - e1.y
        if (abs(diffY) > 100 && abs(velocityY) > 100) {
            if (diffY > 0) {
                finishAndRemoveTask()
                return true
            }
        }
        return false
    }
}