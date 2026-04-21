package com.namesupport.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.namesupport.data.ContactRepository
import com.namesupport.model.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactRepository(application)

    private val _contacts = MutableLiveData<List<ContactItem>>(emptyList())
    val contacts: LiveData<List<ContactItem>> = _contacts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _hasScanned = MutableLiveData(false)
    val hasScanned: LiveData<Boolean> = _hasScanned

    private val _message = MutableLiveData<String?>(null)
    val message: LiveData<String?> = _message

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

    fun applyChanges(selectedContacts: List<ContactItem>) {
        if (selectedContacts.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            val successCount = withContext(Dispatchers.IO) {
                selectedContacts.count { contact -> repository.applyPhoneticName(contact) }
            }
            _message.value = "Updated $successCount of ${selectedContacts.size} contacts"
            val refreshed = withContext(Dispatchers.IO) {
                repository.getHebrewContactsWithoutPhonetic()
            }
            _contacts.value = refreshed
            _hasScanned.value = true
            _isLoading.value = false
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
