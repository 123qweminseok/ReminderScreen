package com.minseok.reminderscreen

import android.app.TimePickerDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class DayDetailDialogFragment : DialogFragment() {
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var dateText: TextView
    private lateinit var dayText: TextView
    private lateinit var addButton: MaterialButton
    private lateinit var bottomSheet: CardView
    private lateinit var backgroundDim: View
    private lateinit var date: LocalDate

    interface DataChangeListener {
        fun onDataChanged()
    }

    private var dataChangeListener: DataChangeListener? = null

    fun setDataChangeListener(listener: DataChangeListener) {
        this.dataChangeListener = listener
    }

    companion object {
        private const val ARG_DATE = "arg_date"

        fun newInstance(date: LocalDate): DayDetailDialogFragment {
            val fragment = DayDetailDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_DATE, date)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_day_detail_popup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = TodoViewModel.getInstance(requireActivity().application)

        bottomSheet = view.findViewById(R.id.bottomSheet)
        backgroundDim = view.findViewById(R.id.backgroundDim)
        dateText = view.findViewById(R.id.dateText)
        dayText = view.findViewById(R.id.dayText)
        recyclerView = view.findViewById(R.id.rvTodoList)
        addButton = view.findViewById(R.id.addButton)

        date = arguments?.getSerializable(ARG_DATE) as? LocalDate ?: LocalDate.now()

        setupRecyclerView()
        setupUI()
        setupAnimation()
        updateTodoItems()
    }








    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onItemClick = { todoItem ->
                showEditTodoDialog(todoItem)
            },
            onItemLongClick = { todoItem ->
                showDeleteConfirmationDialog(todoItem)
            },
            onItemCheckChanged = { updatedItem ->
                viewModel.updateTodoItem(updatedItem)
                dataChangeListener?.onDataChanged()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val todoItem = adapter.currentList[position].copy()

                // 새로운 상태로 아이템 업데이트
                val updatedItem = todoItem.copy(
                    isCompleted = direction == ItemTouchHelper.RIGHT,
                    createdAt = System.currentTimeMillis()
                )

                // 즉시 UI 업데이트
                val currentList = adapter.currentList.toMutableList()
                currentList[position] = updatedItem
                adapter.submitList(currentList) {
                    // 리스트 제출이 완료된 후에 실행
                    adapter.notifyItemChanged(position)
                }

                // ViewModel 업데이트
                viewModel.updateTodoItem(updatedItem)

                // 캘린더 뷰 업데이트
                dataChangeListener?.onDataChanged()
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
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    // 스와이프 방향에 따른 배경색 설정이나 아이콘 추가 가능
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }






    private fun setupUI() {
        val formatter = DateTimeFormatter.ofPattern("M월 d일")
        dateText.text = "음력 ${date.format(formatter)}"
        dayText.text = "${date.dayOfMonth} ${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}"

        // 추가 버튼의 텍스트를 동적으로 설정
        addButton.text = "${date.format(formatter)}에 추가"

        addButton.setOnClickListener {
            showAddTodoDialog()
        }

        view?.findViewById<ImageButton>(R.id.btnMore)?.setOnClickListener {
            // 추가 옵션 메뉴 표시
        }

        backgroundDim.setOnClickListener {
            dismiss()
        }
    }
    private fun setupAnimation() {
        backgroundDim.alpha = 0f
        backgroundDim.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        bottomSheet.translationY = 1000f
        bottomSheet.alpha = 0f
        bottomSheet.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun updateTodoItems() {
        val items = viewModel.getTodoItemsForDate(date)
        adapter.submitList(items)
        dataChangeListener?.onDataChanged()
    }

    private fun showAddTodoDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_todo, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val btnSetTime = dialogView.findViewById<TextView>(R.id.btnSetTime)

        var selectedTime: Date? = null

        btnSetTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(context, { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                selectedTime = calendar.time
                btnSetTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("할 일 추가")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val content = etContent.text.toString()
                if (content.isNotEmpty()) {
                    val todoItem = TodoItem(
                        content = content,
                        date = localDateToDate(date),
                        time = selectedTime,
                        createdAt = System.currentTimeMillis()
                    )
                    viewModel.addTodoItem(todoItem)
                    updateTodoItems()
                }
            }
            .setNegativeButton("취소", null)
            .create()
            .apply {
                show()
                getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(resources.getColor(android.R.color.white, null))
                    setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
                }
                getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(resources.getColor(android.R.color.white, null))
                    setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
            }
    }

    private fun showEditTodoDialog(todoItem: TodoItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_todo, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val btnSetTime = dialogView.findViewById<TextView>(R.id.btnSetTime)

        // 기존 데이터로 초기화
        etContent.setText(todoItem.content)

        var selectedTime: Date? = todoItem.time
        btnSetTime.text = selectedTime?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        } ?: "시간 설정"

        btnSetTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            selectedTime?.let { calendar.time = it }

            TimePickerDialog(context, { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                selectedTime = calendar.time
                btnSetTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("할 일 수정")
            .setView(dialogView)
            .setPositiveButton("수정") { _, _ ->
                val content = etContent.text.toString()
                if (content.isNotEmpty()) {
                    val updatedItem = todoItem.copy(
                        content = content,
                        time = selectedTime,
                        createdAt = System.currentTimeMillis()
                    )

                    // 즉시 UI 업데이트
                    val currentList = adapter.currentList.toMutableList()
                    val position = currentList.indexOfFirst { it.id == todoItem.id }
                    if (position != -1) {
                        currentList[position] = updatedItem
                        adapter.submitList(currentList)
                    }

                    // ViewModel 업데이트
                    viewModel.updateTodoItem(updatedItem)

                    // 캘린더 뷰 업데이트
                    dataChangeListener?.onDataChanged()
                }
            }
            .setNegativeButton("취소", null)
            .create()
            .apply {
                show()
                getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                    setTextColor(resources.getColor(android.R.color.white, null))
                    setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
                }
                getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                    setTextColor(resources.getColor(android.R.color.white, null))
                    setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
            }
    }

    private fun localDateToDate(localDate: LocalDate): Date {
        val calendar = Calendar.getInstance()
        calendar.set(
            localDate.year,
            localDate.monthValue - 1,  // Calendar의 월은 0-based
            localDate.dayOfMonth,
            0, 0, 0
        )
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }









    private fun localTimeToDate(localTime: LocalTime): Date {
        val localDateTime = LocalDate.now().atTime(localTime)
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
    }

    private fun showDeleteConfirmationDialog(todoItem: TodoItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("할 일 삭제")
            .setMessage("이 할 일을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteTodoItem(todoItem)
                dataChangeListener?.onDataChanged()
                val javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                val items = viewModel.getTodoItemsByDate(javaDate)
                adapter.submitList(items)
            }
            .setNegativeButton("취소") { _, _ ->
                val javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                val items = viewModel.getTodoItemsByDate(javaDate)
                adapter.submitList(items)
            }
            .show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.DialogAnimation
        }
    }

    override fun dismiss() {
        backgroundDim.animate()
            .alpha(0f)
            .setDuration(200)
            .start()

        bottomSheet.animate()
            .translationY(bottomSheet.height.toFloat())
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                super.dismiss()
            }
            .start()
    }
}