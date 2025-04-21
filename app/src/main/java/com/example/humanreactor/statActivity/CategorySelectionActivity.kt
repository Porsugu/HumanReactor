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

class CategorySelectionActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnConfirm: Button
    private lateinit var dbHelper: ActionDatabaseHelper
    private var selectedCategory: Category? = null
    private var type: String = "action"  // Default to action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_selection)

        // Get type from intent
        type = intent.getStringExtra("type") ?: "action"

        // Initialize views
        recyclerView = findViewById(R.id.recycler_categories)
        btnConfirm = findViewById(R.id.btn_confirm)

        // Initialize database helper
        dbHelper = ActionDatabaseHelper(this)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // If this is for actions, load categories from database
        if (type == "action") {
            loadCategories()
        } else {
            // For now, we're ignoring the mental flow as per requirements
            Toast.makeText(this, "Mental stats not implemented yet", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup confirm button
        btnConfirm.setOnClickListener {
            if (selectedCategory == null) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            } else {
                // Navigate to performance detail activity with the selected category
                val intent = Intent(this, PerformanceDetailActivity::class.java)
                intent.putExtra("categoryId", selectedCategory!!.id)
                intent.putExtra("categoryName", selectedCategory!!.name)
                startActivity(intent)
            }
        }
    }

    private fun loadCategories() {
        val categories = dbHelper.getAllCategories()

        if (categories.isEmpty()) {
            Toast.makeText(this, "No categories found", Toast.LENGTH_SHORT).show()
        } else {
            recyclerView.adapter = CategoryAdapter(categories) { category ->
                // Update the selected category
                selectedCategory = category

                // Refresh the adapter to update checkboxes
                (recyclerView.adapter as CategoryAdapter).notifyDataSetChanged()
            }
        }
    }

    inner class CategoryAdapter(
        private val categories: List<Category>,
        private val onItemSelected: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

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
            holder.checkbox.isChecked = selectedCategory?.id == category.id

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