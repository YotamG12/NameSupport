package com.namesupport.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.namesupport.data.AppPreferences
import com.namesupport.data.ContactRepository
import com.namesupport.model.ContactItem
import com.namesupport.service.ContactsMonitorService
import com.namesupport.worker.ContactSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactRepository(application)
    val prefs = AppPreferences(application)

    // ── Scan / preview state ──────────────────────────────────────────────────

    private val _contacts = MutableLiveData<List<ContactItem>>(emptyList())
    val contacts: LiveData<List<ContactItem>> = _contacts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasScanned = MutableLiveData(false)
    val hasScanned: LiveData<Boolean> = _hasScanned

    private val _message = MutableLiveData<String?>(null)
    val message: LiveData<String?> = _message

    // ── Status screen state ───────────────────────────────────────────────────

    private val _serviceRunning = MutableLiveData(false)
    val serviceRunning: LiveData<Boolean> = _serviceRunning

    private val _translatedCount = MutableLiveData(0)
    val translatedCount: LiveData<Int> = _translatedCount

    // ── Actions ───────────────────────────────────────────────────────────────

    fun refreshStatus() {
        _serviceRunning.value = ContactsMonitorService.isRunning
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) { repository.countTranslatedContacts() }
            _translatedCount.value = count
        }
    }

    fun scanContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                repository.getHebrewContactsWithoutPhonetic()
            }
            _contacts.value = result
            _hasScanned.value = true
            _isLoading.value = false
        }
    }

    /**
     * Apply transliterations to [selectedContacts].
     * If [completeFirstRun] is true, marks onboarding done and starts the
     * background service + WorkManager schedule.
     */
    fun applyChanges(selectedContacts: List<ContactItem>, completeFirstRun: Boolean = false) {
        if (selectedContacts.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            val successCount = withContext(Dispatchers.IO) {
                selectedContacts.count { repository.applyPhoneticName(it) }
            }
            _message.value = "Updated $successCount of ${selectedContacts.size} contacts"

            if (completeFirstRun) {
                prefs.isFirstRunComplete = true
                startMonitorService()
                ContactSyncWorker.schedule(getApplication())
            }

            val refreshed = withContext(Dispatchers.IO) {
                repository.getHebrewContactsWithoutPhonetic()
            }
            _contacts.value = refreshed
            _hasScanned.value = true
            _isLoading.value = false
            refreshStatus()
        }
    }

    fun setServiceEnabled(enabled: Boolean) {
        prefs.serviceEnabled = enabled
        val app = getApplication<Application>()
        if (enabled) {
            startMonitorService()
            ContactSyncWorker.schedule(app)
        } else {
            app.stopService(Intent(app, ContactsMonitorService::class.java))
            ContactSyncWorker.cancel(app)
        }
        _serviceRunning.value = enabled
    }

    fun consumeMessage() {
        _message.value = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun startMonitorService() {
        val app = getApplication<Application>()
        val intent = Intent(app, ContactsMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(intent)
        } else {
            app.startService(intent)
        }
    }
}
