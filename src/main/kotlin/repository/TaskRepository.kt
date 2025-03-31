package com.taskmanager.repository

import com.taskmanager.model.Priority
import com.taskmanager.model.Status
import com.taskmanager.model.Task
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate

// table definition for storing tasks in database. it containts fields for task properties
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

// repository class to handle all database operation for tasks. it manages the connection and crud operations
class TaskRepository(private val database: Database) {
    init {
        transaction(database) {
            SchemaUtils.create(Tasks)
        }
    }

    // create a new task in database and return the generated id. it insert all task properties
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

    // get all task from database and convert them to task objects
    fun getAllTasks(): List<Task> = transaction(database) {
        Tasks.selectAll().map { it.toTask() }
    }

    // find a task by its id and return null if no task found with that id
    fun getTaskById(id: Int): Task? = transaction(database) {
        Tasks.select { Tasks.id eq id }
            .map { it.toTask() }
            .singleOrNull()
    }

    // update existing task details and return true if update was sucessful
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

    // delete a task by its id and return true if deletion was done
    fun deleteTask(id: Int): Boolean = transaction(database) {
        Tasks.deleteWhere { Tasks.id eq id } > 0
    }

    // search for tasks containing keyword in title, description or category field
    fun searchTasks(keyword: String): List<Task> = transaction(database) {
        Tasks.select {
            (Tasks.title like "%$keyword%") or
                    (Tasks.description like "%$keyword%") or
                    (Tasks.category.isNotNull() and (Tasks.category like "%$keyword%"))
        }.map { it.toTask() }
    }

    // filter tasks by specific category value
    fun getTasksByCategory(category: String): List<Task> = transaction(database) {
        Tasks.select { Tasks.category eq category }.map { it.toTask() }
    }

    // get tasks sorted by different fields depending on user preference
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

    // get tasks that are due within next 7 days from today
    fun getTasksDueNextWeek(): List<Task> = transaction(database) {
        val today = LocalDate.now()
        val nextWeek = today.plusDays(7)

        Tasks.select {
            (Tasks.dueDate greaterEq today) and (Tasks.dueDate lessEq nextWeek)
        }.map { it.toTask() }
    }

    // helper method that maps database row to task object. This makes code more cleaner
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
