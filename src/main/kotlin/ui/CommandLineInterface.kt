package com.taskmanager.ui

import com.taskmanager.model.Priority
import com.taskmanager.model.Status
import com.taskmanager.model.Task
import com.taskmanager.service.TaskService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// command line interface for managing tasks. it handle user inputs and display outputs
class CommandLineInterface(private val taskService: TaskService) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // start the application and display menu until user choose to exit
    fun start() {
        println("=== Welcome to Task Manager ===")
        var running = true

        while (running) {
            printMenu()
            when (readCommand()) {
                "add" -> addTask()
                "list" -> listTasks()
                "view" -> viewTask()
                "edit" -> editTask()
                "delete" -> deleteTask()
                "search" -> searchTasks()
                "sort" -> sortTasks()
                "stats" -> showStatistics()
                "upcoming" -> showUpcomingTasks()
                "help" -> printHelp()
                "exit" -> {
                    println("Goodbye!")
                    running = false
                }
                else -> println("Unknown command. Type 'help' to see available commands.")
            }
        }
    }

    // display the main menu with all available commands
    private fun printMenu() {
        println("\nAvailable commands:")
        println("  add     - Add a new task")
        println("  list    - List all tasks")
        println("  view    - View task details")
        println("  edit    - Edit a task")
        println("  delete  - Delete a task")
        println("  search  - Search tasks by keyword")
        println("  sort    - Sort tasks by field")
        println("  stats   - Show task statistics")
        println("  upcoming - Show tasks due in the next week")
        println("  help    - Show help")
        println("  exit    - Exit the application")
        print("\nEnter command: ")
    }

    // show detailed help information about commands and data formats
    private fun printHelp() {
        println("\n=== Task Manager Help ===")
        println("This application allows you to manage your tasks.")
        println("\nCommands:")
        println("  add     - Create a new task with title, description, due date, priority, and status")
        println("  list    - Display all tasks")
        println("  view    - View details of a specific task by ID")
        println("  edit    - Update an existing task")
        println("  delete  - Remove a task by ID")
        println("  search  - Find tasks containing a keyword")
        println("  sort    - Sort tasks by due date, priority, status, or title")
        println("  stats   - Show statistics about your tasks")
        println("  upcoming - Show tasks due in the next 7 days")
        println("  help    - Display this help message")
        println("  exit    - Close the application")
        println("\nDate format: yyyy-MM-dd (e.g., 2023-12-31)")
        println("Priority levels: LOW, MEDIUM, HIGH")
        println("Status options: TODO, IN_PROGRESS, COMPLETED")
    }

    // read user input and convert to lowercase for easier processing
    private fun readCommand(): String = readLine()?.trim()?.lowercase() ?: ""

    // collect task information from user and create new task in system
    private fun addTask() {
        println("\n=== Add New Task ===")

        print("Title: ")
        val title = readLine()?.trim() ?: ""
        if (title.isEmpty()) {
            println("Title cannot be empty")
            return
        }

        print("Description: ")
        val description = readLine()?.trim() ?: ""

        print("Due Date (yyyy-MM-dd): ")
        val dueDate = tryParseDueDate() ?: return

        print("Priority (LOW, MEDIUM, HIGH): ")
        val priority = tryParsePriority() ?: return

        print("Status (TODO, IN_PROGRESS, COMPLETED): ")
        val status = tryParseStatus() ?: return

        print("Category (optional): ")
        val category = readLine()?.trim()?.takeIf { it.isNotEmpty() }

        val taskId = taskService.createTask(title, description, dueDate, priority, status, category)
        println("Task created with ID: $taskId")
    }

    // show all tasks in system in a table format
    private fun listTasks() {
        println("\n=== All Tasks ===")
        val tasks = taskService.getAllTasks()
        if (tasks.isEmpty()) {
            println("No tasks found")
            return
        }

        printTaskList(tasks)
    }

    // get and display detailed information about specific task
    private fun viewTask() {
        print("Enter task ID: ")
        val id = readLine()?.trim()?.toIntOrNull()
        if (id == null) {
            println("Invalid ID")
            return
        }

        val task = taskService.getTaskById(id)
        if (task == null) {
            println("Task not found")
            return
        }

        printTaskDetails(task)
    }

    // update existing task with new values. it keeps old values when user skip input
    private fun editTask() {
        print("Enter task ID to edit: ")
        val id = readLine()?.trim()?.toIntOrNull()
        if (id == null) {
            println("Invalid ID")
            return
        }

        val existingTask = taskService.getTaskById(id)
        if (existingTask == null) {
            println("Task not found")
            return
        }

        println("\n=== Edit Task $id ===")
        println("(Press Enter to keep current value)")

        print("Title [${existingTask.title}]: ")
        val title = readLine()?.trim()?.takeIf { it.isNotEmpty() } ?: existingTask.title

        print("Description [${existingTask.description}]: ")
        val description = readLine()?.trim()?.takeIf { it.isNotEmpty() } ?: existingTask.description

        print("Due Date [${existingTask.dueDate.format(dateFormatter)}] (yyyy-MM-dd): ")
        val dueDate = tryParseDueDate() ?: existingTask.dueDate

        print("Priority [${existingTask.priority}] (LOW, MEDIUM, HIGH): ")
        val priorityInput = readLine()?.trim()
        val priority = if (priorityInput.isNullOrEmpty()) {
            existingTask.priority
        } else {
            try {
                Priority.valueOf(priorityInput.uppercase())
            } catch (e: IllegalArgumentException) {
                println("Invalid priority, keeping current value")
                existingTask.priority
            }
        }

        print("Status [${existingTask.status}] (TODO, IN_PROGRESS, COMPLETED): ")
        val statusInput = readLine()?.trim()
        val status = if (statusInput.isNullOrEmpty()) {
            existingTask.status
        } else {
            try {
                Status.valueOf(statusInput.uppercase().replace(" ", "_"))
            } catch (e: IllegalArgumentException) {
                println("Invalid status, keeping current value")
                existingTask.status
            }
        }

        print("Category [${existingTask.category ?: "None"}]: ")
        val categoryInput = readLine()?.trim()
        val category = when {
            categoryInput.isNullOrEmpty() -> existingTask.category
            categoryInput.equals("none", ignoreCase = true) -> null
            else -> categoryInput
        }

        val updatedTask = existingTask.copy(
            title = title,
            description = description,
            dueDate = dueDate,
            priority = priority,
            status = status,
            category = category
        )

        if (taskService.updateTask(updatedTask)) {
            println("Task updated successfully")
        } else {
            println("Failed to update task")
        }
    }

    // remove task from system after user confirmation
    private fun deleteTask() {
        print("Enter task ID to delete: ")
        val id = readLine()?.trim()?.toIntOrNull()
        if (id == null) {
            println("Invalid ID")
            return
        }

        print("Are you sure you want to delete task $id? (yes/no): ")
        val confirmation = readLine()?.trim()?.lowercase()
        if (confirmation == "yes" || confirmation == "y") {
            if (taskService.deleteTask(id)) {
                println("Task deleted successfully")
            } else {
                println("Failed to delete task. Task may not exist.")
            }
        } else {
            println("Deletion cancelled")
        }
    }

    // search for task containing specific keyword in title, description or category
    private fun searchTasks() {
        println("\n=== Search Tasks ===")
        print("Enter search keyword: ")
        val keyword = readLine()?.trim() ?: ""

        if (keyword.isEmpty()) {
            println("Search keyword cannot be empty")
            return
        }

        val tasks = taskService.searchTasks(keyword)
        if (tasks.isEmpty()) {
            println("No tasks found for keyword: $keyword")
            return
        }

        println("\nSearch results for: $keyword")
        printTaskList(tasks)
    }

    // display tasks in sorted order based on selected field
    private fun sortTasks() {
        println("\n=== Sort Tasks ===")
        println("Sort by: duedate, priority, status, title")
        print("Enter sort field: ")
        val sortField = readLine()?.trim()?.lowercase() ?: ""

        if (sortField !in listOf("duedate", "priority", "status", "title")) {
            println("Invalid sort field")
            return
        }

        val tasks = taskService.getTasksSortedBy(sortField)
        println("\nTasks sorted by $sortField:")
        printTaskList(tasks)
    }

    // show summary statistics about all task in the system
    private fun showStatistics() {
        println("\n=== Task Statistics ===")
        val stats = taskService.getTaskStatistics()

        println("Total tasks: ${stats["totalTasks"]}")
        println("Completed tasks: ${stats["completedTasks"]}")
        println("Completion rate: ${"%.1f".format(stats["completionRate"])}%")
        println("Tasks due today: ${stats["dueTodayTasks"]}")
        println("Overdue tasks: ${stats["overdueTasks"]}")
        println("Tasks due in next week: ${stats["dueNextWeekTasks"]}")
        println("High priority tasks: ${stats["highPriorityTasks"]}")
    }

    // display tasks that are due in the next 7 days from today
    private fun showUpcomingTasks() {
        println("\n=== Tasks Due in Next Week ===")
        val tasks = taskService.getTasksDueNextWeek()

        if (tasks.isEmpty()) {
            println("No tasks due in the next 7 days")
            return
        }

        printTaskList(tasks)
    }

    // helper function to format and display list of tasks in table format
    private fun printTaskList(tasks: List<Task>) {
        val format = "| %-4s | %-25s | %-10s | %-8s | %-12s | %-10s |"
        println(String.format(format, "ID", "Title", "Due Date", "Priority", "Status", "Category"))
        println("-".repeat(80))

        tasks.forEach { task ->
            println(String.format(
                format,
                task.id,
                task.title.take(25),
                task.dueDate.format(dateFormatter),
                task.priority,
                task.status,
                task.category?.take(10) ?: "-"
            ))
        }
    }

    // display all details of a single task in vertical format
    private fun printTaskDetails(task: Task) {
        println("\n=== Task ${task.id} ===")
        println("Title: ${task.title}")
        println("Description: ${task.description}")
        println("Due Date: ${task.dueDate.format(dateFormatter)}")
        println("Priority: ${task.priority}")
        println("Status: ${task.status}")
        println("Category: ${task.category ?: "None"}")
    }

    // parse user input for date and handle error. it returns today date if input empty
    private fun tryParseDueDate(): LocalDate? {
        return try {
            val input = readLine()?.trim()
            if (input.isNullOrEmpty()) {
                println("Using today's date")
                LocalDate.now()
            } else {
                LocalDate.parse(input, dateFormatter)
            }
        } catch (e: DateTimeParseException) {
            println("Invalid date format. Please use yyyy-MM-dd (e.g., 2023-12-31)")
            null
        }
    }

    // parse user input for priority and use medium as default. it validate against enum values
    private fun tryParsePriority(): Priority? {
        return try {
            val input = readLine()?.trim()?.uppercase() ?: "MEDIUM"
            if (input.isEmpty()) {
                println("Using default priority: MEDIUM")
                Priority.MEDIUM
            } else {
                Priority.valueOf(input)
            }
        } catch (e: IllegalArgumentException) {
            println("Invalid priority. Options are: LOW, MEDIUM, HIGH")
            null
        }
    }

    // parse user input for status and use todo as defaut value. it replace spaces with underscores
    private fun tryParseStatus(): Status? {
        return try {
            val input = readLine()?.trim()?.uppercase()?.replace(" ", "_") ?: "TODO"
            if (input.isEmpty()) {
                println("Using default status: TODO")
                Status.TODO
            } else {
                Status.valueOf(input)
            }
        } catch (e: IllegalArgumentException) {
            println("Invalid status. Options are: TODO, IN_PROGRESS, COMPLETED")
            null
        }
    }
}

