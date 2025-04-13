package com.example.humanreactor.customizedMove

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.humanreactor.R
import com.example.humanreactor.databases.Action
import com.example.humanreactor.databases.ActionDatabaseHelper
import com.example.humanreactor.databases.Category
import com.google.android.material.tabs.TabLayout

class MoveDialogManager(
    private val context: Context,
    private val dbHelper: ActionDatabaseHelper,
    private val externalMoves: MutableList<Move>,  // Add external list parameter
    private val externalUsedColors: MutableSet<Int>,  // Add external color set parameter
    private var externalCategoryId: MutableSet<Int>
//    private val externalCatelog: String
) {
    private val TAG = "MoveDialogManager"

    // Store categories and actions
    private var categories: List<Category> = emptyList()
    private var actionsByCategory: Map<Int, List<Action>> = emptyMap()
    var selectedCategoryId: Int = -1
    private var selectedCategoryName: String = ""

    // Show the main dialog
    @SuppressLint("NotifyDataSetChanged")
    fun showMoveManagementDialog() {
        try {
            // Load dialog layout
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tabbed_manage_moves, null)

            // Create dialog
            val dialog = AlertDialog.Builder(context)
                .setTitle("Manage Categories and Moves")
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.window?.setBackgroundDrawableResource(R.drawable.big_frame_wbg)

            // Find view elements
            val tabLayout = dialogView.findViewById<TabLayout>(R.id.tabLayout)
            val categoriesContainer = dialogView.findViewById<FrameLayout>(R.id.categoriesContainer)
            val movesContainer = dialogView.findViewById<FrameLayout>(R.id.movesContainer)
            val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

            // Load tab content
            val categoriesView = LayoutInflater.from(context).inflate(R.layout.tab_categories, null)
            val movesView = LayoutInflater.from(context).inflate(R.layout.tab_category_moves, null)

            categoriesContainer.addView(categoriesView)
            movesContainer.addView(movesView)

            // Set close button
            btnClose.setOnClickListener {
                // Load selected actions to external list before closing dialog
                loadSelectedMovesToExternal()
                dialog.dismiss()
            }

            // Set up category management tab
            setupCategoriesTab(categoriesView, dialog)

            // Set up action management tab
            setupCategoryMovesTab(movesView, dialog)

            // Handle tab selection
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> {
                            // Switch to categories tab
                            categoriesContainer.visibility = View.VISIBLE
                            movesContainer.visibility = View.GONE
                            // Refresh category list and ensure selection status is displayed correctly
                            loadCategories(categoriesView)
                            categoriesView.findViewById<RecyclerView>(R.id.recyclerViewCategories)?.adapter?.notifyDataSetChanged()
                        }
                        1 -> {
                            // Switch to actions tab, a category must be selected
                            if (selectedCategoryId != -1) {
                                categoriesContainer.visibility = View.GONE
                                movesContainer.visibility = View.VISIBLE

                                // Update title to show selected category
                                val tvCategoryTitle = movesView.findViewById<TextView>(R.id.tvCategoryTitle)
                                tvCategoryTitle.text = "Moves in: $selectedCategoryName"

                                // Load actions of selected category
                                loadCategoryMoves(movesView)
                            } else {
                                // If no category is selected, don't allow switching to actions tab
                                Toast.makeText(context, "Please select a category first", Toast.LENGTH_SHORT).show()
                                // Switch back to categories tab
                                tabLayout.getTabAt(0)?.select()
                            }
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            // Show dialog, display categories tab by default
            categoriesContainer.visibility = View.VISIBLE
            movesContainer.visibility = View.GONE
            loadCategories(categoriesView)
            dialog.show()

            Handler(Looper.getMainLooper()).postDelayed({
                val recyclerView = categoriesView.findViewById<RecyclerView>(R.id.recyclerViewCategories)
                recyclerView?.adapter?.notifyDataSetChanged()
                recyclerView?.requestLayout()
            }, 100)

        } catch (e: Exception) {
            Log.e(TAG, "Error in main dialog", e)
//            Toast.makeText(context, "Error creating dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Load selected category's actions to external list
    private fun loadSelectedMovesToExternal() {
        try {
            if (selectedCategoryId == -1) return

            // Get actions of selected category
            val actions = actionsByCategory[selectedCategoryId] ?: emptyList()

            // Clear external list and color set
            externalMoves.clear()
            externalUsedColors.clear()

            // Convert actions and add to external list
            actions.forEach { action ->
                val move = Move(
                    name = action.name,
                    color = action.color,
                    dbId = action.id
                    // Keep other default parameters of Move class
                )
                externalMoves.add(move)
                externalUsedColors.add(action.color)
            }

            // Notify user
//            Toast.makeText(context, "Loaded ${actions.size} moves from '$selectedCategoryName'", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading moves to external list", e)
        }
    }

    // Set up category management tab
    private fun setupCategoriesTab(tabView: View, dialog: AlertDialog) {
        try {
            // Find view elements
            val recyclerView = tabView.findViewById<RecyclerView>(R.id.recyclerViewCategories)
            val btnAddCategory = tabView.findViewById<Button>(R.id.btnAddCategory)

            // Set add category button
            btnAddCategory.setOnClickListener {
                showAddCategoryDialog(dialog, tabView)
            }

            // Set RecyclerView layout - modify this part
            val layoutManager = LinearLayoutManager(context)
            recyclerView.layoutManager = layoutManager

            // Add divider line
            val dividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                layoutManager.orientation
            )
            recyclerView.addItemDecoration(dividerItemDecoration)

            // Set fixed size to improve performance
            recyclerView.setHasFixedSize(true)

            // Load category data
            loadCategories(tabView)

        } catch (e: Exception) {
//            Log.e(TAG, "Error in setup categories tab", e)
        }
    }

    // Set up category actions management tab
    private fun setupCategoryMovesTab(tabView: View, dialog: AlertDialog) {
        try {
            // Find view elements
            val recyclerView = tabView.findViewById<RecyclerView>(R.id.recyclerViewCategoryMoves)
            val btnAddMove = tabView.findViewById<Button>(R.id.btnAddCategoryMove)
            val tvCategoryTitle = tabView.findViewById<TextView>(R.id.tvCategoryTitle)

            // Set RecyclerView layout
            recyclerView.layoutManager = LinearLayoutManager(context)

            // Set add action button
            btnAddMove.setOnClickListener {
                if (selectedCategoryId != -1) {
                    showAddMoveDialog(dialog, tabView)
                } else {
//                    Toast.makeText(context, "Please select a category first", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
//            Log.e(TAG, "Error in setup category moves tab", e)
        }
    }

    // Load category data
    private fun loadCategories(tabView: View) {
        try {
            // Get all categories from database
            categories = dbHelper.getAllCategories()

            if (categories.isNotEmpty()) {
                if (selectedCategoryId == -1 || !categories.any { it.id == selectedCategoryId }) {
                    selectedCategoryId = categories.first().id
                    selectedCategoryName = categories.first().name

//                    loadSelectedMovesToExternal()
                    syncExternalCollections()
                }
            } else {
                selectedCategoryId = -1
                selectedCategoryName = ""
            }

            // Load actions for each category
            val actionsMap = mutableMapOf<Int, List<Action>>()
            categories.forEach { category ->
                actionsMap[category.id] = dbHelper.getActionsByCategory(category.id)
            }
            actionsByCategory = actionsMap

            // Set category RecyclerView adapter
            val recyclerView = tabView.findViewById<RecyclerView>(R.id.recyclerViewCategories)

            val adapter = CategoryAdapter(
                categories,
                onSelectClick = { category ->
                    // Select a category
                    selectedCategoryId = category.id
                    selectedCategoryName = category.name

                    // Automatically load this category's actions to external list
                    loadSelectedMovesToExternal()

                    // Notify adapter to update, immediately show selection effect
                    recyclerView.adapter?.notifyDataSetChanged()

                    // Switch to actions tab
                    val dialog = getAlertDialogFromView(tabView)
                    val tabLayout = dialog?.findViewById<TabLayout>(R.id.tabLayout)
                    tabLayout?.getTabAt(1)?.select()
                },
                onEditClick = { category ->
                    // Edit category
                    showEditCategoryDialog(category, tabView)
                },
                onDeleteClick = { category ->
                    // Delete category
                    showDeleteCategoryConfirmDialog(category, tabView)
                }
            )

            recyclerView.adapter = adapter

            // If a category was previously selected but has now been deleted, reset selection state
            if (selectedCategoryId != -1 && !categories.any { it.id == selectedCategoryId }) {
                selectedCategoryId = -1
                selectedCategoryName = ""
            }

        } catch (e: Exception) {
//            Log.e(TAG, "Error loading categories", e)
        }
    }

    // Load actions of selected category
    private fun loadCategoryMoves(tabView: View) {
        try {
            if (selectedCategoryId == -1) return

            Log.d("DEBUG_LOAD", "Loading moves for category: $selectedCategoryId")

            // 获取所选类别的动作
            val actions = actionsByCategory[selectedCategoryId] ?: emptyList()
            Log.d("DEBUG_LOAD", "Found ${actions.size} actions in database")

            // 设置动作RecyclerView适配器
            val recyclerView = tabView.findViewById<RecyclerView>(R.id.recyclerViewCategoryMoves)

            // 将actions转换为moves
            val categoryMoves = actions.map { action ->
                Move(
                    name = action.name,
                    color = action.color,
                    dbId = action.id,
                    isTrained = false,
                    isCollected = false
                )
            }.toMutableList()

            // 使用全新的适配器实例，避免可能的状态问题
            val adapter = MovesAdapter(
                categoryMoves,
                onEditClick = { position, move ->
                    if (move.dbId != null) {
                        showEditMoveDialog(move, tabView)
                    }
                },
                onDeleteClick = { position, move ->
                    if (move.dbId != null) {
                        showDeleteMoveConfirmDialog(move, tabView)
                    }
                }
            )

            // 设置新的适配器
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(context)

            // 延迟一帧后再次刷新，确保完全渲染
            recyclerView.post {
                adapter.notifyDataSetChanged()
                recyclerView.invalidate()
            }

            Log.d("DEBUG_LOAD", "Moves loaded successfully with ${categoryMoves.size} items")

        } catch (e: Exception) {
            Log.e("DEBUG_LOAD", "Error loading category moves", e)
            e.printStackTrace()
        }
    }

    // Get AlertDialog instance from view
    private fun getAlertDialogFromView(view: View): AlertDialog? {
        var parent = view.parent
        while (parent != null) {
            if (parent is AlertDialog) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    // Show add category dialog
    private fun showAddCategoryDialog(parentDialog: AlertDialog, tabView: View) {
        try {
            // Create custom layout
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // Create name input
            val nameLabel = TextView(context)
            nameLabel.text = "Category Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.hint = "Enter category name"
            layout.addView(nameInput)

            // Create dialog
            val dialog = AlertDialog.Builder(context)
                .setTitle("Add New Category")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val name = nameInput.text.toString().trim()

                    // Check if name is empty
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if name is already used
                    val nameExists = categories.any { it.name.equals(name, ignoreCase = true) }
                    if (nameExists) {
                        Toast.makeText(context, "This category name is already taken", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Add new category to database
                    val categoryId = dbHelper.addCategory(name)
                    if (categoryId > 0) {
                        Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()

                        // Refresh category list
                        loadCategories(tabView)

                        // Automatically select new category
                        selectedCategoryId = categoryId.toInt()
                        selectedCategoryName = name
                    } else {
                        Toast.makeText(context, "Failed to add category", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in add category dialog", e)
            Toast.makeText(context, "Error creating dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Show edit category dialog
    private fun showEditCategoryDialog(category: Category, tabView: View) {
        try {
            // Create custom layout
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // Create name input
            val nameLabel = TextView(context)
            nameLabel.text = "Category Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.setText(category.name)
            layout.addView(nameInput)

            // Create dialog
            val dialog = AlertDialog.Builder(context)
                .setTitle("Edit Category")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val name = nameInput.text.toString().trim()

                    // Check if name is empty
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if name is already used by other categories
                    val nameExists = categories.any { it.id != category.id && it.name.equals(name, ignoreCase = true) }
                    if (nameExists) {
                        Toast.makeText(context, "This category name is already taken", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Update category
                    val updatedCategory = Category(category.id, name)
                    val rowsAffected = dbHelper.updateCategory(updatedCategory)

                    if (rowsAffected > 0) {
//                        Toast.makeText(context, "Category updated successfully", Toast.LENGTH_SHORT).show()

                        // If updating the currently selected category, also update selected category name
                        if (category.id == selectedCategoryId) {
                            selectedCategoryName = name
                        }

                        // Refresh category list
                        loadCategories(tabView)
                    } else {
//                        Toast.makeText(context, "Failed to update category", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in edit category dialog", e)
            Toast.makeText(context, "Error creating dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Show delete category confirmation dialog
    private fun showDeleteCategoryConfirmDialog(category: Category, tabView: View) {
        try {
            // Check if there are actions associated with this category
            val hasActions = (actionsByCategory[category.id]?.isNotEmpty()) ?: false

            val message = if (hasActions) {
                "This category contains moves. Deleting it will also delete all associated moves. Are you sure?"
            } else {
                "Are you sure you want to delete this category?"
            }

            AlertDialog.Builder(context)
                .setTitle("Confirm Delete")
                .setMessage(message)
                .setPositiveButton("Delete") { _, _ ->
                    // Perform delete operation
                    val success = dbHelper.deleteCategory(category.id)

                    if (success) {
//                        Toast.makeText(context, "Category deleted successfully", Toast.LENGTH_SHORT).show()

                        // If deleting the currently selected category, reset selection state and external list
                        if (category.id == selectedCategoryId) {
                            selectedCategoryId = -1
                            selectedCategoryName = ""
                            externalMoves.clear()
                            externalUsedColors.clear()
                        }

                        // Refresh category list
                        loadCategories(tabView)
                    } else {
//                        Toast.makeText(context, "Failed to delete category", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()

        } catch (e: Exception) {
            Log.e(TAG, "Error in delete category confirm dialog", e)
        }
    }

    // Show add action dialog
    private fun showAddMoveDialog(parentDialog: AlertDialog, tabView: View) {
        try {
            if (selectedCategoryId == -1) return

            // Create custom layout
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // Create name input
            val nameLabel = TextView(context)
            nameLabel.text = "Move Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.hint = "Enter move name"
            layout.addView(nameInput)

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

            // Create color button grid
            val colorGrid = GridLayout(context)
            colorGrid.columnCount = 5
            layout.addView(colorGrid)

            // Available colors
            val allColors = listOf(
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN,
//                Color.MAGENTA, Color.GRAY, Color.BLACK,
//                Color.rgb(255, 165, 0), Color.rgb(128, 0, 128),
//                Color.rgb(165, 42, 42), Color.rgb(0, 128, 0),
//                Color.rgb(255, 192, 203), Color.rgb(255, 215, 0),
//                Color.rgb(64, 224, 208), Color.rgb(218, 112, 214)
            )

            // Get colors already used in current category
            val categoryActions = actionsByCategory[selectedCategoryId] ?: emptyList()
            val usedColors = categoryActions.map { it.color }.toSet()

            var selectedColor = allColors.firstOrNull { it !in usedColors } ?: Color.BLACK
            colorPreview.setBackgroundColor(selectedColor)

            // Create color buttons
            for (color in allColors) {
                val buttonContainer = FrameLayout(context)
                val containerParams = LinearLayout.LayoutParams(80, 80)
                containerParams.setMargins(5, 5, 5, 5)
                buttonContainer.layoutParams = containerParams

                val colorButton = Button(context)
                colorButton.setBackgroundColor(color)
                colorButton.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

                val isUsed = usedColors.contains(color)
                colorButton.isEnabled = !isUsed
                colorButton.alpha = if (isUsed) 0.5f else 1.0f

                if (!isUsed) {
                    colorButton.setOnClickListener {
                        selectedColor = color
                        colorPreview.setBackgroundColor(color)
                    }
                }

                buttonContainer.addView(colorButton)

                if (isUsed) {
                    val crossMark = TextView(context)
                    crossMark.text = "X"
                    crossMark.setTextColor(Color.WHITE)
                    crossMark.textSize = 18f
                    crossMark.gravity = android.view.Gravity.CENTER
                    buttonContainer.addView(crossMark)
                }

                colorGrid.addView(buttonContainer)
            }

            // Create dialog
            val dialog = AlertDialog.Builder(context)
                .setTitle("Add New Move")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val name = nameInput.text.toString().trim()

                    if (name.isEmpty()) {
                        Toast.makeText(context, "Move name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if there is already an action with the same name in this category
                    val nameExists = categoryActions.any { it.name.equals(name, ignoreCase = true) }
                    if (nameExists) {
                        Toast.makeText(context, "This move name is already taken in this category", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if color is already used
                    if (usedColors.contains(selectedColor)) {
                        Toast.makeText(context, "This color is already in use in this category", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Create new action and add to database
                    val newAction = Action(
                        name = name,
                        color = selectedColor,
                        categoryId = selectedCategoryId
                    )

                    val insertedId = dbHelper.addAction(newAction)
                    if (insertedId > 0) {
//                        Toast.makeText(context, "Move added successfully", Toast.LENGTH_SHORT).show()

                        // Reload category data and actions
                        loadCategories(getTabViewByIndex(parentDialog, 0))
                        loadCategoryMoves(tabView)

                        // Update external list
                        loadSelectedMovesToExternal()
                    } else {
//                        Toast.makeText(context, "Failed to add move", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in add move dialog", e)
            Toast.makeText(context, "Error creating dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Show edit action dialog
    private fun showEditMoveDialog(move: Move, tabView: View) {
        try {
            if (selectedCategoryId == -1 || move.dbId == null) return

            // Create custom layout
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // Create name input
            val nameLabel = TextView(context)
            nameLabel.text = "Move Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.setText(move.name)
            layout.addView(nameInput)

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
            colorPreview.setBackgroundColor(move.color)
            val previewParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100
            )
            previewParams.setMargins(0, 10, 0, 20)
            colorPreview.layoutParams = previewParams
            layout.addView(colorPreview)

            // Create color button grid
            val colorGrid = GridLayout(context)
            colorGrid.columnCount = 5
            layout.addView(colorGrid)

            // Available colors
            val allColors = listOf(
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN,
//                Color.MAGENTA, Color.GRAY, Color.BLACK,
//                Color.rgb(255, 165, 0), Color.rgb(128, 0, 128),
//                Color.rgb(165, 42, 42), Color.rgb(0, 128, 0),
//                Color.rgb(255, 192, 203), Color.rgb(255, 215, 0),
//                Color.rgb(64, 224, 208), Color.rgb(218, 112, 214)
            )

            // Get colors already used in current category
            val categoryActions = actionsByCategory[selectedCategoryId] ?: emptyList()
            val usedColors = categoryActions.map { it.color }.toSet()

            var selectedColor = move.color

            // Create color buttons
            for (color in allColors) {
                val buttonContainer = FrameLayout(context)
                val containerParams = LinearLayout.LayoutParams(80, 80)
                containerParams.setMargins(5, 5, 5, 5)
                buttonContainer.layoutParams = containerParams

                val colorButton = Button(context)
                colorButton.setBackgroundColor(color)
                colorButton.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

                // Check if color is already used by other actions
                val isUsed = usedColors.contains(color) && color != move.color

                colorButton.isEnabled = !isUsed
                colorButton.alpha = if (isUsed) 0.5f else 1.0f

                // Highlight currently selected color
                if (color == move.color) {
                    colorButton.setBackgroundResource(android.R.drawable.btn_default)
                    colorButton.setBackgroundColor(color)
                }

                if (!isUsed) {
                    colorButton.setOnClickListener {
                        selectedColor = color
                        colorPreview.setBackgroundColor(color)
                    }
                }

                buttonContainer.addView(colorButton)

                if (isUsed) {
                    val crossMark = TextView(context)
                    crossMark.text = "X"
                    crossMark.setTextColor(Color.WHITE)
                    crossMark.textSize = 18f
                    crossMark.gravity = android.view.Gravity.CENTER
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

                    if (name.isEmpty()) {
                        Toast.makeText(context, "Move name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if there is already an action with the same name in this category (except the current action being edited)
                    val nameExists = categoryActions.any {
                        it.id != move.dbId && it.name.equals(name, ignoreCase = true)
                    }
                    if (nameExists) {
                        Toast.makeText(context, "This move name is already taken in this category", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Check if color is already used by other actions
                    if (usedColors.contains(selectedColor) && selectedColor != move.color) {
                        Toast.makeText(context, "This color is already in use in this category", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // Update action in database
                    val updatedAction = Action(
                        id = move.dbId!!,
                        name = name,
                        color = selectedColor,
                        categoryId = selectedCategoryId
                    )

                    val rowsAffected = dbHelper.updateAction(updatedAction)
                    if (rowsAffected > 0) {

                        Log.d("DEBUG_EDIT", "Database deletion successful")

                        categories = dbHelper.getAllCategories()

                        val actionsMap = mutableMapOf<Int, List<Action>>()
                        categories.forEach { category ->
                            actionsMap[category.id] = dbHelper.getActionsByCategory(category.id)
                        }
                        actionsByCategory = actionsMap

                        loadCategoryMoves(tabView)
                        syncExternalCollections()

                        Log.d("DEBUG_EDIT", "After sync - External moves: ${externalMoves.size}")

                        Toast.makeText(context, "Move deleted successfully", Toast.LENGTH_SHORT).show()

                        val parentDialog = getAlertDialogFromView(tabView)
                        if (parentDialog != null) {
                            loadCategories(getTabViewByIndex(parentDialog, 0))
                            loadCategoryMoves(tabView)
                        }
                    } else {
//                        Toast.makeText(context, "Failed to update move", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in edit move dialog", e)
            Toast.makeText(context, "Error creating dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Show delete action confirmation dialog
    private fun showDeleteMoveConfirmDialog(move: Move, tabView: View) {
        try {
            if (move.dbId == null) return

            // 记录删除前的状态
            Log.d("DEBUG_DELETE", "Before delete - External moves: ${externalMoves.size}, Colors: ${externalUsedColors.size}")
            Log.d("DEBUG_DELETE", "Trying to delete move with dbId: ${move.dbId}, name: ${move.name}")

            AlertDialog.Builder(context)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this move?")
                .setPositiveButton("Delete") { _, _ ->
                    // 执行删除操作
                    val success = dbHelper.deleteAction(move.dbId!!)

                    if (success) {
                        Log.d("DEBUG_DELETE", "Database deletion successful")

                        categories = dbHelper.getAllCategories()

                        val actionsMap = mutableMapOf<Int, List<Action>>()
                        categories.forEach { category ->
                            actionsMap[category.id] = dbHelper.getActionsByCategory(category.id)
                        }
                        actionsByCategory = actionsMap

                        loadCategoryMoves(tabView)
                        syncExternalCollections()

                        Log.d("DEBUG_DELETE", "After sync - External moves: ${externalMoves.size}")


                        Toast.makeText(context, "Move deleted successfully", Toast.LENGTH_SHORT).show()


                        val parentDialog = getAlertDialogFromView(tabView)
                        if (parentDialog != null) {

                            loadCategories(getTabViewByIndex(parentDialog, 0))

                            loadCategoryMoves(tabView)
                        }
                    } else {
                        Toast.makeText(context, "Failed to delete move", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()

        } catch (e: Exception) {
            Log.e("DEBUG_DELETE", "Error in delete move confirm dialog", e)
        }
    }

    private fun syncExternalCollections() {
        if (selectedCategoryId == -1) return

        Log.d("DEBUG_SYNC", "Syncing external collections for category: $selectedCategoryId")


        val actions = actionsByCategory[selectedCategoryId] ?: emptyList()


        externalMoves.clear()
        externalUsedColors.clear()
        externalCategoryId.clear()
        externalCategoryId.add(selectedCategoryId)
        actions.forEach { action ->
            val move = Move(
                name = action.name,
                color = action.color,
                dbId = action.id,
                // 其他属性使用默认值
                isTrained = false,
                isCollected = false
            )
            externalMoves.add(move)
            externalUsedColors.add(action.color)
        }

        Log.d("DEBUG_SYNC", "Sync complete - External moves: ${externalMoves.size}")
    }


    // Get tab view by index
    private fun getTabViewByIndex(dialog: AlertDialog, index: Int): View {
        return when (index) {
            0 -> dialog.findViewById<FrameLayout>(R.id.categoriesContainer).getChildAt(0)
            1 -> dialog.findViewById<FrameLayout>(R.id.movesContainer).getChildAt(0)
            else -> throw IndexOutOfBoundsException("Invalid tab index")
        }
    }

    // Modified CategoryAdapter implementation
    inner class CategoryAdapter(
        private val categories: List<Category>,
        private val onSelectClick: (Category) -> Unit,
        private val onEditClick: (Category) -> Unit,
        private val onDeleteClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
            val btnSelect: Button = itemView.findViewById(R.id.btnSelect)
            val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
            val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
            val moveCount: TextView = itemView.findViewById(R.id.tvMoveCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]

            // Set category name
            holder.tvCategoryName.text = category.name

            // Set move count
            val moveCount = actionsByCategory[category.id]?.size ?: 0
            holder.moveCount.text = "$moveCount moves"

            // Use different background resource to distinguish selected item
            if (category.id == selectedCategoryId) {
                holder.itemView.setBackgroundResource(R.drawable.selected_category_item_background)
            } else {
                holder.itemView.setBackgroundResource(R.drawable.category_item_background)
            }

            // Set button click events
            holder.btnSelect.setOnClickListener {
                // Record old selected position
                val oldSelectedPosition = categories.indexOfFirst { it.id == selectedCategoryId }

                // Update selection state
                selectedCategoryId = category.id
                selectedCategoryName = category.name

                // Automatically load to external list
                loadSelectedMovesToExternal()

                // Notify adapter to update specific items, not the entire list
                if (oldSelectedPosition >= 0) {
                    notifyItemChanged(oldSelectedPosition)
                }
                notifyItemChanged(position)

                // Switch to moves tab
                onSelectClick(category)
                syncExternalCollections()
            }

            holder.btnEdit.setOnClickListener { onEditClick(category) }
            holder.btnDelete.setOnClickListener { onDeleteClick(category) }
        }

        override fun getItemCount(): Int = categories.size

        // Add this method to prevent view reuse issues
        override fun getItemViewType(position: Int): Int {
            return position
        }
    }
}