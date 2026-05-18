package com.namesupport

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.namesupport.notification.NotificationHelper
import com.namesupport.ui.ContactAdapter
import com.namesupport.ui.MainViewModel
import com.namesupport.util.ApiKeyManager

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ContactAdapter

    // ── Permission launchers ──────────────────────────────────────────────────

    private val requestContacts = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            viewModel.scanContacts()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        }
    }

    private val requestNotification = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* notification is best-effort; proceed regardless */ }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationHelper.createChannels(this)
        requestNotificationPermissionIfNeeded()

        setupRecyclerView()

        if (viewModel.prefs.isFirstRunComplete) {
            showStatusMode()
        } else {
            showOnboardingMode()
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.prefs.isFirstRunComplete) {
            viewModel.refreshStatus()
        }
    }

    // ── UI modes ──────────────────────────────────────────────────────────────

    private fun showOnboardingMode() {
        findViewById<CardView>(R.id.cardOnboarding).visibility = View.VISIBLE
        findViewById<CardView>(R.id.cardStatus).visibility = View.GONE

        val btnScan = findViewById<Button>(R.id.btnScan)
        val btnApply = findViewById<Button>(R.id.btnApply)

        btnScan.text = getString(R.string.scan_contacts)
        btnScan.setOnClickListener { scanOrRequestPermissions() }

        btnApply.setOnClickListener {
            val selected = adapter.getSelectedContacts()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_contacts_selected), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.applyChanges(selected, completeFirstRun = true)
            }
        }
    }

    private fun showStatusMode() {
        findViewById<CardView>(R.id.cardOnboarding).visibility = View.GONE
        val cardStatus = findViewById<CardView>(R.id.cardStatus)
        cardStatus.visibility = View.VISIBLE

        val switchService = cardStatus.findViewById<SwitchMaterial>(R.id.switchService)
        switchService.isChecked = viewModel.prefs.serviceEnabled
        switchService.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setServiceEnabled(isChecked)
        }

        val btnScan = findViewById<Button>(R.id.btnScan)
        btnScan.text = getString(R.string.scan_now)
        btnScan.setOnClickListener { scanOrRequestPermissions() }

        val btnApply = findViewById<Button>(R.id.btnApply)
        btnApply.setOnClickListener {
            val selected = adapter.getSelectedContacts()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_contacts_selected), Toast.LENGTH_SHORT).show()
            } else {
                viewModel.applyChanges(selected)
            }
        }
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val tvStatusMsg = findViewById<TextView>(R.id.tvStatusMsg)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val btnScan = findViewById<Button>(R.id.btnScan)
        val btnApply = findViewById<Button>(R.id.btnApply)

        viewModel.contacts.observe(this) { contacts ->
            adapter.submitList(contacts)
            val hasResults = contacts.isNotEmpty()
            recyclerView.visibility = if (hasResults) View.VISIBLE else View.GONE
            tvEmpty.visibility = if (hasResults) View.GONE else View.VISIBLE
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
            tvStatusMsg.text = msg
            tvStatusMsg.visibility = View.VISIBLE
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            viewModel.consumeMessage()
        }

        viewModel.serviceRunning.observe(this) { running ->
            val dot = if (running) "● " else "○ "
            val label = if (running) getString(R.string.service_running) else getString(R.string.service_stopped)
            findViewById<TextView>(R.id.tvServiceStatus).text = "$dot$label"
        }

        viewModel.translatedCount.observe(this) { count ->
            findViewById<TextView>(R.id.tvTranslatedCount).text =
                resources.getQuantityString(R.plurals.contacts_translated, count, count)
        }
    }

    // ── Options menu ──────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_api_key -> { showApiKeyDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showApiKeyDialog() {
        val editText = EditText(this).apply {
            hint = getString(R.string.api_key_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(ApiKeyManager.getApiKey(this@MainActivity) ?: "")
            setPadding(48, 24, 48, 24)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.api_key_title))
            .setMessage(getString(R.string.api_key_message))
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val key = editText.text.toString().trim()
                ApiKeyManager.setApiKey(this, key)
                val msg = if (key.isNotEmpty()) getString(R.string.api_key_saved)
                          else getString(R.string.api_key_cleared)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = ContactAdapter()
        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)
        rv.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
    }

    private fun scanOrRequestPermissions() {
        if (hasContactsPermission()) {
            viewModel.scanContacts()
        } else {
            requestContacts.launch(
                arrayOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_CONTACTS,
                )
            )
        }
    }

    private fun hasContactsPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
