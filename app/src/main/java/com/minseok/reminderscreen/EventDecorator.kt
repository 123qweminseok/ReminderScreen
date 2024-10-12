package com.minseok.reminderscreen

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.style.LineBackgroundSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class EventDecorator(private val eventDates: Map<CalendarDay, List<String>>) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay): Boolean {
        return eventDates.containsKey(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(object : LineBackgroundSpan {
            override fun drawBackground(
                canvas: Canvas,
                paint: Paint,
                left: Int,
                right: Int,
                top: Int,
                baseline: Int,
                bottom: Int,
                text: CharSequence,
                start: Int,
                end: Int,
                lnum: Int
            ) {
                val events = eventDates.values.firstOrNull() ?: return

                val originalTextSize = paint.textSize
                val originalColor = paint.color

                paint.textSize = 10f

                val yPos = bottom + 10f  // 날짜 아래에 위치하도록 조정
                val maxEventsToShow = 3

                events.take(maxEventsToShow).forEachIndexed { index, event ->
                    when (index) {
                        0 -> paint.color = Color.rgb(76, 175, 80)  // 초록
                        1 -> paint.color = Color.rgb(33, 150, 243) // 파랑
                        2 -> paint.color = Color.rgb(244, 67, 54)  // 빨강
                    }
                    val eventText = event.take(4) + if (event.length > 4) ".." else ""
                    canvas.drawText(eventText, left.toFloat(), yPos + (index * 12f), paint)
                }

                // 원래 페인트 설정 복구
                paint.textSize = originalTextSize
                paint.color = originalColor
            }
        })
    }
}