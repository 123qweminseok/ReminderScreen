package com.minseok.reminderscreen

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
class FeedbackFragment : Fragment() {
    private lateinit var viewModel: TodoViewModel
    private lateinit var completedTasksCount: TextView
    private lateinit var inProgressTasksCount: TextView
    private lateinit var weeklyChart: LineChart
    private lateinit var monthlyPieChart: PieChart
    private lateinit var dialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feedback, container, false)
        viewModel = TodoViewModel.getInstance(requireActivity().application)
        initializeViews(view)
        setupCharts()
        observeData()
        return view
    }

    private fun initializeViews(view: View) {
        completedTasksCount = view.findViewById(R.id.completedTasksCount)
        inProgressTasksCount = view.findViewById(R.id.inProgressTasksCount)
        weeklyChart = view.findViewById(R.id.weeklyChart)
        monthlyPieChart = view.findViewById(R.id.monthlyPieChart)

        view.findViewById<MaterialButton>(R.id.btnViewIncomplete).setOnClickListener {
            showIncompleteTasksDialog()
        }
    }







    private fun showIncompleteTasksDialog() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)

        val incompleteTasks = viewModel.todoItems.value?.filter {
            !it.isCompleted && it.date.after(calendar.time)
        }?.sortedBy { it.date }?.toMutableList() ?: mutableListOf()

        if (incompleteTasks.isEmpty()) {
            Toast.makeText(context, "미완료 작업이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_incomplete_tasks, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvIncompleteTasks)
        val adapter = IncompleteTasksAdapter(incompleteTasks)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // 스와이프 기능 추가
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val task = adapter.getTaskAt(position)

                task.isCompleted = true
                viewModel.updateTodoItem(task)
                adapter.removeTask(position)

                if (adapter.itemCount == 0) {
                    dialog.dismiss()
                    Toast.makeText(context, "모든 작업이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                }
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
                if (isCurrentlyActive) {
                    val itemView = viewHolder.itemView
                    val background = GradientDrawable()
                    background.setColor(Color.parseColor("#4CAF50"))
                    background.cornerRadius = 12f
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })


        itemTouchHelper.attachToRecyclerView(recyclerView)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        dialog = AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }




    private class IncompleteTasksAdapter(
        private val tasks: MutableList<TodoItem>
    ) : RecyclerView.Adapter<IncompleteTasksAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvContent: TextView = view.findViewById(R.id.tvContent)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
        }

        fun getTaskAt(position: Int): TodoItem = tasks[position]

        fun removeTask(position: Int) {
            tasks.removeAt(position)
            notifyItemRemoved(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_incomplete_task, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val task = tasks[position]
            holder.tvContent.text = task.content
            holder.tvDate.text = SimpleDateFormat("MM월 dd일 (E)", Locale.KOREAN)
                .format(task.date)
        }

        override fun getItemCount() = tasks.size
    }














    private fun setupCharts() {
        weeklyChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(arrayOf("일", "월", "화", "수", "목", "금", "토"))
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
            }

            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 10f
                setDrawGridLines(true)
                labelCount = 6
            }

            axisRight.isEnabled = false
            setDrawBorders(false)
        }

        monthlyPieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            legend.isEnabled = false
            setDrawEntryLabels(false)
        }
    }

    private fun observeData() {
        viewModel.todoItems.observe(viewLifecycleOwner) { items ->
            updateTodayStats(items)
            updateWeeklyChart(items)
            updateMonthlyStats(items)
        }
    }

    private fun updateTodayStats(items: List<TodoItem>) {
        val today = Calendar.getInstance().time
        val todayItems = items.filter { isSameDay(it.date, today) }

        val completedCount = todayItems.count { it.isCompleted }
        val inProgressCount = todayItems.count { !it.isCompleted }

        completedTasksCount.text = completedCount.toString()
        inProgressTasksCount.text = inProgressCount.toString()
    }

    private fun updateWeeklyChart(items: List<TodoItem>) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_WEEK, -(calendar.get(Calendar.DAY_OF_WEEK) - 1))

        val weeklyData = mutableListOf<Entry>()

        for (i in 0..6) {
            val dayItems = items.filter { isSameDay(it.date, calendar.time) }
            val completedCount = dayItems.count { it.isCompleted }
            weeklyData.add(Entry(i.toFloat(), completedCount.toFloat()))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val dataSet = LineDataSet(weeklyData, "완료된 작업").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
        }

        weeklyChart.data = LineData(dataSet)
        weeklyChart.invalidate()
    }

    private fun updateMonthlyStats(items: List<TodoItem>) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)

        val monthlyIncompleteCount = items.count {
            !it.isCompleted && it.date.after(calendar.time)
        }

        val pieEntries = listOf(
            PieEntry(monthlyIncompleteCount.toFloat(), "미완료"),
            PieEntry((items.size - monthlyIncompleteCount).toFloat(), "완료")
        )

        val dataSet = PieDataSet(pieEntries, "월간 통계").apply {
            colors = listOf(Color.RED, Color.BLUE)
            valueTextSize = 14f
            valueTextColor = Color.WHITE
        }

        monthlyPieChart.data = PieData(dataSet)
        monthlyPieChart.invalidate()
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
}