package com.minseok.reminderscreen

import android.app.*
import androidx.core.app.NotificationManagerCompat
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.tv.material3.ButtonShape
import java.text.SimpleDateFormat
import java.util.*
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.minseok.reminderscreen.NotificationHelper  // 이게 있는지 확인
import com.minseok.reminderscreen.data.model.MacroTemplate
import com.minseok.reminderscreen.ui.dialog.AddMacroDialog
import com.minseok.reminderscreen.ui.macro.MacroAdapter
import com.minseok.reminderscreen.ui.macro.MacroItemAdapter
import com.minseok.reminderscreen.viewmodel.MacroViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private var currentDate = Calendar.getInstance()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var popupWindow: PopupWindow
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var macroViewModel: MacroViewModel
    private lateinit var macroAdapter: MacroAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        macroViewModel = ViewModelProvider(this).get(MacroViewModel::class.java)
        drawerLayout = findViewById(R.id.drawerLayout)

        window.statusBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR


        viewModel = TodoViewModel.getInstance(application)


        notificationHelper = NotificationHelper(this)  // 이 초기화가 있는지 확인

        setupRecyclerView()
        setupObservers()
        setupUI()
        startService(Intent(this, LockScreenService::class.java))
        setupWorkManager()
        checkPermissions()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = null // 기본 제목 제거

// TextView로 커스텀 제목 설정
        val titleTextView = TextView(this).apply {
            text = "MY PRIDE" // 원하는 제목 텍스트
            textSize = 16f
            setTextColor(Color.parseColor("#2196F3")) // 진한 하늘색
            setTypeface(typeface, Typeface.BOLD) // 굵게 설정
            gravity = Gravity.CENTER
            setShadowLayer(10f, 0f, 0f, Color.parseColor("#BBDEFB")) // 하늘색 빛 효과
        }









        viewModel = TodoViewModel.getInstance(application)
        notificationHelper = NotificationHelper(this)


        setupMacroDrawer()





// 애니메이션 추가
        val blinkAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.blinking_animation)
        titleTextView.startAnimation(blinkAnimation)

// 툴바에 제목 추가
        toolbar.addView(titleTextView, Toolbar.LayoutParams(
            Toolbar.LayoutParams.WRAP_CONTENT,
            Toolbar.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))




        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        if (sharedPreferences.getBoolean("show_todo_list", true)) {
            startService(Intent(this, LockScreenService::class.java))
        }

        setupBottomNavigation()
    }

    private fun setupMacroDrawer() {
        val macroRecyclerView = findViewById<RecyclerView>(R.id.rvMacros)
        macroAdapter = MacroAdapter(
            onMacroClick = { macro ->
                // 싱글 클릭: 매크로 내용 보기
                showMacroDetailsDialog(macro)
            },
            onMacroDoubleClick = { macro ->
                // 더블 클릭: 매크로 실행
                macroViewModel.executeMacro(macro)
                drawerLayout.closeDrawer(GravityCompat.END)
                Toast.makeText(this, "매크로가 실행되었습니다.", Toast.LENGTH_SHORT).show()
            },
            onMacroLongClick = { macro ->
                // 롱클릭: 삭제
                AlertDialog.Builder(this)
                    .setTitle("매크로 삭제")
                    .setMessage("이 매크로를 삭제하시겠습니까?")
                    .setPositiveButton("삭제") { _, _ ->
                        macroViewModel.deleteMacroTemplate(macro)
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        )

        macroRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = macroAdapter
        }

        findViewById<ExtendedFloatingActionButton>(R.id.fabAddMacro).setOnClickListener {
            AddMacroDialog().show(supportFragmentManager, "AddMacroDialog")
        }

        macroViewModel.macroTemplates.observe(this) { macros ->
            macroAdapter.submitList(macros)
        }
    }

    // MainActivity.kt
    private fun showMacroDetailsDialog(macro: MacroTemplate) {
        macroViewModel.getMacroItems(macro.id) { items ->
            val dialogView = layoutInflater.inflate(R.layout.dialog_macro_details, null)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvMacroItems)

            val adapter = MacroItemAdapter(
                onItemClick = { /* 클릭 이벤트 필요 없음 */ },
                onDeleteClick = { /* 아무 작업도 수행하지 않음 */ }
            )
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter

            // timeOffset을 실제 시간으로 변환하여 표시
            val itemsWithTime = items.map { item ->
                item.copy(
                    content = item.timeOffset?.let { offset ->
                        val time = Calendar.getInstance().apply {
                            add(Calendar.MILLISECOND, offset.toInt())
                        }.time
                        "${item.content} (${SimpleDateFormat("HH:mm", Locale.getDefault()).format(time)})"
                    } ?: item.content
                )
            }

            adapter.submitList(itemsWithTime)

            AlertDialog.Builder(this)
                .setTitle("${macro.name} 상세")
                .setView(dialogView)
                .setPositiveButton("확인", null)
                .show()
        }
    }


    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                AlertDialog.Builder(this)
                    .setTitle("알림 권한 필요")
                    .setMessage("할 일 알림을 받기 위해서는 알림 권한이 필요합니다.")
                    .setPositiveButton("설정") { _, _ ->
                        try {
                            Intent().also { intent ->
                                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                intent.putExtra(Settings.EXTRA_CHANNEL_ID, packageName)
                                startActivity(intent)
                            }
                        } catch (e: Exception) {
                            // 앱 상세 설정으로 이동
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                            })
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }

        // 알람 권한도 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("알람 권한 필요")
                    .setMessage("정확한 시간에 알림을 받기 위해서는 알람 권한이 필요합니다.")
                    .setPositiveButton("설정") { _, _ ->
                        try {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:$packageName")
                            })
                        } catch (e: Exception) {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }
    }


    private fun updateWidget() {
        val intent = Intent(this, TodoWidget::class.java).apply {
            action = "com.minseok.reminderscreen.UPDATE_WIDGET"
        }
        sendBroadcast(intent)
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    showHomeView()
                    true
                }

                R.id.navigation_calendar -> {
                    showCalendarFragment()
                    true
                }

                R.id.navigation_feedback -> {
                    showFeedbackFragment()
                    true
                }

                else -> false
            }
        }
    }

    private fun showFeedbackFragment() {
        findViewById<View>(R.id.dateNavigation).visibility = View.GONE
        findViewById<View>(R.id.rvTodoList).visibility = View.GONE
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment !is FeedbackFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FeedbackFragment())
                .commit()
        }
    }

    private fun showHomeView() {
        findViewById<View>(R.id.dateNavigation).visibility = View.VISIBLE
        // 메인 화면에서는 항상 할일 목록을 보여줌
        findViewById<View>(R.id.rvTodoList).visibility = View.VISIBLE
        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    private fun showCalendarFragment() {
        findViewById<View>(R.id.dateNavigation).visibility = View.GONE
        findViewById<View>(R.id.rvTodoList).visibility = View.GONE
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment !is CalendarFragment) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CalendarFragment())
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_menu) {
            showCustomMenu(findViewById(R.id.action_menu))
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    private fun showCustomMenu(anchorView: View) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val customView = inflater.inflate(R.layout.custom_menu_layout, null)

        popupWindow = PopupWindow(
            customView,
            280.dpToPx(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            elevation = 8f
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            // 메뉴가 오른쪽 아래로 표시되도록 설정
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            showAtLocation(
                anchorView,
                Gravity.TOP or Gravity.END,
                16.dpToPx(), // 오른쪽 마진
                location[1] + anchorView.height // 앵커뷰 아래에 표시
            )
        }

        // 메뉴 아이템 클릭 리스너 설정
        customView.findViewById<TextView>(R.id.menu_notification).setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
            popupWindow.dismiss()
        }

        customView.findViewById<TextView>(R.id.menu_background).setOnClickListener {
            showColorPickerDialog()
            popupWindow.dismiss()
        }
// MainActivity.kt 의 showCustomMenu 함수 수정
        customView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_lock_screen).apply {
            isChecked = sharedPreferences.getBoolean("show_todo_list", false)
            setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit().putBoolean("show_todo_list", isChecked).apply()
                if (isChecked) {
                    startService(Intent(this@MainActivity, LockScreenService::class.java))
                } else {
                    stopService(Intent(this@MainActivity, LockScreenService::class.java))
                    sendBroadcast(Intent("com.minseok.reminderscreen.FINISH_LOCKSCREEN"))
                }
            }
        }

        // 팝업 표시
        popupWindow.showAsDropDown(anchorView, 0, 0)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()














    private fun showColorPickerDialog() {
        ColorPickerDialog.Builder(this, R.style.CustomDialogTheme)            .setTitle("배경색 선택")
            .setPositiveButton("선택", ColorEnvelopeListener { envelope, _ ->
                val color = envelope.color
                saveBackgroundColor(color)
            })
            .setNegativeButton("취소") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .setBottomSpace(12)
            .attachAlphaSlideBar(false)
            .attachBrightnessSlideBar(true)
            .apply {
                colorPickerView.apply {
                    elevation = 8f
                    background = GradientDrawable().apply {
                        cornerRadius = 20f
                        setColor(Color.WHITE)
                        setStroke(2, Color.parseColor("#E8E8E8")) // 테두리 추가
                    }
                    setPadding(20, 20, 20, 20)
                }

            }
            .show()
    }
    private fun saveBackgroundColor(color: Int) {
        sharedPreferences.edit().putInt("backgroundColor", color).apply()
    }








    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onItemClick = ::onTodoItemClick,
            onItemLongClick = ::onTodoItemLongClick,
            onItemCheckChanged = { updatedItem ->
                viewModel.updateTodoItem(updatedItem)
            }
        )

        val recyclerView = findViewById<RecyclerView>(R.id.rvTodoList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val todoItem = adapter.currentList[position].copy()

                when (direction) {
                    ItemTouchHelper.RIGHT -> {
                        todoItem.isCompleted = true
                        viewModel.updateTodoItem(todoItem)
                    }
                    ItemTouchHelper.LEFT -> {
                        todoItem.isCompleted = false
                        viewModel.updateTodoItem(todoItem)
                    }
                }
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

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }






    private fun setupObservers() {
        viewModel.todoItems.observe(this) { items ->
            // 전체 아이템을 받아서 현재 선택된 날짜의 아이템만 필터링
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
        findViewById<TextView>(R.id.tvCurrentDate).text =
            SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault()).format(currentDate.time)
    }

    private fun refreshTodoList() {
        // loadTodoItems 대신 새로운 방식 사용
        viewModel.todoItems.value?.let { items ->
            val filteredItems = items.filter { isSameDay(it.date, currentDate.time) }
            adapter.submitList(filteredItems)
        }
    }

    private fun showAddTodoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_todo, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val btnSetDate = dialogView.findViewById<Button>(R.id.btnSetDate)
        val btnSetTime = dialogView.findViewById<Button>(R.id.btnSetTime)



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

        // 버튼 공통 스타일
        val buttonBackground = GradientDrawable().apply {
            setColor(Color.parseColor("#E8F1FF"))
            cornerRadius = 16f
        }

        // 날짜 선택 버튼 스타일링
        btnSetDate.apply {
            background = buttonBackground
            setPadding(40, 20, 40, 20)
            setTextColor(Color.parseColor("#2196F3"))
            text = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                .format(currentDate.time)
            elevation = 4f
        }

        // 시간 선택 버튼 스타일링
        btnSetTime.apply {
            background = buttonBackground.constantState?.newDrawable()
            setPadding(40, 20, 40, 20)
            setTextColor(Color.parseColor("#2196F3"))
            text = "시간 선택"
            elevation = 4f
        }

        var selectedDate: Date = currentDate.time
        var selectedTime: Date? = null

        btnSetTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this, R.style.CustomDatePickerDialog2, { _, hourOfDay, minute ->
                    calendar.time = selectedDate
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)

                    selectedTime = calendar.time
                    btnSetTime.text = String.format("%02d:%02d", hourOfDay, minute)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        btnSetTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this, R.style.CustomDatePickerDialog2, { _, hourOfDay, minute ->
                    calendar.time = selectedDate
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)

                    selectedTime = calendar.time
                    btnSetTime.text = String.format("%02d:%02d", hourOfDay, minute)
                }, calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), false
            ).show()
        }

        val dialog = AlertDialog.Builder(this, R.style.RoundedAlertDialog)
            .setTitle("할 일 추가")
            .setView(dialogView)
            .setPositiveButton("추가", null)  // 여기에 추가
            .setNegativeButton("취소", null)  // 여기에 추가
            .create()

        // 다이얼로그 배경 설정
        dialog.window?.apply {
            setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 24f
            })
            // 다이얼로그 크기 설정
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }



        dialog.setOnShowListener {
            // 추가 버튼 스타일링
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                text = "추가"
                textSize = 16f
                setTextColor(Color.BLACK)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#00C853"))  // 좀 더 선명한 초록색
                    cornerRadius = 30f  // 더 둥글게
                    elevation = 8f
                }
                setPadding(70, 25, 70, 25)  // 좌우 패딩 증가

                setOnClickListener {
                    val content = etContent.text.toString()
                    if (content.isNotEmpty()) {
                        val todoItem = TodoItem(
                            content = content,
                            date = selectedDate,
                            time = selectedTime
                        )
                        viewModel.addTodoItem(todoItem)
                        updateWidget() // 기존 기능 유지

                        if (selectedTime != null) {
                            try {
                                notificationHelper.scheduleNotification(todoItem)
                                Toast.makeText(
                                    this@MainActivity,
                                    "알림이 ${
                                        SimpleDateFormat("HH:mm", Locale.getDefault())
                                            .format(selectedTime)
                                    } 에 설정되었습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "알림 설정에 실패했습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                e.printStackTrace()
                            }
                        }
                        dialog.dismiss()
                    } else {
                        etContent.error = "내용을 입력해주세요"
                    }
                }
            }

            // 취소 버튼 스타일링
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                text = "취소"
                textSize = 16f
                setTextColor(Color.BLACK)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FF5252"))  // 좀 더 선명한 빨간색
                    cornerRadius = 30f  // 추가 버튼과 동일하게
                    elevation = 8f  // 추가 버튼과 동일한 그림자
                }
                setPadding(70, 25, 70, 25)  // 추가 버튼과 동일한 패딩

                setOnClickListener { dialog.dismiss() }
            }
        }

        dialog.show()

// 다이얼로그 제목 스타일링
        dialog.findViewById<TextView>(android.R.id.title)?.apply {
            textSize = 20f
            setTextColor(Color.parseColor("#1976D2"))  // 좀 더 진한 파란색
            setPadding(40, 35, 40, 35)  // 상하 패딩 약간 증가
        }


    }












    private fun showEditTodoDialog(todoItem: TodoItem) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_todo, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val btnSetDate = dialogView.findViewById<Button>(R.id.btnSetDate)
        val btnSetTime = dialogView.findViewById<Button>(R.id.btnSetTime)

        // EditText 스타일링
        etContent.apply {
            setText(todoItem.content)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F5F9FF"))  // 밝은 하늘색 배경
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#E8F1FF"))  // 테두리
            }
            setPadding(40, 30, 40, 30)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
            textSize = 16f
        }

        // 버튼 공통 스타일
        val buttonStyle = GradientDrawable().apply {
            setColor(Color.parseColor("#E8F1FF"))
            cornerRadius = 16f
        }

        // 날짜 버튼 스타일링
        btnSetDate.apply {
            text = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault()).format(todoItem.date)
            background = buttonStyle.constantState?.newDrawable()
            setPadding(40, 20, 40, 20)
            setTextColor(Color.parseColor("#2196F3"))
            elevation = 4f
        }

        // 시간 버튼 스타일링
        btnSetTime.apply {
            text = todoItem.time?.let {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
            } ?: "시간 미설정"
            background = buttonStyle.constantState?.newDrawable()
            setPadding(40, 20, 40, 20)
            setTextColor(Color.parseColor("#2196F3"))
            elevation = 4f
        }

        var selectedDate: Date = todoItem.date
        var selectedTime: Date? = todoItem.time

        btnSetDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { time = selectedDate }
            DatePickerDialog(
                this,
                R.style.CustomDatePickerDialog,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    btnSetDate.text = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                        .format(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSetTime.setOnClickListener {
            val calendar = Calendar.getInstance().apply { time = selectedDate }
            TimePickerDialog(
                this,
                R.style.CustomDatePickerDialog2,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    selectedTime = calendar.time
                    btnSetTime.text = String.format("%02d:%02d", hourOfDay, minute)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        val dialog = AlertDialog.Builder(this, R.style.RoundedAlertDialog)
            .setTitle("할 일 수정")
            .setView(dialogView)
            .setPositiveButton("수정", null)
            .setNegativeButton("취소", null)
            .create()

        // 다이얼로그 배경 및 모서리 설정
        dialog.window?.apply {
            setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 24f
            })
            // 다이얼로그 크기 설정
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.setOnShowListener {
            // 수정 버튼 스타일링
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                text = "수정"
                textSize = 16f
                setTextColor(Color.BLACK)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#2196F3"))  // 파란색
                    cornerRadius = 30f
                }
                setPadding(70, 25, 70, 25)
                elevation = 8f

                setOnClickListener {
                    val content = etContent.text.toString()
                    if (content.isNotEmpty()) {
                        todoItem.content = content
                        todoItem.date = selectedDate
                        todoItem.time = selectedTime
                        viewModel.updateTodoItem(todoItem)

                        // 알림 업데이트
                        if (selectedTime != null) {
                            try {
                                notificationHelper.scheduleNotification(todoItem)
                                Toast.makeText(
                                    this@MainActivity,
                                    "알림이 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedTime)} 에 수정되었습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "알림 설정에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                e.printStackTrace()
                            }
                        }
                        dialog.dismiss()
                    } else {
                        etContent.error = "내용을 입력해주세요"
                    }
                }
            }

            // 취소 버튼 스타일링
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                text = "취소"
                textSize = 16f
                setTextColor(Color.BLACK)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FF5252"))  // 빨간색
                    cornerRadius = 30f
                }
                setPadding(70, 25, 70, 25)
                elevation = 8f
            }
        }

        // 다이얼로그 제목 스타일링
        dialog.show()
        dialog.findViewById<TextView>(android.R.id.title)?.apply {
            textSize = 20f
            setTextColor(Color.parseColor("#1976D2"))
            setPadding(40, 35, 40, 35)
            gravity = Gravity.CENTER
        }
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