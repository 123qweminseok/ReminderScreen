package com.minseok.reminderscreen

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class LockScreenActivity : AppCompatActivity() {
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 잠금 화면 위에 액티비티를 표시하기 위한 플래그 설정
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_lock_screen)

        viewModel = ViewModelProvider(this).get(TodoViewModel::class.java)
        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(::onTodoItemClick, ::onTodoItemLongClick)
        findViewById<RecyclerView>(R.id.rvTodoList).apply {
            layoutManager = LinearLayoutManager(this@LockScreenActivity)
            this.adapter = this@LockScreenActivity.adapter
        }
    }

    private fun setupObservers() {
        viewModel.todoItems.observe(this) { items ->
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            adapter.submitList(items.filter { it.date >= today })
        }
    }

    private fun onTodoItemClick(todoItem: TodoItem) {
        // 잠금 화면에서는 아이템 클릭 시 간단한 상태 변경만 수행
        todoItem.isCompleted = !todoItem.isCompleted
        viewModel.updateTodoItem(todoItem)
    }

    private fun onTodoItemLongClick(todoItem: TodoItem) {
        // 잠금 화면에서는 길게 누르기 동작을 무시
    }
}