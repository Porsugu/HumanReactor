//package com.example.humanreactor.customizedMove
//
//import android.annotation.SuppressLint
//import android.view.LayoutInflater
//import android.view.MotionEvent
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.RelativeLayout
//import android.widget.TextView
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.core.view.isInvisible
//import androidx.recyclerview.widget.RecyclerView
//import com.example.humanreactor.R
//
//class MovesAdapter(
//    private val moves: List<Move>,
//    private val onEditClick: (Int) -> Unit,
//    private val onDeleteClick: (Int) -> Unit
//) : RecyclerView.Adapter<MovesAdapter.MoveViewHolder>() {
//
//    class MoveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
//        val tvMoveName: TextView = itemView.findViewById(R.id.tvMoveName)
////        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
////        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
//        val editBTN:RelativeLayout = itemView.findViewById(R.id.editBTN)
//        val delBTN:RelativeLayout = itemView.findViewById(R.id.delBTN)
//        val editBTN_Cover:ConstraintLayout = itemView.findViewById(R.id.editBTN_Cover)
//        val delBTN_Cover:ConstraintLayout = itemView.findViewById(R.id.delBTN_Cover)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_move, parent, false)
//        return MoveViewHolder(view)
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onBindViewHolder(holder: MoveViewHolder, position: Int) {
//        val move = moves[position]
//
//        holder.tvMoveName.text = move.name
//        holder.colorIndicator.setBackgroundColor(move.color)
//
//
//        holder.delBTN.setOnTouchListener(null)
//        holder.editBTN.setOnTouchListener(null)
//
//        // Set new listeners
//        holder.delBTN.setOnTouchListener { view, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    // RelativeLayout is pressed down
//                    holder.delBTN_Cover.isInvisible = false
//                    true
//                }
//                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                    // RelativeLayout is released or touch is canceled
//                    holder.delBTN_Cover.isInvisible = true
//                    onDeleteClick(position)
//                    true
//                }
//                else -> false
//            }
//        }
//        holder.editBTN.setOnTouchListener { view, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    // RelativeLayout is pressed down
//                    holder.editBTN_Cover.isInvisible = false
//                    true
//                }
//                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                    // RelativeLayout is released or touch is canceled
//                    holder.editBTN_Cover.isInvisible = true
//                    onEditClick(position)
//                    true
//                }
//                else -> false
//            }
//        }
////        holder.editBTN.setOnClickListener { onEditClick(position) }
////        holder.delBTN.setOnClickListener { onDeleteClick(position) }
//    }
//
//    override fun getItemCount() = moves.size
//
//    // Force complete rebind when items change
//    override fun getItemId(position: Int): Long {
//        return position.toLong()
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return position
//    }
//}
package com.example.humanreactor.customizedMove

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.example.humanreactor.R

class MovesAdapter(
    private val moves: MutableList<Move>,
    private val onEditClick: (Int, Move) -> Unit,
    private val onDeleteClick: (Int, Move) -> Unit
) : RecyclerView.Adapter<MovesAdapter.MoveViewHolder>() {

    class MoveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val colorIndicator: View = itemView.findViewById(R.id.colorIndicator)
        val tvMoveName: TextView = itemView.findViewById(R.id.tvMoveName)
        val editBTN: View = itemView.findViewById(R.id.editBTN)
        val delBTN: View = itemView.findViewById(R.id.delBTN)
        val editBTN_Cover: ConstraintLayout = itemView.findViewById(R.id.editBTN_Cover)
        val delBTN_Cover: ConstraintLayout = itemView.findViewById(R.id.delBTN_Cover)
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

        holder.delBTN.setOnTouchListener(null)
        holder.editBTN.setOnTouchListener(null)

        // Set new listeners
        holder.delBTN.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Button is pressed down
                    holder.delBTN_Cover.isInvisible = false
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Button is released
                    holder.delBTN_Cover.isInvisible = true
                    onDeleteClick(position, move)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    // Touch is canceled
                    holder.delBTN_Cover.isInvisible = true
                    true
                }
                else -> false
            }
        }

        holder.editBTN.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Button is pressed down
                    holder.editBTN_Cover.isInvisible = false
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Button is released
                    holder.editBTN_Cover.isInvisible = true
                    onEditClick(position, move)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    // Touch is canceled
                    holder.editBTN_Cover.isInvisible = true
                    true
                }
                else -> false
            }
        }
    }

    override fun getItemCount() = moves.size

    // Force complete rebind when items change
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun getPositionById(id: Int): Int {
        return moves.indexOfFirst { it.dbId == id }
    }

    // 从列表中移除项目
    fun removeItem(position: Int) {
        if (position >= 0 && position < moves.size) {
            moves.removeAt(position)
        }
    }
}