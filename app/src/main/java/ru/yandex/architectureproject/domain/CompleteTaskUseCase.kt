package ru.yandex.architectureproject.domain

import kotlinx.coroutines.delay
import ru.yandex.architectureproject.data.repository.TaskRepository

private const val DELAY_FOR_COMPLETE = 10000L

class CompleteTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int) {
        repository.completeTask(taskId)
        delay(DELAY_FOR_COMPLETE)
        repository.deleteTask(taskId)
    }
}
