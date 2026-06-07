package com.namesupport.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.namesupport.data.ContactRepository
import com.namesupport.data.db.AppDatabase
import com.namesupport.data.db.ContactRecord
import com.namesupport.model.ContactItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ApprovalBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ApprovalReceiver"
        const val ACTION_APPROVE = "com.namesupport.ACTION_APPROVE"
        const val ACTION_DISMISS = "com.namesupport.ACTION_DISMISS"
        const val ACTION_SAVE_TO_DEVICE = "com.namesupport.ACTION_SAVE_TO_DEVICE"
        const val EXTRA_CONTACT_ID = "extra_contact_id"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_SUGGESTION = "extra_suggestion"
        const val EXTRA_IS_WHATSAPP_ONLY = "extra_is_whatsapp_only"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (contactId == -1L) return

        NotificationManagerCompat.from(context).cancel(notifId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).contactRecordDao()
                when (intent.action) {
                    ACTION_APPROVE -> {
                        val name = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: return@launch
                        val suggestion = intent.getStringExtra(EXTRA_SUGGESTION) ?: return@launch
                        val isWhatsApp = intent.getBooleanExtra(EXTRA_IS_WHATSAPP_ONLY, false)
                        val contact = ContactItem(contactId, name, suggestion, isWhatsApp)
                        val success = ContactRepository(context).applyPhoneticName(contact)
                        if (success) {
                            dao.upsert(ContactRecord(contactId, ContactRecord.APPROVED))
                        } else {
                            Log.e(TAG, "Failed to write phonetic name for contact $contactId")
                        }
                    }
                    ACTION_SAVE_TO_DEVICE -> {
                        val name = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: return@launch
                        val suggestion = intent.getStringExtra(EXTRA_SUGGESTION) ?: return@launch
                        val contact = ContactItem(contactId, name, suggestion, isWhatsAppOnly = true)
                        val success = ContactRepository(context).saveToDeviceAndApply(contact)
                        if (success) {
                            dao.upsert(ContactRecord(contactId, ContactRecord.APPROVED))
                        } else {
                            Log.e(TAG, "saveToDeviceAndApply failed for contact $contactId")
                        }
                    }
                    ACTION_DISMISS -> {
                        dao.upsert(ContactRecord(contactId, ContactRecord.DISMISSED))
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
