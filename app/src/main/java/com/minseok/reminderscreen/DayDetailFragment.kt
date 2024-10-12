package com.minseok.reminderscreen

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import androidx.appcompat.app.AlertDialog
import android.widget.EditText

class DayDetailDialogFragment : DialogFragment() {
    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var dateText: TextView
    private lateinit var dayText: TextView
    private lateinit var addButton: FloatingActionButton
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_day_detail_popup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(TodoViewModel::class.java)

        dateText = view.findViewById(R.id.dateText)
        dayText = view.findViewById(R.id.dayText)
        recyclerView = view.findViewById(R.id.rvTodoList)
        addButton = view.findViewById(R.id.addButton)

        date = arguments?.getSerializable(ARG_DATE) as? LocalDate ?: LocalDate.now()

        setupRecyclerView()
        setupUI()
        updateTodoItems()
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(::onTodoItemClick, ::onTodoItemLongClick)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val todoItem = adapter.currentList[position]
                showDeleteConfirmationDialog(todoItem)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun setupUI() {
        val formatter = DateTimeFormatter.ofPattern("M월 d일")
        dateText.text = date.format(formatter)
        dayText.text = "${date.dayOfMonth} ${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}"

        addButton.setOnClickListener {
            showAddTodoDialog()
        }
    }

    private fun updateTodoItems() {
        val items = viewModel.getTodoItemsForDate(date)
        adapter.submitList(items)
        dataChangeListener?.onDataChanged()
    }

    private fun onTodoItemClick(todoItem: TodoItem) {
        showEditTodoDialog(todoItem)
    }

    private fun onTodoItemLongClick(todoItem: TodoItem) {
        showDeleteConfirmationDialog(todoItem)
    }

    private fun showAddTodoDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_todo, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val btnSetTime = dialogView.findViewById<TextView>(R.id.btnSetTime)

        var selectedTime: LocalTime? = null

        btnSetTime.setOnClickListener {
            val currentTime = LocalTime.now()
            TimePickerDialog(context, { _, hourOfDay, minute ->
                selectedTime = LocalTime.of(hourOfDay, minute)
                btnSetTime.text = selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
            }, currentTime.hour, currentTime.minute, false).show()
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
                        time = selectedTime?.let { localTimeToDate(it) }
                    )
                    viewModel.addTodoItem(todoItem)
                    updateTodoItems()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditTodoDialog(todoItem: TodoItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_todo, null)
        val etContent = dialogView.findViewById<EditText>(R.id.etContent)
        val btnSetTime = dialogView.findViewById<TextView>(R.id.btnSetTime)

        etContent.setText(todoItem.content)
        btnSetTime.text = todoItem.time?.let {
            LocalTime.ofInstant(it.toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
        } ?: "시간 미설정"

        var selectedTime: LocalTime? = todoItem.time?.let {
            LocalTime.ofInstant(it.toInstant(), ZoneId.systemDefault())
        }

        btnSetTime.setOnClickListener {
            val currentTime = selectedTime ?: LocalTime.now()
            TimePickerDialog(context, { _, hourOfDay, minute ->
                selectedTime = LocalTime.of(hourOfDay, minute)
                btnSetTime.text = selectedTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
            }, currentTime.hour, currentTime.minute, false).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("할 일 수정")
            .setView(dialogView)
            .setPositiveButton("수정") { _, _ ->
                val content = etContent.text.toString()
                if (content.isNotEmpty()) {
                    todoItem.content = content
                    todoItem.time = selectedTime?.let { localTimeToDate(it) }
                    viewModel.updateTodoItem(todoItem)
                    updateTodoItems()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun localDateToDate(localDate: LocalDate): Date {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
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
                updateTodoItems()
            }
            .setNegativeButton("취소") { _, _ ->
                updateTodoItems()  // 삭제를 취소한 경우 목록을 다시 로드
            }
            .show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}