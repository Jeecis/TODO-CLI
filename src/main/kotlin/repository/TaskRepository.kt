package com.taskmanager.repository

import com.taskmanager.model.Priority
import com.taskmanager.model.Status
import com.taskmanager.model.Task
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

object Tasks : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 100)
    val description = text("description")
    val dueDate = date("due_date")
    val priority = enumeration<Priority>("priority")
    val status = enumeration<Status>("status")
    val category = varchar("category", 50).nullable()

    override val primaryKey = PrimaryKey(id)
}

class TaskRepository(private val database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(Tasks)
        }
    }

    fun createTask(task: Task): Int = transaction(database) {
        Tasks.insert {
            it[title] = task.title
            it[description] = task.description
            it[dueDate] = task.dueDate
            it[priority] = task.priority
            it[status] = task.status
            it[category] = task.category
        }[Tasks.id]
    }

    fun getAllTasks(): List<Task> = transaction(database) {
        Tasks.selectAll().map { it.toTask() }
    }

    fun getTaskById(id: Int): Task? = transaction(database) {
        Tasks.select { Tasks.id eq id }
            .map { it.toTask() }
            .singleOrNull()
    }

    fun updateTask(task: Task): Boolean = transaction(database) {
        if (task.id == null) return@transaction false

        Tasks.update({ Tasks.id eq task.id }) {
            it[title] = task.title
            it[description] = task.description
            it[dueDate] = task.dueDate
            it[priority] = task.priority
            it[status] = task.status
            it[category] = task.category
        } > 0
    }

    fun deleteTask(id: Int): Boolean = transaction(database) {
        Tasks.deleteWhere { Tasks.id eq id } > 0
    }

    // Additional feature 1: Search by keyword
    fun searchTasks(keyword: String): List<Task> = transaction(database) {
        Tasks.select {
            (Tasks.title like "%$keyword%") or
                    (Tasks.description like "%$keyword%") or
                    (Tasks.category.isNotNull() and (Tasks.category like "%$keyword%"))
        }.map { it.toTask() }
    }

    // Additional feature 1: Search by category
    fun getTasksByCategory(category: String): List<Task> = transaction(database) {
        Tasks.select { Tasks.category eq category }.map { it.toTask() }
    }

    // Additional feature 1: Sort by different fields
    fun getTasksSortedBy(sortField: String): List<Task> = transaction(database) {
        val query = when (sortField.lowercase()) {
            "duedate" -> Tasks.selectAll().orderBy(Tasks.dueDate)
            "priority" -> Tasks.selectAll().orderBy(Tasks.priority)
            "status" -> Tasks.selectAll().orderBy(Tasks.status)
            "title" -> Tasks.selectAll().orderBy(Tasks.title)
            else -> Tasks.selectAll()
        }
        query.map { it.toTask() }
    }

    // Additional feature 2: Tasks due in next week
    fun getTasksDueNextWeek(): List<Task> = transaction(database) {
        val today = LocalDate.now()
        val nextWeek = today.plusDays(7)

        Tasks.select {
            (Tasks.dueDate greaterEq today) and (Tasks.dueDate lessEq nextWeek)
        }.map { it.toTask() }
    }

    // Helper function to convert row to Task object
    private fun ResultRow.toTask(): Task {
        return Task(
            id = this[Tasks.id],
            title = this[Tasks.title],
            description = this[Tasks.description],
            dueDate = this[Tasks.dueDate],
            priority = this[Tasks.priority],
            status = this[Tasks.status],
            category = this[Tasks.category]
        )
    }
}
