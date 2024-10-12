package com.minseok.reminderscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.YearMonth
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

class CalendarFragment : Fragment(), DayDetailDialogFragment.DataChangeListener{

    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(TodoViewModel::class.java)

        calendarView = view.findViewById(R.id.calendarView)
        recyclerView = view.findViewById(R.id.rvTodoList)

        setupCalendarView()
        setupRecyclerView()
        observeTodoItems()
    }

    private fun setupCalendarView() {
        val currentMonth = YearMonth.now()
        val firstMonth = currentMonth.minusMonths(10)
        val lastMonth = currentMonth.plusMonths(10)
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        calendarView.setup(firstMonth, lastMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        class DayViewContainer(view: View) : ViewContainer(view) {
            val textView = view.findViewById<TextView>(R.id.calendarDayText)
            val eventsTextView = view.findViewById<TextView>(R.id.calendarDayEvents)
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.textView.text = data.date.dayOfMonth.toString()
                if (data.position == DayPosition.MonthDate) {
                    container.textView.setTextColor(resources.getColor(android.R.color.black))
                    // 해당 날짜의 일정 표시
                    val events = viewModel.getTodoItemsForDate(data.date)
                    container.eventsTextView.text = events.take(3).joinToString("\n") { it.content }
                } else {
                    container.textView.setTextColor(resources.getColor(android.R.color.darker_gray))
                    container.eventsTextView.text = ""
                }
                container.view.setOnClickListener {
                    updateTodoList(data.date)
                    navigateToDayDetail(data.date)

                }
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            val textView = view.findViewById<TextView>(R.id.headerTextView)
        }

        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                val month = data.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                val year = data.yearMonth.year
                container.textView.text = "$month $year"
            }
        }

        calendarView.monthScrollListener = { month ->
            updateTodoList(month.yearMonth.atDay(1))
        }
    }
    private fun navigateToDayDetail(date: LocalDate) {
        val fragment = DayDetailDialogFragment.newInstance(date)
        fragment.setDataChangeListener(this)
        fragment.show(parentFragmentManager, "DayDetailDialog")
    }
    override fun onDataChanged() {
        calendarView.notifyCalendarChanged()
        updateTodoList(YearMonth.now().atDay(1))
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(::onTodoItemClick, ::onTodoItemLongClick)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeTodoItems() {
        viewModel.todoItems.observe(viewLifecycleOwner) { items ->
            calendarView.notifyCalendarChanged()
            updateTodoList(YearMonth.now().atDay(1))
        }
    }

    private fun updateTodoList(date: LocalDate) {
        val filteredItems = viewModel.getTodoItemsForDate(date)
        adapter.submitList(filteredItems)
    }

    private fun onTodoItemClick(todoItem: TodoItem) {
        // TODO: Implement edit functionality
    }

    private fun onTodoItemLongClick(todoItem: TodoItem) {
        // TODO: Implement delete functionality
    }
}