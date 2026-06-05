package com.namesupport.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.namesupport.R
import com.namesupport.data.AppPreferences
import com.namesupport.data.db.AppDatabase
import com.namesupport.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = AppPreferences(this)
        val switchMonitoring = findViewById<MaterialSwitch>(R.id.switchMonitoring)
        val rowRescan = findViewById<View>(R.id.btnRescanDismissed)

        lifecycleScope.launch {
            switchMonitoring.isChecked = prefs.isMonitoringEnabled.first()
        }

        switchMonitoring.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                prefs.setMonitoringEnabled(isChecked)
                if (isChecked) {
                    WorkManagerScheduler.enqueue(this@SettingsActivity)
                } else {
                    WorkManagerScheduler.cancel(this@SettingsActivity)
                }
            }
        }

        rowRescan.setOnClickListener {
            lifecycleScope.launch {
                AppDatabase.getInstance(this@SettingsActivity)
                    .contactRecordDao()
                    .deleteAllDismissed()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
