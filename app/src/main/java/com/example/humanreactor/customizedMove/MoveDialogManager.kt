//package com.example.humanreactor.customizedMove
//
//import android.app.AlertDialog
//import android.content.Context
//import android.graphics.Color
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.*
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.humanreactor.R
//
//class MoveDialogManager(
//    private val context: Context,
//    private val moves: MutableList<Move>,
//    private val usedColors: MutableSet<Int>
//) {
//    private val TAG = "MoveDialogManager"
//
//    // Show the main Move management dialog
//    fun showMoveManagementDialog() {
//        try {
//            // Inflate the dialog layout
//            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_manage_moves, null)
//
//
//            // Create dialog
//            val dialog = AlertDialog.Builder(context)
//                .setTitle("Manage Moves")
//                .setView(dialogView)
//                .setCancelable(true)
//                .create()
//
//            // Find views
//            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
//            val btnAddMove = dialogView.findViewById<Button>(R.id.btnAddMove)
//            val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
//
//
//            // Setup RecyclerView with adapter
//            val movesAdapter = MovesAdapter(
//                moves,
//                onEditClick = { position -> showEditDialog(dialog, position) },
//                onDeleteClick = { position ->
//                    if (position < moves.size) {
//                        val moveToDelete = moves[position]
//                        usedColors.remove(moveToDelete.color)
//                        moves.removeAt(position)
//                        recyclerView.adapter?.notifyDataSetChanged()
//                    }
//                }
//            )
//
//            recyclerView.layoutManager = LinearLayoutManager(context)
//            recyclerView.adapter = movesAdapter
//
//            // Setup Add button
//            btnAddMove.setOnClickListener {
//                showAddDialog(dialog, recyclerView)
//            }
//
//            btnClose.setOnClickListener {
//                dialog.dismiss()
//            }
//
//            dialog.setOnShowListener {
//                recyclerView.adapter?.notifyDataSetChanged()
//                recyclerView.post {
//                    recyclerView.requestLayout()
//                }
//            }
//            dialog.show()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error in main dialog", e)
//            Toast.makeText(context, "Error creating dialog: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    // Show dialog for adding a new Move
//    private fun showAddDialog(parentDialog: AlertDialog, recyclerView: RecyclerView) {
//        try {
//            // Create a custom layout
//            val layout = LinearLayout(context)
//            layout.orientation = LinearLayout.VERTICAL
//            layout.setPadding(50, 30, 50, 30)
//
//            // Create name input
//            val nameLabel = TextView(context)
//            nameLabel.text = "Move Name:"
//            layout.addView(nameLabel)
//
//            val nameInput = EditText(context)
//            nameInput.hint = "Enter name"
//            layout.addView(nameInput)
//
//            // Add error text for name
//            val nameErrorText = TextView(context)
//            nameErrorText.text = "This name is already taken. Please choose another."
//            nameErrorText.setTextColor(Color.RED)
//            nameErrorText.visibility = View.GONE
//            layout.addView(nameErrorText)
//
//            // Add spacing
//            val space = Space(context)
//            space.minimumHeight = 20
//            layout.addView(space)
//
//            // Create color selection
//            val colorLabel = TextView(context)
//            colorLabel.text = "Select Color:"
//            layout.addView(colorLabel)
//
//            // Create color preview
//            val colorPreview = View(context)
//            colorPreview.setBackgroundColor(Color.WHITE)
//            val previewParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT, 100
//            )
//            previewParams.setMargins(0, 10, 0, 20)
//            colorPreview.layoutParams = previewParams
//            layout.addView(colorPreview)
//
//            // Create a grid layout for color buttons
//            val colorGrid = GridLayout(context)
//            colorGrid.columnCount = 5  // 5 columns for more colors
//            layout.addView(colorGrid)
//
//            // Available colors with expanded palette
//            val allColors = listOf(
//                // Basic colors
//                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
//                Color.CYAN, Color.MAGENTA, Color.GRAY, Color.BLACK,
//
//                // Additional colors
//                Color.rgb(255, 165, 0),   // Orange
//                Color.rgb(128, 0, 128),   // Purple
//                Color.rgb(165, 42, 42),   // Brown
//                Color.rgb(0, 128, 0),     // Dark Green
//                Color.rgb(255, 192, 203), // Pink
//                Color.rgb(255, 215, 0),   // Gold
//                Color.rgb(64, 224, 208),  // Turquoise
//                Color.rgb(218, 112, 214)  // Orchid
//            )
//
//            var selectedColor = allColors.firstOrNull { it !in usedColors } ?: Color.BLACK
//            colorPreview.setBackgroundColor(selectedColor)
//
//            // Create color buttons
//            for (color in allColors) {
//                // Create a FrameLayout to hold both the color button and the 'X' mark
//                val buttonContainer = FrameLayout(context)
//                val containerParams = LinearLayout.LayoutParams(80, 80)
//                containerParams.setMargins(5, 5, 5, 5)
//                buttonContainer.layoutParams = containerParams
//
//                // The color button
//                val colorButton = Button(context)
//                colorButton.setBackgroundColor(color)
//                colorButton.layoutParams = FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                    FrameLayout.LayoutParams.MATCH_PARENT
//                )
//
//                // Check if this color is already in use
//                val isUsed = usedColors.contains(color)
//
//                // Set enabled state
//                colorButton.isEnabled = !isUsed
//                colorButton.alpha = if (isUsed) 0.5f else 1.0f
//
//                // Add click listener only if color is available
//                if (!isUsed) {
//                    colorButton.setOnClickListener {
//                        selectedColor = color
//                        colorPreview.setBackgroundColor(color)
//                    }
//                }
//
//                buttonContainer.addView(colorButton)
//
//                // Add an 'X' mark on top of used colors
//                if (isUsed) {
//                    val crossMark = TextView(context)
//                    crossMark.text = "X"
//                    crossMark.setTextColor(Color.WHITE)
//                    crossMark.textSize = 18f
//                    crossMark.gravity = android.view.Gravity.CENTER
//
//                    val crossParams = FrameLayout.LayoutParams(
//                        FrameLayout.LayoutParams.MATCH_PARENT,
//                        FrameLayout.LayoutParams.MATCH_PARENT
//                    )
//                    crossMark.layoutParams = crossParams
//
//                    buttonContainer.addView(crossMark)
//                }
//
//                colorGrid.addView(buttonContainer)
//            }
//
//            // Error text for color selection
//            val colorErrorText = TextView(context)
//            colorErrorText.text = "This color is already in use."
//            colorErrorText.setTextColor(Color.RED)
//            colorErrorText.visibility = View.GONE
//            layout.addView(colorErrorText)
//
//            // Create dialog
//            val dialog = AlertDialog.Builder(context)
//                .setTitle("Add New Move")
//                .setView(layout)
//                .setPositiveButton("Save") { _, _ ->
//                    val name = nameInput.text.toString().trim()
//
//                    // Check if name is empty
//                    if (name.isEmpty()) {
//                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
//                        return@setPositiveButton
//                    }
//
//                    // Check if name is already used
//                    val nameExists = moves.any { it.name.equals(name, ignoreCase = true) }
//                    if (nameExists) {
//                        Toast.makeText(context, "This name is already taken", Toast.LENGTH_SHORT).show()
//                        return@setPositiveButton
//                    }
//
//                    // Check if color is already used
//                    if (usedColors.contains(selectedColor)) {
//                        Toast.makeText(context, "This color is already in use", Toast.LENGTH_SHORT).show()
//                        return@setPositiveButton
//                    }
//
//                    // Add new move
//                    val newMove = Move(name, selectedColor)
//                    moves.add(newMove)
//                    usedColors.add(selectedColor)
//
//                    // Update RecyclerView
//                    recyclerView.adapter?.notifyDataSetChanged()
//                }
//                .setNegativeButton("Cancel", null)
//                .create()
//
//            dialog.show()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error in add dialog", e)
//            Toast.makeText(context, "Error creating add dialog: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    // Show dialog for editing an existing Move
//    private fun showEditDialog(parentDialog: AlertDialog, position: Int) {
//        try {
//            if (position >= moves.size) return
//
//            val moveToEdit = moves[position]
//            val recyclerView = parentDialog.findViewById<RecyclerView>(R.id.recyclerView)
//
//
//            // Create a custom layout
//            val layout = LinearLayout(context)
//            layout.orientation = LinearLayout.VERTICAL
//            layout.setPadding(50, 30, 50, 30)
//
//            // Create name input
//            val nameLabel = TextView(context)
//            nameLabel.text = "Move Name:"
//            layout.addView(nameLabel)
//
//            val nameInput = EditText(context)
//            nameInput.setText(moveToEdit.name)
//            layout.addView(nameInput)
//
//            // Add error text for name
//            val nameErrorText = TextView(context)
//            nameErrorText.text = "This name is already taken. Please choose another."
//            nameErrorText.setTextColor(Color.RED)
//            nameErrorText.visibility = View.GONE
//            layout.addView(nameErrorText)
//
//            // Add spacing
//            val space = Space(context)
//            space.minimumHeight = 20
//            layout.addView(space)
//
//            // Create color selection
//            val colorLabel = TextView(context)
//            colorLabel.text = "Select Color:"
//            layout.addView(colorLabel)
//
//            // Create color preview
//            val colorPreview = View(context)
//            colorPreview.setBackgroundColor(moveToEdit.color)
//            val previewParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT, 100
//            )
//            previewParams.setMargins(0, 10, 0, 20)
//            colorPreview.layoutParams = previewParams
//            layout.addView(colorPreview)
//
//            // Create a grid layout for color buttons
//            val colorGrid = GridLayout(context)
//            colorGrid.columnCount = 5  // 5 columns for more colors
//            layout.addView(colorGrid)
//
//            // Available colors with expanded palette
//            val allColors = listOf(
//                // Basic colors
//                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
//                Color.CYAN, Color.MAGENTA, Color.GRAY, Color.BLACK,
//
//                // Additional colors
//                Color.rgb(255, 165, 0),   // Orange
//                Color.rgb(128, 0, 128),   // Purple
//                Color.rgb(165, 42, 42),   // Brown
//                Color.rgb(0, 128, 0),     // Dark Green
//                Color.rgb(255, 192, 203), // Pink
//                Color.rgb(255, 215, 0),   // Gold
//                Color.rgb(64, 224, 208),  // Turquoise
//                Color.rgb(218, 112, 214)  // Orchid
//            )
//
//            var selectedColor = moveToEdit.color
//
//            // Create color buttons
//            for (color in allColors) {
//                // Create a FrameLayout to hold both the color button and the 'X' mark
//                val buttonContainer = FrameLayout(context)
//                val containerParams = LinearLayout.LayoutParams(80, 80)
//                containerParams.setMargins(5, 5, 5, 5)
//                buttonContainer.layoutParams = containerParams
//
//                // The color button
//                val colorButton = Button(context)
//                colorButton.setBackgroundColor(color)
//                colorButton.layoutParams = FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.MATCH_PARENT,
//                    FrameLayout.LayoutParams.MATCH_PARENT
//                )
//
//                // Check if this color is already in use (except the current move's color)
//                val isUsed = usedColors.contains(color) && color != moveToEdit.color
//
//                // Set enabled state
//                colorButton.isEnabled = !isUsed
//                colorButton.alpha = if (isUsed) 0.5f else 1.0f
//
//                // Highlight the currently selected color
//                if (color == moveToEdit.color) {
//                    colorButton.setBackgroundResource(android.R.drawable.btn_default)
//                    colorButton.setBackgroundColor(color)
//                }
//
//                // Add click listener only if color is available
//                if (!isUsed) {
//                    colorButton.setOnClickListener {
//                        selectedColor = color
//                        colorPreview.setBackgroundColor(color)
//                    }
//                }
//
//                buttonContainer.addView(colorButton)
//
//                // Add an 'X' mark on top of used colors
//                if (isUsed) {
//                    val crossMark = TextView(context)
//                    crossMark.text = "X"
//                    crossMark.setTextColor(Color.WHITE)
//                    crossMark.textSize = 18f
//                    crossMark.gravity = android.view.Gravity.CENTER
//
//                    val crossParams = FrameLayout.LayoutParams(
//                        FrameLayout.LayoutParams.MATCH_PARENT,
//                        FrameLayout.LayoutParams.MATCH_PARENT
//                    )
//                    crossMark.layoutParams = crossParams
//
//                    buttonContainer.addView(crossMark)
//                }
//
//                colorGrid.addView(buttonContainer)
//            }
//
//            // Create dialog
//            val dialog = AlertDialog.Builder(context)
//                .setTitle("Edit Move")
//                .setView(layout)
//                .setPositiveButton("Save") { _, _ ->
//                    val name = nameInput.text.toString().trim()
//
//                    // Check if name is empty
//                    if (name.isEmpty()) {
//                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
//                        return@setPositiveButton
//                    }
//
//                    // Check if name is already used by another move
//                    val nameExists = moves.any {
//                        it != moveToEdit && it.name.equals(name, ignoreCase = true)
//                    }
//                    if (nameExists) {
//                        Toast.makeText(context, "This name is already taken", Toast.LENGTH_SHORT).show()
//                        return@setPositiveButton
//                    }
//
//                    // Check if color is already used by another move
//                    if (usedColors.contains(selectedColor) && selectedColor != moveToEdit.color) {
//                        Toast.makeText(context, "This color is already in use", Toast.LENGTH_SHORT).show()
//                        return@setPositiveButton
//                    }
//
//                    // Update move
//                    usedColors.remove(moveToEdit.color)
//                    moveToEdit.name = name
//                    moveToEdit.color = selectedColor
//                    usedColors.add(selectedColor)
//
//                    // Update RecyclerView
//                    recyclerView?.adapter?.notifyDataSetChanged()
//                }
//                .setNegativeButton("Cancel", null)
//                .create()
//
//            dialog.show()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error in edit dialog", e)
//            Toast.makeText(context, "Error creating edit dialog: ${e.message}", Toast.LENGTH_LONG).show()
//        }
//    }
//}

package com.example.humanreactor.customizedMove

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
    private val externalMoves: MutableList<Move>,  // 添加外部列表参数
    private val externalUsedColors: MutableSet<Int>  // 添加外部颜色集合参数
) {
    private val TAG = "MoveDialogManager"

    // 存储类别和动作
    private var categories: List<Category> = emptyList()
    private var actionsByCategory: Map<Int, List<Action>> = emptyMap()
    private var selectedCategoryId: Int = -1
    private var selectedCategoryName: String = ""

    // 显示主对话框
    fun showMoveManagementDialog() {
        try {
            // 加载对话框布局
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_tabbed_manage_moves, null)

            // 创建对话框
            val dialog = AlertDialog.Builder(context)
                .setTitle("Manage Categories and Moves")
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // 查找视图元素
            val tabLayout = dialogView.findViewById<TabLayout>(R.id.tabLayout)
            val categoriesContainer = dialogView.findViewById<FrameLayout>(R.id.categoriesContainer)
            val movesContainer = dialogView.findViewById<FrameLayout>(R.id.movesContainer)
            val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

            // 加载标签页内容
            val categoriesView = LayoutInflater.from(context).inflate(R.layout.tab_categories, null)
            val movesView = LayoutInflater.from(context).inflate(R.layout.tab_category_moves, null)

            categoriesContainer.addView(categoriesView)
            movesContainer.addView(movesView)

            // 设置关闭按钮
            btnClose.setOnClickListener {
                // 关闭对话框前自动加载选中的动作到外部列表
                loadSelectedMovesToExternal()
                dialog.dismiss()
            }

            // 设置类别管理标签页
            setupCategoriesTab(categoriesView, dialog)

            // 设置动作管理标签页
            setupCategoryMovesTab(movesView, dialog)

            // 处理标签页选择
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> {
                            // 切换到类别标签页
                            categoriesContainer.visibility = View.VISIBLE
                            movesContainer.visibility = View.GONE
                            // 刷新类别列表并确保选中状态正确显示
                            loadCategories(categoriesView)
                            categoriesView.findViewById<RecyclerView>(R.id.recyclerViewCategories)?.adapter?.notifyDataSetChanged()
                        }
                        1 -> {
                            // 切换到动作标签页，需要有选中的类别
                            if (selectedCategoryId != -1) {
                                categoriesContainer.visibility = View.GONE
                                movesContainer.visibility = View.VISIBLE

                                // 更新标题显示选中的类别
                                val tvCategoryTitle = movesView.findViewById<TextView>(R.id.tvCategoryTitle)
                                tvCategoryTitle.text = "Moves in: $selectedCategoryName"

                                // 加载所选类别的动作
                                loadCategoryMoves(movesView)
                            } else {
                                // 如果没有选中类别，不允许切换到动作标签页
                                Toast.makeText(context, "Please select a category first", Toast.LENGTH_SHORT).show()
                                // 切回类别标签页
                                tabLayout.getTabAt(0)?.select()
                            }
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            // 显示对话框，默认显示类别标签页
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
            Toast.makeText(context, "Error creating dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 加载选中类别的动作到外部列表
    private fun loadSelectedMovesToExternal() {
        try {
            if (selectedCategoryId == -1) return

            // 获取所选类别的动作
            val actions = actionsByCategory[selectedCategoryId] ?: emptyList()

            // 清空外部列表和颜色集合
            externalMoves.clear()
            externalUsedColors.clear()

            // 将动作转换并添加到外部列表
            actions.forEach { action ->
                val move = Move(
                    name = action.name,
                    color = action.color,
                    dbId = action.id
                    // 保留Move类的其他默认参数
                )
                externalMoves.add(move)
                externalUsedColors.add(action.color)
            }

            // 通知用户
            Toast.makeText(context, "Loaded ${actions.size} moves from '$selectedCategoryName'", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading moves to external list", e)
        }
    }

    // 设置类别管理标签页
    private fun setupCategoriesTab(tabView: View, dialog: AlertDialog) {
        try {
            // 查找视图元素
            val recyclerView = tabView.findViewById<RecyclerView>(R.id.recyclerViewCategories)
            val btnAddCategory = tabView.findViewById<Button>(R.id.btnAddCategory)

            // 设置添加类别按钮
            btnAddCategory.setOnClickListener {
                showAddCategoryDialog(dialog, tabView)
            }

            // 设置RecyclerView布局 - 修改这部分
            val layoutManager = LinearLayoutManager(context)
            recyclerView.layoutManager = layoutManager

            // 添加分隔线
            val dividerItemDecoration = DividerItemDecoration(
                recyclerView.context,
                layoutManager.orientation
            )
            recyclerView.addItemDecoration(dividerItemDecoration)

            // 设置固定大小以提高性能
            recyclerView.setHasFixedSize(true)

            // 加载类别数据
            loadCategories(tabView)

        } catch (e: Exception) {
            Log.e(TAG, "Error in setup categories tab", e)
        }
    }

    // 设置类别动作管理标签页
    private fun setupCategoryMovesTab(tabView: View, dialog: AlertDialog) {
        try {
            // 查找视图元素
            val recyclerView = tabView.findViewById<RecyclerView>(R.id.recyclerViewCategoryMoves)
            val btnAddMove = tabView.findViewById<Button>(R.id.btnAddCategoryMove)
            val tvCategoryTitle = tabView.findViewById<TextView>(R.id.tvCategoryTitle)

            // 设置RecyclerView布局
            recyclerView.layoutManager = LinearLayoutManager(context)

            // 设置添加动作按钮
            btnAddMove.setOnClickListener {
                if (selectedCategoryId != -1) {
                    showAddMoveDialog(dialog, tabView)
                } else {
                    Toast.makeText(context, "Please select a category first", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in setup category moves tab", e)
        }
    }

    // 加载类别数据
    private fun loadCategories(tabView: View) {
        try {
            // 从数据库获取所有类别
            categories = dbHelper.getAllCategories()

            // 为每个类别加载动作
            val actionsMap = mutableMapOf<Int, List<Action>>()
            categories.forEach { category ->
                actionsMap[category.id] = dbHelper.getActionsByCategory(category.id)
            }
            actionsByCategory = actionsMap

            // 设置类别RecyclerView适配器
            val recyclerView = tabView.findViewById<RecyclerView>(R.id.recyclerViewCategories)

            val adapter = CategoryAdapter(
                categories,
                onSelectClick = { category ->
                    // 选择一个类别
                    selectedCategoryId = category.id
                    selectedCategoryName = category.name

                    // 自动将此类别的动作加载到外部列表
                    loadSelectedMovesToExternal()

                    // 通知适配器更新，立即显示选中效果
                    recyclerView.adapter?.notifyDataSetChanged()

                    // 切换到动作标签页
                    val dialog = getAlertDialogFromView(tabView)
                    val tabLayout = dialog?.findViewById<TabLayout>(R.id.tabLayout)
                    tabLayout?.getTabAt(1)?.select()
                },
                onEditClick = { category ->
                    // 编辑类别
                    showEditCategoryDialog(category, tabView)
                },
                onDeleteClick = { category ->
                    // 删除类别
                    showDeleteCategoryConfirmDialog(category, tabView)
                }
            )

            recyclerView.adapter = adapter

            // 如果之前选中了类别但现在已被删除，重置选中状态
            if (selectedCategoryId != -1 && !categories.any { it.id == selectedCategoryId }) {
                selectedCategoryId = -1
                selectedCategoryName = ""
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading categories", e)
        }
    }

    // 加载所选类别的动作
    private fun loadCategoryMoves(tabView: View) {
        try {
            if (selectedCategoryId == -1) return

            // 获取所选类别的动作
            val actions = actionsByCategory[selectedCategoryId] ?: emptyList()

            // 设置动作RecyclerView适配器
            val recyclerView = tabView.findViewById<RecyclerView>(R.id.recyclerViewCategoryMoves)

            val adapter = MovesAdapter(
                actions.map { action ->
                    // 将Action转换为Move对象（用于显示）
                    Move(
                        name = action.name,
                        color = action.color,
                        dbId = action.id,
                        isTrained = false,
                        isCollected = false
                    )
                }.toMutableList(),
                onEditClick = { position, move ->
                    // 编辑动作
                    if (move.dbId != null) {
                        showEditMoveDialog(move, tabView)
                    }
                },
                onDeleteClick = { position, move ->
                    // 删除动作
                    if (move.dbId != null) {
                        showDeleteMoveConfirmDialog(move, tabView)
                    }
                }
            )

            recyclerView.adapter = adapter

        } catch (e: Exception) {
            Log.e(TAG, "Error loading category moves", e)
        }
    }

    // 从视图获取AlertDialog实例
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

    // 显示添加类别对话框
    private fun showAddCategoryDialog(parentDialog: AlertDialog, tabView: View) {
        try {
            // 创建自定义布局
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // 创建名称输入
            val nameLabel = TextView(context)
            nameLabel.text = "Category Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.hint = "Enter category name"
            layout.addView(nameInput)

            // 创建对话框
            val dialog = AlertDialog.Builder(context)
                .setTitle("Add New Category")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val name = nameInput.text.toString().trim()

                    // 检查名称是否为空
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 检查名称是否已被使用
                    val nameExists = categories.any { it.name.equals(name, ignoreCase = true) }
                    if (nameExists) {
                        Toast.makeText(context, "This category name is already taken", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 添加新类别到数据库
                    val categoryId = dbHelper.addCategory(name)
                    if (categoryId > 0) {
                        Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()

                        // 刷新类别列表
                        loadCategories(tabView)

                        // 自动选择新添加的类别
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

    // 显示编辑类别对话框
    private fun showEditCategoryDialog(category: Category, tabView: View) {
        try {
            // 创建自定义布局
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // 创建名称输入
            val nameLabel = TextView(context)
            nameLabel.text = "Category Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.setText(category.name)
            layout.addView(nameInput)

            // 创建对话框
            val dialog = AlertDialog.Builder(context)
                .setTitle("Edit Category")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val name = nameInput.text.toString().trim()

                    // 检查名称是否为空
                    if (name.isEmpty()) {
                        Toast.makeText(context, "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 检查名称是否已被其他类别使用
                    val nameExists = categories.any { it.id != category.id && it.name.equals(name, ignoreCase = true) }
                    if (nameExists) {
                        Toast.makeText(context, "This category name is already taken", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 更新类别
                    val updatedCategory = Category(category.id, name)
                    val rowsAffected = dbHelper.updateCategory(updatedCategory)

                    if (rowsAffected > 0) {
                        Toast.makeText(context, "Category updated successfully", Toast.LENGTH_SHORT).show()

                        // 如果更新的是当前选中的类别，也更新选中的类别名
                        if (category.id == selectedCategoryId) {
                            selectedCategoryName = name
                        }

                        // 刷新类别列表
                        loadCategories(tabView)
                    } else {
                        Toast.makeText(context, "Failed to update category", Toast.LENGTH_SHORT).show()
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

    // 显示删除类别确认对话框
    private fun showDeleteCategoryConfirmDialog(category: Category, tabView: View) {
        try {
            // 检查是否有动作关联到此类别
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
                    // 执行删除操作
                    val success = dbHelper.deleteCategory(category.id)

                    if (success) {
                        Toast.makeText(context, "Category deleted successfully", Toast.LENGTH_SHORT).show()

                        // 如果删除的是当前选中的类别，重置选中状态和外部列表
                        if (category.id == selectedCategoryId) {
                            selectedCategoryId = -1
                            selectedCategoryName = ""
                            externalMoves.clear()
                            externalUsedColors.clear()
                        }

                        // 刷新类别列表
                        loadCategories(tabView)
                    } else {
                        Toast.makeText(context, "Failed to delete category", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()

        } catch (e: Exception) {
            Log.e(TAG, "Error in delete category confirm dialog", e)
        }
    }

    // 显示添加动作对话框
    private fun showAddMoveDialog(parentDialog: AlertDialog, tabView: View) {
        try {
            if (selectedCategoryId == -1) return

            // 创建自定义布局
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // 创建名称输入
            val nameLabel = TextView(context)
            nameLabel.text = "Move Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.hint = "Enter move name"
            layout.addView(nameInput)

            // 添加间距
            val space = Space(context)
            space.minimumHeight = 20
            layout.addView(space)

            // 创建颜色选择
            val colorLabel = TextView(context)
            colorLabel.text = "Select Color:"
            layout.addView(colorLabel)

            // 创建颜色预览
            val colorPreview = View(context)
            colorPreview.setBackgroundColor(Color.WHITE)
            val previewParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100
            )
            previewParams.setMargins(0, 10, 0, 20)
            colorPreview.layoutParams = previewParams
            layout.addView(colorPreview)

            // 创建颜色按钮网格
            val colorGrid = GridLayout(context)
            colorGrid.columnCount = 5
            layout.addView(colorGrid)

            // 可用颜色
            val allColors = listOf(
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN,
//                Color.MAGENTA, Color.GRAY, Color.BLACK,
//                Color.rgb(255, 165, 0), Color.rgb(128, 0, 128),
//                Color.rgb(165, 42, 42), Color.rgb(0, 128, 0),
//                Color.rgb(255, 192, 203), Color.rgb(255, 215, 0),
//                Color.rgb(64, 224, 208), Color.rgb(218, 112, 214)
            )

            // 获取当前类别中已使用的颜色
            val categoryActions = actionsByCategory[selectedCategoryId] ?: emptyList()
            val usedColors = categoryActions.map { it.color }.toSet()

            var selectedColor = allColors.firstOrNull { it !in usedColors } ?: Color.BLACK
            colorPreview.setBackgroundColor(selectedColor)

            // 创建颜色按钮
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

            // 创建对话框
            val dialog = AlertDialog.Builder(context)
                .setTitle("Add New Move")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val name = nameInput.text.toString().trim()

                    if (name.isEmpty()) {
                        Toast.makeText(context, "Move name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 检查此类别中是否已有同名动作
                    val nameExists = categoryActions.any { it.name.equals(name, ignoreCase = true) }
                    if (nameExists) {
                        Toast.makeText(context, "This move name is already taken in this category", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 检查颜色是否已被使用
                    if (usedColors.contains(selectedColor)) {
                        Toast.makeText(context, "This color is already in use in this category", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 创建新动作并添加到数据库
                    val newAction = Action(
                        name = name,
                        color = selectedColor,
                        categoryId = selectedCategoryId
                    )

                    val insertedId = dbHelper.addAction(newAction)
                    if (insertedId > 0) {
                        Toast.makeText(context, "Move added successfully", Toast.LENGTH_SHORT).show()

                        // 重新加载类别数据和动作
                        loadCategories(getTabViewByIndex(parentDialog, 0))
                        loadCategoryMoves(tabView)

                        // 更新外部列表
                        loadSelectedMovesToExternal()
                    } else {
                        Toast.makeText(context, "Failed to add move", Toast.LENGTH_SHORT).show()
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

    // 显示编辑动作对话框
    private fun showEditMoveDialog(move: Move, tabView: View) {
        try {
            if (selectedCategoryId == -1 || move.dbId == null) return

            // 创建自定义布局
            val layout = LinearLayout(context)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(50, 30, 50, 30)

            // 创建名称输入
            val nameLabel = TextView(context)
            nameLabel.text = "Move Name:"
            layout.addView(nameLabel)

            val nameInput = EditText(context)
            nameInput.setText(move.name)
            layout.addView(nameInput)

            // 添加间距
            val space = Space(context)
            space.minimumHeight = 20
            layout.addView(space)

            // 创建颜色选择
            val colorLabel = TextView(context)
            colorLabel.text = "Select Color:"
            layout.addView(colorLabel)

            // 创建颜色预览
            val colorPreview = View(context)
            colorPreview.setBackgroundColor(move.color)
            val previewParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100
            )
            previewParams.setMargins(0, 10, 0, 20)
            colorPreview.layoutParams = previewParams
            layout.addView(colorPreview)

            // 创建颜色按钮网格
            val colorGrid = GridLayout(context)
            colorGrid.columnCount = 5
            layout.addView(colorGrid)

            // 可用颜色
            val allColors = listOf(
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN,
//                Color.MAGENTA, Color.GRAY, Color.BLACK,
//                Color.rgb(255, 165, 0), Color.rgb(128, 0, 128),
//                Color.rgb(165, 42, 42), Color.rgb(0, 128, 0),
//                Color.rgb(255, 192, 203), Color.rgb(255, 215, 0),
//                Color.rgb(64, 224, 208), Color.rgb(218, 112, 214)
            )

            // 获取当前类别中已使用的颜色
            val categoryActions = actionsByCategory[selectedCategoryId] ?: emptyList()
            val usedColors = categoryActions.map { it.color }.toSet()

            var selectedColor = move.color

            // 创建颜色按钮
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

                // 检查颜色是否已被其他动作使用
                val isUsed = usedColors.contains(color) && color != move.color

                colorButton.isEnabled = !isUsed
                colorButton.alpha = if (isUsed) 0.5f else 1.0f

                // 高亮当前选中的颜色
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

            // 创建对话框
            val dialog = AlertDialog.Builder(context)
                .setTitle("Edit Move")
                .setView(layout)
                .setPositiveButton("Save") { _, _ ->
                    val name = nameInput.text.toString().trim()

                    if (name.isEmpty()) {
                        Toast.makeText(context, "Move name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 检查此类别中是否已有同名动作(除了当前编辑的动作)
                    val nameExists = categoryActions.any {
                        it.id != move.dbId && it.name.equals(name, ignoreCase = true)
                    }
                    if (nameExists) {
                        Toast.makeText(context, "This move name is already taken in this category", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 检查颜色是否已被其他动作使用
                    if (usedColors.contains(selectedColor) && selectedColor != move.color) {
                        Toast.makeText(context, "This color is already in use in this category", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    // 更新数据库中的动作
                    val updatedAction = Action(
                        id = move.dbId!!,
                        name = name,
                        color = selectedColor,
                        categoryId = selectedCategoryId
                    )

                    val rowsAffected = dbHelper.updateAction(updatedAction)
                    if (rowsAffected > 0) {
                        Toast.makeText(context, "Move updated successfully", Toast.LENGTH_SHORT).show()

                        // 重新加载类别数据和动作
                        val parentDialog = getAlertDialogFromView(tabView)
                        if (parentDialog != null) {
                            loadCategories(getTabViewByIndex(parentDialog, 0))
                            loadCategoryMoves(tabView)

                            // 更新外部列表
                            loadSelectedMovesToExternal()
                        }
                    } else {
                        Toast.makeText(context, "Failed to update move", Toast.LENGTH_SHORT).show()
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

    // 显示删除动作确认对话框

    private fun showDeleteMoveConfirmDialog(move: Move, tabView: View) {
        try {
            if (move.dbId == null) return

            AlertDialog.Builder(context)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this move?")
                .setPositiveButton("Delete") { _, _ ->
                    // 执行删除操作
                    val success = dbHelper.deleteAction(move.dbId!!)

                    if (success) {
                        Toast.makeText(context, "Move deleted successfully", Toast.LENGTH_SHORT).show()

                        // 直接刷新UI，不需要完全重新加载
                        val recyclerView = tabView.findViewById<RecyclerView>(R.id.recyclerViewCategoryMoves)

                        // 获取当前的数据集和适配器
                        val adapter = recyclerView.adapter as? MovesAdapter
                        if (adapter != null) {
                            // 找到被删除的项目在当前列表中的位置
                            val position = adapter.getPositionById(move.dbId!!)
                            if (position >= 0) {
                                // 从适配器的数据集中移除该项
                                adapter.removeItem(position)
                                // 通知适配器项目被移除
                                adapter.notifyItemRemoved(position)
                            }
                        }

                        // 更新外部列表
                        loadSelectedMovesToExternal()

                        // 同时更新类别数据（可能会影响类别页面的动作计数）
                        val parentDialog = getAlertDialogFromView(tabView)
                        if (parentDialog != null) {
                            loadCategories(getTabViewByIndex(parentDialog, 0))
                        }
                    } else {
                        Toast.makeText(context, "Failed to delete move", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
                .show()

        } catch (e: Exception) {
            Log.e(TAG, "Error in delete move confirm dialog", e)
        }
    }

    // 根据索引获取标签页视图
    private fun getTabViewByIndex(dialog: AlertDialog, index: Int): View {
        return when (index) {
            0 -> dialog.findViewById<FrameLayout>(R.id.categoriesContainer).getChildAt(0)
            1 -> dialog.findViewById<FrameLayout>(R.id.movesContainer).getChildAt(0)
            else -> throw IndexOutOfBoundsException("Invalid tab index")
        }
    }

    // 修改后的CategoryAdapter实现
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

            // 设置类别名称
            holder.tvCategoryName.text = category.name

            // 设置动作数量
            val moveCount = actionsByCategory[category.id]?.size ?: 0
            holder.moveCount.text = "$moveCount moves"

            // 使用不同的背景资源来区分选中项
            if (category.id == selectedCategoryId) {
                holder.itemView.setBackgroundResource(R.drawable.selected_category_item_background)
            } else {
                holder.itemView.setBackgroundResource(R.drawable.category_item_background)
            }

            // 设置按钮点击事件
            holder.btnSelect.setOnClickListener {
                // 记录旧的选中位置
                val oldSelectedPosition = categories.indexOfFirst { it.id == selectedCategoryId }

                // 更新选中状态
                selectedCategoryId = category.id
                selectedCategoryName = category.name

                // 自动加载到外部列表
                loadSelectedMovesToExternal()

                // 通知适配器更新特定项，而不是整个列表
                if (oldSelectedPosition >= 0) {
                    notifyItemChanged(oldSelectedPosition)
                }
                notifyItemChanged(position)

                // 切换到动作标签页
                onSelectClick(category)
            }

            holder.btnEdit.setOnClickListener { onEditClick(category) }
            holder.btnDelete.setOnClickListener { onDeleteClick(category) }
        }

        override fun getItemCount(): Int = categories.size

        // 添加此方法，防止视图重用导致的问题
        override fun getItemViewType(position: Int): Int {
            return position
        }
    }
}

