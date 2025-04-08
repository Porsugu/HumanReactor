package com.example.humanreactor.customizedMove

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
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
//        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
//        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val editBTN:RelativeLayout = itemView.findViewById(R.id.editBTN)
        val delBTN:RelativeLayout = itemView.findViewById(R.id.delBTN)
        val editBTN_Cover:ConstraintLayout = itemView.findViewById(R.id.editBTN_Cover)
        val delBTN_Cover:ConstraintLayout = itemView.findViewById(R.id.delBTN_Cover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_move, parent, false)
        return MoveViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
        val move = moves[position]

        holder.tvMoveName.text = move.name
        holder.colorIndicator.setBackgroundColor(move.color)

        // Use a new OnClickListener each time to prevent issues
//        holder.btnEdit.setOnClickListener(null) // Clear previous listener first
//        holder.btnDelete.setOnClickListener(null) // Clear previous listener first
//        holder.delBTN.setOnClickListener(null)
//        holder.editBTN.setOnClickListener(null)
        holder.delBTN.setOnTouchListener(null)
        holder.editBTN.setOnTouchListener(null)

        // Set new listeners

//        holder.btnEdit.setOnClickListener { onEditClick(position) }
//        holder.btnDelete.setOnClickListener { onDeleteClick(position) }
        holder.delBTN.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // RelativeLayout is pressed down
                    holder.delBTN_Cover.isInvisible = false
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // RelativeLayout is released or touch is canceled
                    holder.delBTN_Cover.isInvisible = true
                    onDeleteClick(position)
                    true
                }
                else -> false
            }
        }
        holder.editBTN.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // RelativeLayout is pressed down
                    holder.editBTN_Cover.isInvisible = false
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // RelativeLayout is released or touch is canceled
                    holder.editBTN_Cover.isInvisible = true
                    onEditClick(position)
                    true
                }
                else -> false
            }
        }
//        holder.editBTN.setOnClickListener { onEditClick(position) }
//        holder.delBTN.setOnClickListener { onDeleteClick(position) }
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