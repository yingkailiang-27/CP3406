package au.edu.jcu.assessment.utilityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import au.edu.jcu.assessment.utilityapp.data.PlannerRepository
import au.edu.jcu.assessment.utilityapp.data.WeatherInfo
import au.edu.jcu.assessment.utilityapp.data.WeatherRepository
import au.edu.jcu.assessment.utilityapp.model.PlannerSettings
import au.edu.jcu.assessment.utilityapp.model.PlannerSnapshot
import au.edu.jcu.assessment.utilityapp.model.StudyCourse
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class WeatherUiState(
    val loading: Boolean = false,
    val data: WeatherInfo? = null,
    val error: String? = null
)

data class PlannerUiState(
    val snapshot: PlannerSnapshot = PlannerSnapshot(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val storageError: String? = null,
    val weather: WeatherUiState = WeatherUiState(),
    val message: String? = null
)

class PlannerViewModel(
    private val planner: PlannerRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = mutableState.asStateFlow()
    private var observation: Job? = null
    private var weatherJob: Job? = null
    private var requestId = 0

    init { observeSavedData() }

    fun observeSavedData() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, storageError = null) }
        observation = viewModelScope.launch {
            var previous: PlannerSettings? = null
            try {
                planner.snapshots.collect { snapshot ->
                    mutableState.update { it.copy(snapshot = snapshot, loading = false, storageError = null) }
                    val settings = snapshot.settings
                    if (previous == null || previous?.city != settings.city ||
                        previous?.temperatureUnit != settings.temperatureUnit ||
                        previous?.showWeather != settings.showWeather
                    ) {
                        refreshWeather()
                    }
                    previous = settings
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(loading = false, storageError = "Unable to read saved data. Please retry.")
                }
            }
        }
    }

    /** Cancellation + a request sequence prevent an older city request replacing a newer one. */
    fun refreshWeather() {
        val settings = mutableState.value.snapshot.settings
        val id = ++requestId
        weatherJob?.cancel()
        if (!settings.showWeather) {
            mutableState.update { it.copy(weather = WeatherUiState()) }
            return
        }
        mutableState.update { it.copy(weather = WeatherUiState(loading = true)) }
        weatherJob = viewModelScope.launch {
            try {
                val data = weatherRepository.fetch(settings.city, settings.temperatureUnit)
                if (id == requestId) mutableState.update { it.copy(weather = WeatherUiState(data = data)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                val message = when (exception) {
                    is IOException -> "Couldn't load weather. Check your connection and retry."
                    is HttpException -> "Weather service is unavailable. Please retry later."
                    else -> "Weather data couldn't be read. Please retry."
                }
                if (id == requestId) mutableState.update {
                    it.copy(weather = WeatherUiState(error = message))
                }
            }
        }
    }

    private fun save(success: String? = null, onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (mutableState.value.saving || mutableState.value.loading ||
            mutableState.value.storageError != null
        ) return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                action()
                mutableState.update { it.copy(message = success) }
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mutableState.update {
                    it.copy(message = if (exception is IllegalArgumentException) exception.message
                        else "Changes could not be saved. Please try again.")
                }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun addTask(title: String, deadline: String? = null, onSuccess: () -> Unit) =
        save("Task added.", onSuccess) { planner.addTask(title, deadline) }
    fun toggleTask(id: String) = save { planner.toggleTask(id) }
    fun deleteTask(id: String) = save("Task deleted.") { planner.deleteTask(id) }
    fun addCourse(course: StudyCourse, onSuccess: () -> Unit) =
        save("Weekly class added.", onSuccess) { planner.addCourse(course) }
    fun deleteCourse(id: String) = save("Class deleted.") { planner.deleteCourse(id) }
    fun updateSettings(change: (PlannerSettings) -> PlannerSettings) =
        save { planner.updateSettings(change) }
    fun showMessage(message: String) { mutableState.update { it.copy(message = message) } }
    fun clearMessage() { mutableState.update { it.copy(message = null) } }

    class Factory(
        private val planner: PlannerRepository,
        private val weather: WeatherRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PlannerViewModel::class.java))
            return PlannerViewModel(planner, weather) as T
        }
    }
}
