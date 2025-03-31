package com.taskmanager.service

import com.taskmanager.model.Priority
import com.taskmanager.model.Status
import com.taskmanager.model.Task
import com.taskmanager.repository.TaskRepository
import java.time.LocalDate

class TaskService(private val taskRepository: TaskRepository) {
    fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate,
        priority: Priority,
        status: Status,
        category: String?
    ): Int {
        val task = Task(
            title = title,
            description = description,
            dueDate = dueDate,
            priority = priority,
            status = status,
            category = category
        )
        return taskRepository.createTask(task)
    }

    fun getAllTasks(): List<Task> = taskRepository.getAllTasks()

    fun getTaskById(id: Int): Task? = taskRepository.getTaskById(id)

    fun updateTask(task: Task): Boolean = taskRepository.updateTask(task)

    fun deleteTask(id: Int): Boolean = taskRepository.deleteTask(id)

    // Additional feature methods
    fun searchTasks(keyword: String): List<Task> = taskRepository.searchTasks(keyword)

    fun getTasksByCategory(category: String): List<Task> = taskRepository.getTasksByCategory(category)

    fun getTasksSortedBy(sortField: String): List<Task> = taskRepository.getTasksSortedBy(sortField)

    fun getTasksDueNextWeek(): List<Task> = taskRepository.getTasksDueNextWeek()

    fun getTaskStatistics(): Map<String, Any> {
        val allTasks = getAllTasks()
        val completedTasks = allTasks.count { it.status == Status.COMPLETED }
        val todayTasks = allTasks.count { it.dueDate == LocalDate.now() }
        val overdueTasks = allTasks.count {
            it.dueDate.isBefore(LocalDate.now()) && it.status != Status.COMPLETED
        }
        val dueNextWeekTasks = taskRepository.getTasksDueNextWeek().size
        val highPriorityTasks = allTasks.count { it.priority == Priority.HIGH }

        return mapOf(
            "totalTasks" to allTasks.size,
            "completedTasks" to completedTasks,
            "completionRate" to if (allTasks.isNotEmpty()) completedTasks * 100.0 / allTasks.size else 0.0,
            "dueTodayTasks" to todayTasks,
            "overdueTasks" to overdueTasks,
            "dueNextWeekTasks" to dueNextWeekTasks,
            "highPriorityTasks" to highPriorityTasks
        )
    }
}