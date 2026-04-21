package com.namesupport.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.namesupport.R
import com.namesupport.model.ContactItem

class ContactAdapter : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    private val items = mutableListOf<ContactItem>()
    private val checked = mutableMapOf<Long, Boolean>()

    fun submitList(contacts: List<ContactItem>) {
        items.clear()
        items.addAll(contacts)
        // Default new contacts to checked; preserve state of contacts already shown
        contacts.forEach { if (!checked.containsKey(it.id)) checked[it.id] = true }
        // Remove stale entries for contacts no longer in the list
        val currentIds = contacts.map { it.id }.toSet()
        checked.keys.retainAll(currentIds)
        notifyDataSetChanged()
    }

    fun getSelectedContacts(): List<ContactItem> =
        items.filter { checked[it.id] == true }

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
        private val tvTranslit: TextView = view.findViewById(R.id.tvTransliteration)

        fun bind(contact: ContactItem) {
            tvOriginal.text = contact.displayName
            tvTranslit.text = "→ ${contact.suggestion}"  // → Suggestion

            // Clear listener before setting isChecked to avoid spurious callbacks
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = checked[contact.id] ?: true
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                checked[contact.id] = isChecked
            }
        }
    }
}
