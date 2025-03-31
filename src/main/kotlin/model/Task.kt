package com.taskmanager.model

import java.time.LocalDate

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class Status {
    TODO, IN_PROGRESS, COMPLETED
}

data class Task(
    val id: Int? = null,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val priority: Priority,
    val status: Status,
    val category: String? = null
)