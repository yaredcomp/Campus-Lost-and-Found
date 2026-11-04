package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Item(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerEmail: String = "",
    val ownerName: String = "",
    val type: String = "LOST", // "LOST" or "FOUND"
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val imageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
