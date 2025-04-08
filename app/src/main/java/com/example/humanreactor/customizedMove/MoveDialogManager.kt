package com.example.humanreactor.customizedMove

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.humanreactor.R

class MoveDialogManager(
    private val context: Context,
    private val moves: MutableList<Move>,
    private val usedColors: MutableSet<Int>
) {
    private val TAG = "MoveDialogManager"

    // Show the main Move management dialog
    fun showMoveManagementDialog() {
        try {
            // Inflate the dialog layout
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_manage_moves, null)


            // Create dialog
            val dialog = AlertDialog.Builder(context)
                .setTitle("Manage Moves")
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Find views
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
            val btnAddMove = dialogView.findViewById<Button>(R.id.btnAddMove)
            val btnClose = dialogView.findViewById<Button>(R.id.btnClose)


            // Setup RecyclerView with adapter
            val movesAdapter = MovesAdapter(
                moves,
                onEditClick = { position -> showEditDialog(dialog, position) },
                onDeleteClick = { position ->
                    if (position < moves.size) {
                        val moveToDelete = moves[position]
                        usedColors.remove(moveToDelete.color)
                        moves.removeAt(position)
                        recyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            )

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = movesAdapter

            // Setup Add button
            btnAddMove.setOnClickListener {
                showAddDialog(dialog, recyclerView)
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.setOnShowListener {
                recyclerView.adapter?.notifyDataSetChanged()
                recyclerView.post {
                    recyclerView.requestLayout()
                }
            }
            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in main dialog", e)
            Toast.makeText(context, "Error creating dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Show dialog for adding a new Move
    private fun showAddDialog(parentDialog: AlertDialog, recyclerView: RecyclerView) {
        try {
            // Create a custom layout
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // Create name input
            val nameLabel = TextView(context)
            nameLabel.text = "Move Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.hint = "Enter name"
            layout.addView(nameInput)

            // Add error text for name
            val nameErrorText = TextView(context)
            nameErrorText.text = "This name is already taken. Please choose another."
            nameErrorText.setTextColor(Color.RED)
            nameErrorText.visibility = View.GONE
            layout.addView(nameErrorText)

            // Add spacing
            val space = Space(context)
            space.minimumHeight = 20
            layout.addView(space)

            // Create color selection
            val colorLabel = TextView(context)
            colorLabel.text = "Select Color:"
            layout.addView(colorLabel)

            // Create color preview
            val colorPreview = View(context)
            colorPreview.setBackgroundColor(Color.WHITE)
            val previewParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100
            )
            previewParams.setMargins(0, 10, 0, 20)
            colorPreview.layoutParams = previewParams
            layout.addView(colorPreview)

            // Create a grid layout for color buttons
            val colorGrid = GridLayout(context)
            colorGrid.columnCount = 5  // 5 columns for more colors
            layout.addView(colorGrid)

            // Available colors with expanded palette
            val allColors = listOf(
                // Basic colors
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN, Color.MAGENTA, Color.GRAY, Color.BLACK,

                // Additional colors
                Color.rgb(255, 165, 0),   // Orange
                Color.rgb(128, 0, 128),   // Purple
                Color.rgb(165, 42, 42),   // Brown
                Color.rgb(0, 128, 0),     // Dark Green
                Color.rgb(255, 192, 203), // Pink
                Color.rgb(255, 215, 0),   // Gold
                Color.rgb(64, 224, 208),  // Turquoise
                Color.rgb(218, 112, 214)  // Orchid
            )

            var selectedColor = allColors.firstOrNull { it !in usedColors } ?: Color.BLACK
            colorPreview.setBackgroundColor(selectedColor)

            // Create color buttons
            for (color in allColors) {
                // Create a FrameLayout to hold both the color button and the 'X' mark
                val buttonContainer = FrameLayout(context)
                val containerParams = LinearLayout.LayoutParams(80, 80)
                containerParams.setMargins(5, 5, 5, 5)
                buttonContainer.layoutParams = containerParams

                // The color button
                val colorButton = Button(context)
                colorButton.setBackgroundColor(color)
                colorButton.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

                // Check if this color is already in use
                val isUsed = usedColors.contains(color)

                // Set enabled state
                colorButton.isEnabled = !isUsed
                colorButton.alpha = if (isUsed) 0.5f else 1.0f

                // Add click listener only if color is available
                if (!isUsed) {
                    colorButton.setOnClickListener {
                        selectedColor = color
                        colorPreview.setBackgroundColor(color)
                    }
                }

                buttonContainer.addView(colorButton)

                // Add an 'X' mark on top of used colors
                if (isUsed) {
                    val crossMark = TextView(context)
                    crossMark.text = "X"
                    crossMark.setTextColor(Color.WHITE)
                    crossMark.textSize = 18f
                    crossMark.gravity = android.view.Gravity.CENTER

                    val crossParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    crossMark.layoutParams = crossParams

                    buttonContainer.addView(crossMark)
                }

                colorGrid.addView(buttonContainer)
            }

            // Error text for color selection
            val colorErrorText = TextView(context)
            colorErrorText.text = "This color is already in use."
            colorErrorText.setTextColor(Color.RED)
            colorErrorText.visibility = View.GONE
            layout.addView(colorErrorText)

            // Create dialog
            val dialog = AlertDialog.Builder(context)
                .setTitle("Add New Move")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val name = nameInput.text.toString().trim()

                    // Check if name is empty
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if name is already used
                    val nameExists = moves.any { it.name.equals(name, ignoreCase = true) }
                    if (nameExists) {
                        Toast.makeText(context, "This name is already taken", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if color is already used
                    if (usedColors.contains(selectedColor)) {
                        Toast.makeText(context, "This color is already in use", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Add new move
                    val newMove = Move(name, selectedColor)
                    moves.add(newMove)
                    usedColors.add(selectedColor)

                    // Update RecyclerView
                    recyclerView.adapter?.notifyDataSetChanged()
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in add dialog", e)
            Toast.makeText(context, "Error creating add dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Show dialog for editing an existing Move
    private fun showEditDialog(parentDialog: AlertDialog, position: Int) {
        try {
            if (position >= moves.size) return

            val moveToEdit = moves[position]
            val recyclerView = parentDialog.findViewById<RecyclerView>(R.id.recyclerView)


            // Create a custom layout
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // Create name input
            val nameLabel = TextView(context)
            nameLabel.text = "Move Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.setText(moveToEdit.name)
            layout.addView(nameInput)

            // Add error text for name
            val nameErrorText = TextView(context)
            nameErrorText.text = "This name is already taken. Please choose another."
            nameErrorText.setTextColor(Color.RED)
            nameErrorText.visibility = View.GONE
            layout.addView(nameErrorText)

            // Add spacing
            val space = Space(context)
            space.minimumHeight = 20
            layout.addView(space)

            // Create color selection
            val colorLabel = TextView(context)
            colorLabel.text = "Select Color:"
            layout.addView(colorLabel)

            // Create color preview
            val colorPreview = View(context)
            colorPreview.setBackgroundColor(moveToEdit.color)
            val previewParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100
            )
            previewParams.setMargins(0, 10, 0, 20)
            colorPreview.layoutParams = previewParams
            layout.addView(colorPreview)

            // Create a grid layout for color buttons
            val colorGrid = GridLayout(context)
            colorGrid.columnCount = 5  // 5 columns for more colors
            layout.addView(colorGrid)

            // Available colors with expanded palette
            val allColors = listOf(
                // Basic colors
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN, Color.MAGENTA, Color.GRAY, Color.BLACK,

                // Additional colors
                Color.rgb(255, 165, 0),   // Orange
                Color.rgb(128, 0, 128),   // Purple
                Color.rgb(165, 42, 42),   // Brown
                Color.rgb(0, 128, 0),     // Dark Green
                Color.rgb(255, 192, 203), // Pink
                Color.rgb(255, 215, 0),   // Gold
                Color.rgb(64, 224, 208),  // Turquoise
                Color.rgb(218, 112, 214)  // Orchid
            )

            var selectedColor = moveToEdit.color

            // Create color buttons
            for (color in allColors) {
                // Create a FrameLayout to hold both the color button and the 'X' mark
                val buttonContainer = FrameLayout(context)
                val containerParams = LinearLayout.LayoutParams(80, 80)
                containerParams.setMargins(5, 5, 5, 5)
                buttonContainer.layoutParams = containerParams

                // The color button
                val colorButton = Button(context)
                colorButton.setBackgroundColor(color)
                colorButton.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

                // Check if this color is already in use (except the current move's color)
                val isUsed = usedColors.contains(color) && color != moveToEdit.color

                // Set enabled state
                colorButton.isEnabled = !isUsed
                colorButton.alpha = if (isUsed) 0.5f else 1.0f

                // Highlight the currently selected color
                if (color == moveToEdit.color) {
                    colorButton.setBackgroundResource(android.R.drawable.btn_default)
                    colorButton.setBackgroundColor(color)
                }

                // Add click listener only if color is available
                if (!isUsed) {
                    colorButton.setOnClickListener {
                        selectedColor = color
                        colorPreview.setBackgroundColor(color)
                    }
                }

                buttonContainer.addView(colorButton)

                // Add an 'X' mark on top of used colors
                if (isUsed) {
                    val crossMark = TextView(context)
                    crossMark.text = "X"
                    crossMark.setTextColor(Color.WHITE)
                    crossMark.textSize = 18f
                    crossMark.gravity = android.view.Gravity.CENTER

                    val crossParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    crossMark.layoutParams = crossParams

                    buttonContainer.addView(crossMark)
                }

                colorGrid.addView(buttonContainer)
            }

            // Create dialog
            val dialog = AlertDialog.Builder(context)
                .setTitle("Edit Move")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val name = nameInput.text.toString().trim()

                    // Check if name is empty
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if name is already used by another move
                    val nameExists = moves.any {
                        it != moveToEdit && it.name.equals(name, ignoreCase = true)
                    }
                    if (nameExists) {
                        Toast.makeText(context, "This name is already taken", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if color is already used by another move
                    if (usedColors.contains(selectedColor) && selectedColor != moveToEdit.color) {
                        Toast.makeText(context, "This color is already in use", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Update move
                    usedColors.remove(moveToEdit.color)
                    moveToEdit.name = name
                    moveToEdit.color = selectedColor
                    usedColors.add(selectedColor)

                    // Update RecyclerView
                    recyclerView?.adapter?.notifyDataSetChanged()
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in edit dialog", e)
            Toast.makeText(context, "Error creating edit dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
