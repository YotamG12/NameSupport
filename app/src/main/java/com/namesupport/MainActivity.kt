package com.namesupport

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.namesupport.ui.ContactAdapter
import com.namesupport.ui.MainViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: ContactAdapter

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            viewModel.scanContacts()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnScan = findViewById<Button>(R.id.btnScan)
        val btnApply = findViewById<Button>(R.id.btnApply)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = ContactAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        btnScan.setOnClickListener {
            if (hasContactsPermission()) {
                viewModel.scanContacts()
            } else {
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                    )
                )
            }
        }

        btnApply.setOnClickListener {
            val selected = adapter.getSelectedContacts()
            if (selected.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_contacts_selected), Toast.LENGTH_SHORT)
                    .show()
            } else {
                viewModel.applyChanges(selected)
            }
        }

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
            tvStatus.text = msg
            tvStatus.visibility = View.VISIBLE
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            viewModel.consumeMessage()
        }
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
}
