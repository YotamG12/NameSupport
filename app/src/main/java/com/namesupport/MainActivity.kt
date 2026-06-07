package com.namesupport

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.namesupport.notification.NotificationHelper
import com.namesupport.ui.ContactAdapter
import com.namesupport.ui.MainViewModel
import com.namesupport.ui.SettingsActivity
import com.namesupport.worker.WorkManagerScheduler

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ContactAdapter

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_CONTACTS] == true &&
            permissions[Manifest.permission.WRITE_CONTACTS] == true
        ) {
            WorkManagerScheduler.scheduleContactChangeJob(this)
            viewModel.scanContacts()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        NotificationHelper.createChannel(this)

        val btnScan = findViewById<View>(R.id.btnScan)
        val btnApply = findViewById<View>(R.id.btnApply)
        val progressBar = findViewById<LinearProgressIndicator>(R.id.progressBar)
        val layoutEmpty = findViewById<View>(R.id.layoutEmpty)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = ContactAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        btnScan.setOnClickListener { requestScan() }

        btnApply.setOnClickListener {
            val selected = adapter.getSelectedContacts()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_contacts_selected), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val hasWhatsAppOnly = selected.any { it.isWhatsAppOnly }
            if (hasWhatsAppOnly) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_whatsapp_title))
                    .setMessage(getString(R.string.dialog_whatsapp_message))
                    .setPositiveButton(getString(R.string.action_save_to_device)) { _, _ ->
                        viewModel.applyChanges(selected, saveWhatsAppToDevice = true)
                    }
                    .setNeutralButton(getString(R.string.dialog_phonetic_only)) { _, _ ->
                        viewModel.applyChanges(selected, saveWhatsAppToDevice = false)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                viewModel.applyChanges(selected)
            }
        }

        viewModel.contacts.observe(this) { contacts ->
            adapter.submitList(contacts)
            val hasResults = contacts.isNotEmpty()
            recyclerView.visibility = if (hasResults) View.VISIBLE else View.GONE
            layoutEmpty.visibility = if (hasResults) View.GONE else View.VISIBLE
            btnApply.isEnabled = hasResults
        }

        viewModel.hasScanned.observe(this) { scanned ->
            if (scanned && viewModel.contacts.value.isNullOrEmpty()) {
                tvEmpty.text = getString(R.string.no_contacts_found)
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnScan.isEnabled = !loading
            if (loading) btnApply.isEnabled = false
        }

        viewModel.message.observe(this) { msg ->
            msg ?: return@observe
            tvStatus.text = msg
            tvStatus.visibility = View.VISIBLE
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            viewModel.consumeMessage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestScan() {
        val permissionsNeeded = buildList {
            if (!hasPermission(Manifest.permission.READ_CONTACTS)) add(Manifest.permission.READ_CONTACTS)
            if (!hasPermission(Manifest.permission.WRITE_CONTACTS)) add(Manifest.permission.WRITE_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsNeeded.isEmpty()) {
            WorkManagerScheduler.scheduleContactChangeJob(this)
            viewModel.scanContacts()
        } else {
            requestPermissions.launch(permissionsNeeded.toTypedArray())
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
