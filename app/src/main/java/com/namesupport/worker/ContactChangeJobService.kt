package com.namesupport.worker

import android.app.job.JobParameters
import android.app.job.JobService
import com.namesupport.data.AppPreferences
import com.namesupport.data.ContactRepository
import com.namesupport.data.db.AppDatabase
import com.namesupport.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ContactChangeJobService : JobService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            try {
                val context = applicationContext
                val prefs = AppPreferences(context)
                if (!prefs.isMonitoringEnabled.first()) return@launch

                NotificationHelper.createChannel(context)

                val dao = AppDatabase.getInstance(context).contactRecordDao()
                val handledIds = dao.getAllHandledIds().toSet()

                val repository = ContactRepository(context)
                val candidates = repository.getHebrewContactsWithoutPhonetic()

                for (contact in candidates) {
                    if (contact.id !in handledIds) {
                        NotificationHelper.showApprovalNotification(context, contact)
                    }
                }
            } finally {
                // Content URI triggers are one-shot — reschedule to keep listening.
                WorkManagerScheduler.scheduleContactChangeJob(applicationContext)
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        scope.cancel()
        return true
    }
}
