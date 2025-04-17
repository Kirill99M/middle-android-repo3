package ru.yandex.architectureproject.presentation.state

sealed interface TaskAction {
    data object LoadTasks : TaskAction

    data class CreateTask(val task: String) : TaskAction

    data class UpdateTask(val taskId: Int, val isCompleted: Boolean) : TaskAction

    data class DeleteTask(val taskId: Int) : TaskAction
}
