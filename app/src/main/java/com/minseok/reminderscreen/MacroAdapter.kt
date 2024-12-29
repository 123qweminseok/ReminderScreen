// MacroAdapter.kt
package com.minseok.reminderscreen.ui.macro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.minseok.reminderscreen.R
import com.minseok.reminderscreen.data.model.MacroTemplate

class MacroAdapter(
    private val onMacroClick: (MacroTemplate) -> Unit,
    private val onMacroDoubleClick: (MacroTemplate) -> Unit,
    private val onMacroLongClick: (MacroTemplate) -> Unit
) : RecyclerView.Adapter<MacroAdapter.MacroViewHolder>() {
    private var macros = listOf<MacroTemplate>()  // 이 줄 추가

    private var lastClickTime = 0L
    private val DOUBLE_CLICK_TIME_DELTA: Long = 300 // 더블 클릭 간격 (밀리초)

    inner class MacroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvName: TextView = view.findViewById(R.id.tvMacroName)
        private val tvDescription: TextView = view.findViewById(R.id.tvMacroDescription)
        private val tvItemCount: TextView = view.findViewById(R.id.tvItemCount)

        fun bind(macro: MacroTemplate) {
            tvName.text = macro.name
            tvDescription.text = macro.description

            itemView.setOnClickListener {
                val clickTime = System.currentTimeMillis()
                if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                    // 더블 클릭
                    onMacroDoubleClick(macro)
                } else {
                    // 싱글 클릭
                    onMacroClick(macro)
                }
                lastClickTime = clickTime
            }

            itemView.setOnLongClickListener {
                onMacroLongClick(macro)
                true
            }
        }
    }




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MacroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_macro, parent, false)
        return MacroViewHolder(view)
    }

    override fun onBindViewHolder(holder: MacroViewHolder, position: Int) {
        holder.bind(macros[position])
    }

    override fun getItemCount() = macros.size

    fun submitList(newMacros: List<MacroTemplate>) {
        macros = newMacros
        notifyDataSetChanged()
    }
}