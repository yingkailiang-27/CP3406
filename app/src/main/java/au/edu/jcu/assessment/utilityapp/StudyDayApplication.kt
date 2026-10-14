package au.edu.jcu.assessment.utilityapp

import android.app.Application
import au.edu.jcu.assessment.utilityapp.di.AppContainer
import au.edu.jcu.assessment.utilityapp.reminders.ClassReminderWorker
import au.edu.jcu.assessment.utilityapp.reminders.ReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StudyDayApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ClassReminderWorker.createChannel(this)
        applicationScope.launch {
            // Settings are observed outside the Activity so scheduling does not depend on a screen.
            try {
                container.plannerRepository.snapshots
                    .map { it.settings.remindersEnabled }
                    .distinctUntilChanged()
                    .collect { ReminderScheduler.setEnabled(this@StudyDayApplication, it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The foreground ViewModel surfaces a storage error with a retry action.
            }
        }
    }
}
