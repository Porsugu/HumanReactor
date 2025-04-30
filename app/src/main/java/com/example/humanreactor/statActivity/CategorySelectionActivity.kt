//package com.example.humanreactor.statActivity
//
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.CheckBox
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.humanreactor.R
//import com.example.humanreactor.databases.ActionDatabaseHelper
//import com.example.humanreactor.databases.Category
//
//class CategorySelectionActivity : AppCompatActivity() {
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var btnConfirm: Button
//    private lateinit var dbHelper: ActionDatabaseHelper
//    private var selectedCategory: Category? = null
//    private var type: String = "action"  // Default to action
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_category_selection)
//
//        // Get type from intent
//        type = intent.getStringExtra("type") ?: "action"
//
//        // Initialize views
//        recyclerView = findViewById(R.id.recycler_categories)
//        btnConfirm = findViewById(R.id.btn_confirm)
//
//        // Initialize database helper
//        dbHelper = ActionDatabaseHelper(this)
//
//        // Setup RecyclerView
//        recyclerView.layoutManager = LinearLayoutManager(this)
//
//        // If this is for actions, load categories from database
//        if (type == "action") {
//            loadCategories()
//        } else {
//            // For now, we're ignoring the mental flow as per requirements
//            Toast.makeText(this, "Mental stats not implemented yet", Toast.LENGTH_SHORT).show()
//            finish()
//        }
//
//        // Setup confirm button
//        btnConfirm.setOnClickListener {
//            if (selectedCategory == null) {
//                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
//            } else {
//                // Navigate to performance detail activity with the selected category
//                val intent = Intent(this, PerformanceDetailActivity::class.java)
//                intent.putExtra("type", type)
//                intent.putExtra("categoryId", selectedCategory!!.id)
//                intent.putExtra("categoryName", selectedCategory!!.name)
//                startActivity(intent)
//            }
//        }
//    }
//
//    private fun loadCategories() {
//        val categories = dbHelper.getAllCategories()
//
//        if (categories.isEmpty()) {
//            Toast.makeText(this, "No categories found", Toast.LENGTH_SHORT).show()
//        } else {
//            recyclerView.adapter = CategoryAdapter(categories) { category ->
//                // Update the selected category
//                selectedCategory = category
//
//                // Refresh the adapter to update checkboxes
//                (recyclerView.adapter as CategoryAdapter).notifyDataSetChanged()
//            }
//        }
//    }
//
//    inner class CategoryAdapter(
//        private val categories: List<Category>,
//        private val onItemSelected: (Category) -> Unit
//    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
//
//        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//            val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_category)
//            val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
//            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_category_stat, parent, false)
//            return CategoryViewHolder(view)
//        }
//
//        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
//            val category = categories[position]
//            holder.tvCategoryName.text = category.name
//
//            // Set checkbox state based on whether this category is selected
//            holder.checkbox.isChecked = selectedCategory?.id == category.id
//
//            // Remove the checkbox listener to avoid triggering it during binding
//            holder.checkbox.setOnCheckedChangeListener(null)
//
//            // Set up item click listener
//            holder.itemView.setOnClickListener {
//                onItemSelected(category)
//            }
//
//            // Set up checkbox click listener
//            holder.checkbox.setOnClickListener {
//                onItemSelected(category)
//            }
//        }
//
//        override fun getItemCount(): Int = categories.size
//    }
//}

//package com.example.humanreactor.statActivity
//
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.CheckBox
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.humanreactor.R
//import com.example.humanreactor.databases.ActionDatabaseHelper
//import com.example.humanreactor.databases.Category
//import com.example.humanreactor.databases.QuizDatabaseHelper
//import com.example.humanreactor.databases.QuizCategory
//
//class CategorySelectionActivity : AppCompatActivity() {
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var btnConfirm: Button
//    private lateinit var actionDbHelper: ActionDatabaseHelper
//    private lateinit var quizDbHelper: QuizDatabaseHelper
//    private var selectedActionCategory: Category? = null
//    private var selectedQuizCategory: QuizCategory? = null
//    private var type: String = "action"  // Default to action
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_category_selection)
//
//        // Get type from intent
//        type = intent.getStringExtra("type") ?: "action"
//
//        // Initialize views
//        recyclerView = findViewById(R.id.recycler_categories)
//        btnConfirm = findViewById(R.id.btn_confirm)
//
//        // Initialize database helpers
//        actionDbHelper = ActionDatabaseHelper(this)
//        quizDbHelper = QuizDatabaseHelper(this)
//
//        // Setup RecyclerView
//        recyclerView.layoutManager = LinearLayoutManager(this)
//
//        // Load categories based on type
//        when (type) {
//            "action" -> loadActionCategories()
//            "mental" -> loadQuizCategories()
//            else -> {
//                Toast.makeText(this, "Invalid type selected", Toast.LENGTH_SHORT).show()
//                finish()
//            }
//        }
//
//        // Setup confirm button
//        btnConfirm.setOnClickListener {
//            if (type == "action" && selectedActionCategory == null) {
//                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
//            } else if (type == "mental" && selectedQuizCategory == null) {
//                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
//            } else {
//                navigateToDetail()
//            }
//        }
//    }
//
//    private fun navigateToDetail() {
//        val intent = Intent(this, PerformanceDetailActivity::class.java)
//
//        if (type == "action" && selectedActionCategory != null) {
//            intent.putExtra("type", "action")
//            intent.putExtra("categoryId", selectedActionCategory!!.id)
//            intent.putExtra("categoryName", selectedActionCategory!!.name)
//        } else if (type == "quiz" && selectedQuizCategory != null) {
//            intent.putExtra("type", "quiz")
//            intent.putExtra("categoryId", selectedQuizCategory!!.id)
//            intent.putExtra("categoryName", selectedQuizCategory!!.name)
//        }
//
//        startActivity(intent)
//    }
//
//    private fun loadActionCategories() {
//        val categories = actionDbHelper.getAllCategories()
//
//        if (categories.isEmpty()) {
//            Toast.makeText(this, "No action categories found", Toast.LENGTH_SHORT).show()
//        } else {
//            recyclerView.adapter = ActionCategoryAdapter(categories) { category ->
//                // Update the selected category
//                selectedActionCategory = category
//                selectedQuizCategory = null  // Reset other category
//
//                // Refresh the adapter to update checkboxes
//                (recyclerView.adapter as ActionCategoryAdapter).notifyDataSetChanged()
//            }
//        }
//    }
//
//    private fun loadQuizCategories() {
//        val categories = quizDbHelper.getAllQuizCategories()
//
//        if (categories.isEmpty()) {
//            Toast.makeText(this, "No quiz categories found", Toast.LENGTH_SHORT).show()
//        } else {
//            recyclerView.adapter = QuizCategoryAdapter(categories) { category ->
//                // Update the selected category
//                selectedQuizCategory = category
//                selectedActionCategory = null  // Reset other category
//
//                // Refresh the adapter to update checkboxes
//                (recyclerView.adapter as QuizCategoryAdapter).notifyDataSetChanged()
//            }
//        }
//    }
//
//    // Adapter for Action Categories
//    inner class ActionCategoryAdapter(
//        private val categories: List<Category>,
//        private val onItemSelected: (Category) -> Unit
//    ) : RecyclerView.Adapter<ActionCategoryAdapter.CategoryViewHolder>() {
//
//        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//            val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_category)
//            val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
//            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_category_stat, parent, false)
//            return CategoryViewHolder(view)
//        }
//
//        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
//            val category = categories[position]
//            holder.tvCategoryName.text = category.name
//
//            // Set checkbox state based on whether this category is selected
//            holder.checkbox.isChecked = selectedActionCategory?.id == category.id
//
//            // Remove the checkbox listener to avoid triggering it during binding
//            holder.checkbox.setOnCheckedChangeListener(null)
//
//            // Set up item click listener
//            holder.itemView.setOnClickListener {
//                onItemSelected(category)
//            }
//
//            // Set up checkbox click listener
//            holder.checkbox.setOnClickListener {
//                onItemSelected(category)
//            }
//        }
//
//        override fun getItemCount(): Int = categories.size
//    }
//
//    // Adapter for Quiz Categories
//    inner class QuizCategoryAdapter(
//        private val categories: List<QuizCategory>,
//        private val onItemSelected: (QuizCategory) -> Unit
//    ) : RecyclerView.Adapter<QuizCategoryAdapter.CategoryViewHolder>() {
//
//        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//            val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_category)
//            val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
//            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_category_stat, parent, false)
//            return CategoryViewHolder(view)
//        }
//
//        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
//            val category = categories[position]
//            holder.tvCategoryName.text = category.name
//
//            // Set checkbox state based on whether this category is selected
//            holder.checkbox.isChecked = selectedQuizCategory?.id == category.id
//
//            // Remove the checkbox listener to avoid triggering it during binding
//            holder.checkbox.setOnCheckedChangeListener(null)
//
//            // Set up item click listener
//            holder.itemView.setOnClickListener {
//                onItemSelected(category)
//            }
//
//            // Set up checkbox click listener
//            holder.checkbox.setOnClickListener {
//                onItemSelected(category)
//            }
//        }
//
//        override fun getItemCount(): Int = categories.size
//    }
//}

package com.example.humanreactor.statActivity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.humanreactor.R
import com.example.humanreactor.databases.ActionDatabaseHelper
import com.example.humanreactor.databases.Category
import com.example.humanreactor.databases.QuizDatabaseHelper
import com.example.humanreactor.databases.QuizCategory

class CategorySelectionActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnConfirm: Button
    private lateinit var btnBack: Button
    private lateinit var actionDbHelper: ActionDatabaseHelper
    private lateinit var quizDbHelper: QuizDatabaseHelper
    private var selectedActionCategory: Category? = null
    private var selectedQuizCategory: QuizCategory? = null
    private var type: String = "action"  // Default to action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_selection)

        // Get type from intent
        type = intent.getStringExtra("type") ?: "action"

        // Initialize views
        recyclerView = findViewById(R.id.recycler_categories)
        btnConfirm = findViewById(R.id.btn_confirm)
        btnBack = findViewById(R.id.btn_back)

        // Initialize database helpers
        actionDbHelper = ActionDatabaseHelper(this)
        quizDbHelper = QuizDatabaseHelper(this)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load categories based on type
        when (type) {
            "action" -> loadActionCategories()
            "mental" -> loadQuizCategories()
            else -> {
                Toast.makeText(this, "Invalid type selected", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Setup confirm button
        btnConfirm.setOnClickListener {
            when (type) {
                "action" -> {
                    if (selectedActionCategory == null) {
                        Toast.makeText(this, "Please select an action category", Toast.LENGTH_SHORT).show()
                    } else {
                        navigateToDetail()
                    }
                }
                "mental" -> {
                    if (selectedQuizCategory == null) {
                        Toast.makeText(this, "Please select a quiz category", Toast.LENGTH_SHORT).show()
                    } else {
                        navigateToDetail()
                    }
                }
                else -> {
                    Toast.makeText(this, "Invalid type selected in mental", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun navigateToDetail() {
        val intent = Intent(this, PerformanceDetailActivity::class.java)

        when (type) {
            "action" -> {
                if (selectedActionCategory != null) {
                    intent.putExtra("type", "action")
                    intent.putExtra("categoryId", selectedActionCategory!!.id)
                    intent.putExtra("categoryName", selectedActionCategory!!.name)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Please select an action category", Toast.LENGTH_SHORT).show()
                }
            }
            "mental" -> {
                if (selectedQuizCategory != null) {
                    intent.putExtra("type", "mental")
                    intent.putExtra("categoryId", selectedQuizCategory!!.id)
                    intent.putExtra("categoryName", selectedQuizCategory!!.name)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Please select a quiz category", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "Invalid type selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadActionCategories() {
        val categories = actionDbHelper.getAllCategories()

        if (categories.isEmpty()) {
            Toast.makeText(this, "No action categories found", Toast.LENGTH_SHORT).show()
        } else {
            recyclerView.adapter = ActionCategoryAdapter(categories) { category ->
                // Update the selected category
                selectedActionCategory = category
                selectedQuizCategory = null  // Reset other category

                // Refresh the adapter to update checkboxes
                (recyclerView.adapter as ActionCategoryAdapter).notifyDataSetChanged()
            }
        }
    }

    private fun loadQuizCategories() {
        val categories = quizDbHelper.getAllQuizCategories()

        if (categories.isEmpty()) {
            Toast.makeText(this, "No quiz categories found", Toast.LENGTH_SHORT).show()
        } else {
            recyclerView.adapter = QuizCategoryAdapter(categories) { category ->
                // Update the selected category
                selectedQuizCategory = category
                selectedActionCategory = null  // Reset other category

                // Log for debugging
                println("Selected Quiz Category: ID=${category.id}, Name=${category.name}")

                // Refresh the adapter to update checkboxes
                (recyclerView.adapter as QuizCategoryAdapter).notifyDataSetChanged()
            }
        }
    }

    // Adapter for Action Categories
    inner class ActionCategoryAdapter(
        private val categories: List<Category>,
        private val onItemSelected: (Category) -> Unit
    ) : RecyclerView.Adapter<ActionCategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_category)
            val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_stat, parent, false)
            return CategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.tvCategoryName.text = category.name

            // Set checkbox state based on whether this category is selected
            holder.checkbox.isChecked = selectedActionCategory?.id == category.id

            // Remove the checkbox listener to avoid triggering it during binding
            holder.checkbox.setOnCheckedChangeListener(null)

            // Set up item click listener
            holder.itemView.setOnClickListener {
                onItemSelected(category)
            }

            // Set up checkbox click listener
            holder.checkbox.setOnClickListener {
                onItemSelected(category)
            }
        }

        override fun getItemCount(): Int = categories.size
    }

    // Adapter for Quiz Categories
    inner class QuizCategoryAdapter(
        private val categories: List<QuizCategory>,
        private val onItemSelected: (QuizCategory) -> Unit
    ) : RecyclerView.Adapter<QuizCategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val checkbox: CheckBox = itemView.findViewById(R.id.checkbox_category)
            val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_stat, parent, false)
            return CategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.tvCategoryName.text = category.name

            // Set checkbox state based on whether this category is selected
            holder.checkbox.isChecked = selectedQuizCategory?.id == category.id

            // Remove the checkbox listener to avoid triggering it during binding
            holder.checkbox.setOnCheckedChangeListener(null)

            // Set up item click listener
            holder.itemView.setOnClickListener {
                onItemSelected(category)
            }

            // Set up checkbox click listener
            holder.checkbox.setOnClickListener {
                onItemSelected(category)
            }
        }

        override fun getItemCount(): Int = categories.size
    }
}