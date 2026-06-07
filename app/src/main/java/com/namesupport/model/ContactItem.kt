package com.namesupport.model

data class ContactItem(
    val id: Long,
    val displayName: String,
    val suggestion: String,
    val isWhatsAppOnly: Boolean = false,
)
