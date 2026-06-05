package com.namesupport.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.namesupport.R
import com.namesupport.model.ContactItem

class ContactAdapter : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private val items = mutableListOf<ContactItem>()
    private val checked = mutableMapOf<Long, Boolean>()
    private val editedSuggestions = mutableMapOf<Long, String>()

    fun submitList(contacts: List<ContactItem>) {
        items.clear()
        items.addAll(contacts)
        editedSuggestions.clear()
        contacts.forEach { if (!checked.containsKey(it.id)) checked[it.id] = true }
        val currentIds = contacts.map { it.id }.toSet()
        checked.keys.retainAll(currentIds)
        notifyDataSetChanged()
    }

    fun getSelectedContacts(): List<ContactItem> =
        items.filter { checked[it.id] == true }
            .map { contact ->
                val edited = editedSuggestions[contact.id]
                if (!edited.isNullOrBlank()) contact.copy(suggestion = edited)
                else contact
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        private val tvOriginal: TextView = view.findViewById(R.id.tvOriginalName)
        private val etTranslit: TextInputEditText = view.findViewById(R.id.etTransliteration)
        private var textWatcher: TextWatcher? = null

        fun bind(contact: ContactItem) {
            tvOriginal.text = contact.displayName

            // Remove old watcher before setText to prevent spurious callbacks on recycled views
            textWatcher?.let { etTranslit.removeTextChangedListener(it) }
            etTranslit.setText(editedSuggestions[contact.id] ?: contact.suggestion)

            val newWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    editedSuggestions[contact.id] = s?.toString() ?: ""
                }
            }
            textWatcher = newWatcher
            etTranslit.addTextChangedListener(newWatcher)

            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = checked[contact.id] ?: true
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                checked[contact.id] = isChecked
            }
        }
    }
}
