package com.namesupport.worker

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.provider.ContactsContract
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    private const val WORK_NAME = "contact_monitor_periodic"
    const val CONTACT_CHANGE_JOB_ID = 1001

    fun enqueue(context: Context) {
        val request = PeriodicWorkRequestBuilder<ContactMonitorWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        scheduleContactChangeJob(context)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        context.getSystemService(JobScheduler::class.java).cancel(CONTACT_CHANGE_JOB_ID)
    }

    fun scheduleContactChangeJob(context: Context) {
        val job = JobInfo.Builder(
            CONTACT_CHANGE_JOB_ID,
            ComponentName(context, ContactChangeJobService::class.java)
        )
            .addTriggerContentUri(
                JobInfo.TriggerContentUri(
                    ContactsContract.Contacts.CONTENT_URI,
                    JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS
                )
            )
            .setTriggerContentUpdateDelay(2_000L)
            .setTriggerContentMaxDelay(10_000L)
            .build()
        context.getSystemService(JobScheduler::class.java).schedule(job)
    }
}
