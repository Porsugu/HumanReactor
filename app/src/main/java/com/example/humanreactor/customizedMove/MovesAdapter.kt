package com.example.humanreactor.customizedMove

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.humanreactor.R

class MovesAdapter(
    private val moves: List<Move>,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<MovesAdapter.MoveViewHolder>() {

    class MoveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        val tvMoveName: TextView = itemView.findViewById(R.id.tvMoveName)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_move, parent, false)
        return MoveViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        val move = moves[position]

        holder.tvMoveName.text = move.name
        holder.colorIndicator.setBackgroundColor(move.color)

        // Use a new OnClickListener each time to prevent issues
        holder.btnEdit.setOnClickListener(null) // Clear previous listener first
        holder.btnDelete.setOnClickListener(null) // Clear previous listener first

        // Set new listeners
        holder.btnEdit.setOnClickListener { onEditClick(position) }
        holder.btnDelete.setOnClickListener { onDeleteClick(position) }
    }

    override fun getItemCount() = moves.size

    // Force complete rebind when items change
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
}