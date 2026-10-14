package au.edu.jcu.assessment.utilityapp.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.edu.jcu.assessment.utilityapp.MainActivity
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.StudyDayApplication
import au.edu.jcu.assessment.utilityapp.model.minuteLabel
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

object ReminderScheduler {
    private const val WORK_NAME = "studyday-class-reminders"
    fun setEnabled(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (enabled) {
            manager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ClassReminderWorker>(15, TimeUnit.MINUTES).build()
            )
        } else {
            manager.cancelUniqueWork(WORK_NAME)
        }
    }
}

/**
 * Best-effort reminders. WorkManager may delay checks under Doze/battery restrictions.
 * Atomic reminder claims prevent a weekly occurrence from generating repeated alerts.
 */
class ClassReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return Result.success()

        return try {
            val repository = (applicationContext as StudyDayApplication).container.plannerRepository
            val due = repository.claimDueReminders(LocalDateTime.now())
            val intent = Intent(applicationContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            due.forEach { occurrence ->
                val room = occurrence.course.room.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
                val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_class_notification)
                    .setContentTitle(applicationContext.getString(R.string.notification_title))
                    .setContentText(applicationContext.getString(
                        R.string.notification_body, occurrence.course.title,
                        minuteLabel(occurrence.course.startMinute), room
                    ))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                // Permission can be revoked between the initial check and notify().
                try { manager.notify(occurrence.course.id.hashCode(), notification) }
                catch (_: SecurityException) { /* Notification permission was revoked. */ }
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val CHANNEL_ID = "studyday_classes"
        fun createChannel(context: Context) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, context.getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(R.string.notification_channel_description) }
            )
        }
    }
}
