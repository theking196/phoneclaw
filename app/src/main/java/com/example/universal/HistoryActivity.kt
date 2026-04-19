package com.example.universal

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        setContentView(R.layout.activity_history)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Command History"
        toolbar.setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.historyRecyclerView)
        val empty = findViewById<LinearLayout>(R.id.emptyState)

        val history = LocalStorage.loadGenerationHistory(this)

        if (history.isEmpty()) {
            rv.visibility = View.GONE
            empty.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            empty.visibility = View.GONE
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = HistoryListAdapter(history)
        }
    }

    class HistoryListAdapter(private val items: List<GenerationHistory>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<HistoryListAdapter.VH>() {

        class VH(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val command: android.widget.TextView = view.findViewById(R.id.historyCommand)
            val time: android.widget.TextView = view.findViewById(R.id.historyTime)
            val preview: android.widget.TextView = view.findViewById(R.id.historyCodePreview)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH =
            VH(android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_history_card, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.command.text = item.userCommand
            holder.time.text = java.text.SimpleDateFormat("MMM dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.timestamp))
            holder.preview.text = item.generatedCode.lines().take(2).joinToString("\n")
        }
    }
}
