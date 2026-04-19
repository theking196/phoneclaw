package com.example.universal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TasksFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_tasks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.tasksRecyclerView)
        val empty = view.findViewById<LinearLayout>(R.id.emptyState)
        val tasks = LocalStorage.loadCronTasks(requireContext())

        if (tasks.isEmpty()) {
            rv.visibility = View.GONE
            empty.visibility = View.VISIBLE
        } else {
            rv.visibility = View.VISIBLE
            empty.visibility = View.GONE
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = ScheduledTasksActivity.TasksListAdapter(tasks.values.toList())
        }
    }
}
