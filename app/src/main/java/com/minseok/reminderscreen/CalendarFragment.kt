package com.minseok.reminderscreen

import android.graphics.Typeface
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
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

class CalendarFragment : Fragment(), DayDetailDialogFragment.DataChangeListener {

    private lateinit var viewModel: TodoViewModel
    private lateinit var adapter: TodoAdapter
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = TodoViewModel.getInstance(requireActivity().application)

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
                    // 해당 날짜의 일정 확인
                    val date = Date.from(data.date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                    val events = viewModel.getTodoItemsByDate(date)

                    if (events.isNotEmpty()) {
                        // 일정이 있는 경우 날짜 텍스트 색상 변경
                        container.textView.setTextColor(resources.getColor(R.color.blue_500)) // 또는 원하는 색상
                        container.textView.typeface = Typeface.DEFAULT_BOLD  // 글자를 두껍게 처리
                    } else {
                        // 일정이 없는 경우 기본 검정색
                        container.textView.setTextColor(resources.getColor(android.R.color.black))
                        container.textView.typeface = Typeface.DEFAULT
                    }

                    // 일정 텍스트 표시
                    container.eventsTextView.text = events.take(3).joinToString("\n") { it.content }
                    container.eventsTextView.setTextColor(resources.getColor(android.R.color.black))
                } else {
                    // 현재 월이 아닌 날짜 처리
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
        fragment.setDataChangeListener(object : DayDetailDialogFragment.DataChangeListener {
            override fun onDataChanged() {
                // 데이터가 변경되면 즉시 UI 업데이트
                calendarView.notifyCalendarChanged()
                val javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
                val filteredItems = viewModel.getTodoItemsByDate(javaDate)
                adapter.submitList(filteredItems)
            }
        })
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
            // 현재 선택된 날짜의 데이터만 갱신
            calendarView.findFirstVisibleMonth()?.let { month ->
                updateTodoList(month.yearMonth.atDay(1))
            }
        }
    }

    private fun updateTodoList(date: LocalDate) {
        // LocalDate를 Date로 변환하여 데이터 조회
        val javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val filteredItems = viewModel.getTodoItemsByDate(javaDate)
        adapter.submitList(filteredItems)
    }

    private fun onTodoItemClick(todoItem: TodoItem) {
        // TODO: Implement edit functionality
    }

    private fun onTodoItemLongClick(todoItem: TodoItem) {
        // TODO: Implement delete functionality
    }
}