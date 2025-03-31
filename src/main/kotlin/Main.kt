package com.taskmanager

import com.taskmanager.repository.TaskRepository
import com.taskmanager.service.TaskService
import com.taskmanager.ui.CommandLineInterface
import org.jetbrains.exposed.sql.Database
import java.lang.System

fun main() {
    try {
        // Get database connection parameters from environment variables or use defaults
        val dbUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/taskmanager"
        val dbUser = System.getenv("DB_USER") ?: "postgres"
        val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"

        println("Connecting to database: $dbUrl")

        // Initialize database connection
        val database = Database.connect(
            url = dbUrl,
            driver = "org.postgresql.Driver",
            user = dbUser,
            password = dbPassword
        )

        println("Database connection established")

        // Setup application components
        val taskRepository = TaskRepository(database)
        val taskService = TaskService(taskRepository)
        val cli = CommandLineInterface(taskService)

        // Start the CLI
        cli.start()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}
