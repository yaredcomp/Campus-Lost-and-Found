package com.example.di

import android.content.Context
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.data.repository.ItemRepository
import com.example.data.repository.StorageRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage

/**
 * Dependency container providing singletons for Firebase services and Repositories.
 */
class AppModule(private val context: Context) {

    val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(
            context.getString(R.string.firestore_database_id)
        )
    }

    val storage: FirebaseStorage by lazy {
        val app = com.google.firebase.FirebaseApp.getInstance()
        val bucket = app.options.storageBucket
        if (!bucket.isNullOrBlank()) {
            FirebaseStorage.getInstance(app, "gs://$bucket")
        } else {
            Firebase.storage
        }
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(auth)
    }

    val itemRepository: ItemRepository by lazy {
        ItemRepository(firestore)
    }

    val storageRepository: StorageRepository by lazy {
        StorageRepository(storage)
    }
}
