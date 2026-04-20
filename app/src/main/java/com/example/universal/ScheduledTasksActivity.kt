package com.example.universal

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ScheduledTasksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        setContentView(R.layout.activity_scheduled_tasks)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Scheduled Tasks"
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.tasksRecyclerView)
        val empty = findViewById<LinearLayout>(R.id.emptyState)
        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddTask)
        fab?.setOnClickListener { showAddTaskDialog() }
        val tasks = LocalStorage.loadCronTasks(this)

        if (tasks.isEmpty()) {
            rv.visibility = View.GONE
            empty.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            empty.visibility = View.GONE
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = TasksListAdapter(tasks.values.toList())
        }
    }

    class TasksListAdapter(private val items: List<CronTask>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<TasksListAdapter.VH>() {

        class VH(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val desc: android.widget.TextView = view.findViewById(R.id.taskDescription)
            val cron: android.widget.TextView = view.findViewById(R.id.cronExpression)
            val status: android.widget.TextView = view.findViewById(R.id.taskStatus)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH =
            VH(android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_task_card, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.desc.text = item.taskDescription
            holder.cron.text = item.cronExpression
            holder.status.text = if (item.isActive) "✅ Active" else "⏸ Paused"
        }
    }

    private fun showAddTaskDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        
        val taskInput = EditText(this).apply { hint = "Task description" }
        val cronInput = EditText(this).apply { hint = "Cron (* * * * *)" }
        
        layout.addView(taskInput)
        layout.addView(cronInput)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Scheduled Task")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val desc = taskInput.text.toString()
                val cron = cronInput.text.toString()
                if (desc.isNotBlank() && cron.isNotBlank()) {
                    addTask(desc, cron)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addTask(description: String, cron: String) {
        // Save task
        val prefs = getSharedPreferences("tasks", MODE_PRIVATE)
        prefs.edit().putString("task_${System.currentTimeMillis()}", "$description|$cron").apply()
        Toast.makeText(this, "Task saved!", Toast.LENGTH_SHORT).show()
        recreate()
    }
}

