package com.example.humanreactor.aiQuestion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.humanreactor.R

class OptionAdapter (
    private val options : List<String>,
    private var selectedPosition : Int = -1,
    private val onOptionSelected: (Int, String) -> Unit)
    : RecyclerView.Adapter<OptionAdapter.OptionViewHolder>(){


        class OptionViewHolder(view : View) : RecyclerView.ViewHolder(view) {
            val optionText: TextView = view.findViewById(R.id.optionText)
            val optionCard: CardView = view.findViewById(R.id.optionCard)

        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ai_question_item_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        val option = options[position]
        holder.optionText.text = option

        // Set background based on selection state
        if (position == selectedPosition) {
            holder.optionCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.red)
            )
        } else {
            holder.optionCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.green)
            )
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            val previouslySelected = selectedPosition
            selectedPosition = position

            // Notify adapter about changes
            if (previouslySelected != -1) { notifyItemChanged(previouslySelected) }
            notifyItemChanged(selectedPosition)

            // Pass selection to callback
            onOptionSelected(position, option)
        }

    }

    override fun getItemCount() = options.size

    // method to reset the selection
    fun resetSelection() {
        val previouslySelected = selectedPosition
        selectedPosition = -1
        if (previouslySelected != -1) {
            notifyItemChanged(previouslySelected)
        }
    }
}