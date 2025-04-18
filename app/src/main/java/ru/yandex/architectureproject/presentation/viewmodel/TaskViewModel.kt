package ru.yandex.architectureproject.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import ru.yandex.architectureproject.domain.AddTaskUseCase
import ru.yandex.architectureproject.domain.CompleteTaskUseCase
import ru.yandex.architectureproject.domain.DeleteTaskUseCase
import ru.yandex.architectureproject.domain.GetAllTasksUseCase
import ru.yandex.architectureproject.domain.IncompleteTaskUseCase
import ru.yandex.architectureproject.presentation.state.TaskAction
import ru.yandex.architectureproject.presentation.state.TaskAction.CreateTask
import ru.yandex.architectureproject.presentation.state.TaskAction.DeleteTask
import ru.yandex.architectureproject.presentation.state.TaskAction.LoadTasks
import ru.yandex.architectureproject.presentation.state.TaskAction.UpdateTask
import ru.yandex.architectureproject.presentation.state.TaskState
import java.util.concurrent.ConcurrentHashMap

class TaskViewModel(
    private val addTaskUseCase: AddTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val getAllTasksUseCase: GetAllTasksUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val incompleteTaskUseCase: IncompleteTaskUseCase,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val taskForDeletionJobMap: ConcurrentHashMap<Int, Job?> = ConcurrentHashMap()
    private val _state = MutableStateFlow<TaskState>(TaskState.Loading)
    private var collectJob: Job? = null
    val state: StateFlow<TaskState> = _state.asStateFlow()

    init {
        reduce(LoadTasks)
    }

    fun reduce(action: TaskAction) {
        when (action) {
            LoadTasks -> handleLoadTasks()
            is CreateTask -> handleCreateTask(action)
            is DeleteTask -> handleDeleteTask(action)
            is UpdateTask -> handleUpdateTask(action)
        }
    }

    private fun handleUpdateTask(action: UpdateTask) {
        viewModelScope.launch(ioDispatcher) {
            if (action.isCompleted) {
                taskForDeletionJobMap[action.taskId] = coroutineContext.job
                completeTaskUseCase.invoke(action.taskId)
            } else {
                taskForDeletionJobMap[action.taskId]?.cancel()
                incompleteTaskUseCase.invoke(action.taskId)
            }
        }
    }

    private fun handleCreateTask(action: CreateTask) {
        viewModelScope.launch(ioDispatcher) {
            addTaskUseCase.invoke(action.task)
        }
    }

    private fun handleDeleteTask(action: DeleteTask) {
        viewModelScope.launch(ioDispatcher) {
            deleteTaskUseCase.invoke(action.taskId)
        }
    }

    private fun handleLoadTasks() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch(ioDispatcher) {
            getAllTasksUseCase()
                .distinctUntilChanged()
                .onStart { _state.value = TaskState.Loading }
                .catch { e -> _state.value = TaskState.Error(e.message ?: "Ошибка загрузки") }
                .collect { tasks -> _state.value = TaskState.Loaded(tasks) }
        }
    }
}
