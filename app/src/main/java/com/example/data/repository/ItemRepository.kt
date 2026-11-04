package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Item
import com.example.util.Constants
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ItemRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth
    private val itemsCollection = db.collection(Constants.COLLECTION_ITEMS)

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before performing item operations")
    }

    /**
     * Real-time stream of all lost and found items.
     * Uses callbackFlow wrapping addSnapshotListener with cleanup on awaitClose.
     */
    fun observeAllItems(): Flow<List<Item>> = callbackFlow {
        val registration = itemsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, itemsCollection.path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Item::class.java)
                    }
                    trySend(items)
                }
            }

        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of items posted by a specific user.
     */
    fun observeUserItems(ownerId: String): Flow<List<Item>> = callbackFlow {
        val registration = itemsCollection
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, itemsCollection.path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Item::class.java)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                    trySend(items)
                }
            }

        awaitClose { registration.remove() }
    }

    /**
     * Observe a single item by its ID.
     */
    fun observeItemById(itemId: String): Flow<Item?> = callbackFlow {
        val docRef = itemsCollection.document(itemId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.READ, docRef.path)
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Item::class.java))
        }

        awaitClose { registration.remove() }
    }

    /**
     * Creates a new item in the Firestore collection.
     */
    suspend fun createItem(
        type: String,
        title: String,
        description: String,
        location: String,
        imageUrl: String
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val user = auth.currentUser
            val email = user?.email?.ifBlank { null } ?: "${uid.take(8)}@campus.edu"
            val displayName = user?.displayName?.ifBlank { null } ?: "Campus Student"
            val docRef = itemsCollection.document()
            val payload = hashMapOf<String, Any>(
                "ownerId" to uid,
                "ownerEmail" to email,
                "ownerName" to displayName,
                "type" to type,
                "title" to title.trim(),
                "description" to description.trim(),
                "location" to location.trim(),
                "imageUrl" to imageUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, itemsCollection.path)
            Result.failure(e)
        }
    }

    /**
     * Updates an existing item.
     */
    suspend fun updateItem(item: Item): Result<Unit> {
        val uid = requireUserId()
        if (item.ownerId != uid) {
            return Result.failure(IllegalStateException("Unauthorized: Only the item owner can update this post"))
        }
        val docRef = itemsCollection.document(item.id)
        return try {
            val payload = hashMapOf<String, Any>(
                "ownerId" to uid,
                "ownerEmail" to item.ownerEmail,
                "ownerName" to item.ownerName,
                "type" to item.type,
                "title" to item.title.trim(),
                "description" to item.description.trim(),
                "location" to item.location.trim(),
                "imageUrl" to item.imageUrl
            )
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Deletes an item by its ID.
     */
    suspend fun deleteItem(itemId: String, ownerId: String): Result<Unit> {
        val uid = requireUserId()
        if (ownerId != uid) {
            return Result.failure(IllegalStateException("Unauthorized: Only the item owner can delete this post"))
        }
        val docRef = itemsCollection.document(itemId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
