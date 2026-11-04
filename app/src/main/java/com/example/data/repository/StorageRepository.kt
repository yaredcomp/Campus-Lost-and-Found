package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.util.Constants
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream

class StorageRepository(
    private val storage: FirebaseStorage = Firebase.storage
) {
    /**
     * Uploads an image from content URI with real-time upload progress (0f..1f).
     * If Cloud Storage is not yet enabled in the Firebase Console, safely falls back
     * to a compressed Base64 data URL so item posting never breaks for the user.
     */
    fun uploadItemImage(
        context: Context,
        imageUri: Uri
    ): Flow<UploadStatus> = callbackFlow {
        val uid = Firebase.auth.currentUser?.uid ?: "anonymous"
        val timestamp = System.currentTimeMillis()
        val imageRef = storage.reference
            .child(Constants.STORAGE_PATH_ITEMS)
            .child(uid)
            .child("$timestamp.jpg")

        trySend(UploadStatus.Progress(0.1f))

        try {
            val uploadTask = imageRef.putFile(imageUri)

            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = if (taskSnapshot.totalByteCount > 0) {
                    taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount.toFloat()
                } else {
                    0.2f
                }
                trySend(UploadStatus.Progress(progress.coerceIn(0.1f, 0.95f)))
            }

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    trySend(UploadStatus.Success(downloadUri.toString()))
                    close()
                }.addOnFailureListener { downloadError ->
                    // Fall back to compressed base64 if download URL fails
                    Log.w(TAG, "Download URL resolution failed, generating fallback: ${downloadError.message}")
                    val fallbackUrl = encodeImageUriToBase64(context, imageUri)
                    if (fallbackUrl != null) {
                        trySend(UploadStatus.Success(fallbackUrl))
                    } else {
                        trySend(UploadStatus.Error(downloadError.localizedMessage ?: "Failed to get download URL"))
                    }
                    close()
                }
            }.addOnFailureListener { uploadError ->
                Log.w(TAG, "Storage upload failed (possibly unenabled storage), creating local fallback: ${uploadError.message}")
                val fallbackUrl = encodeImageUriToBase64(context, imageUri)
                if (fallbackUrl != null) {
                    trySend(UploadStatus.Success(fallbackUrl))
                } else {
                    trySend(UploadStatus.Error(uploadError.localizedMessage ?: "Failed to upload image"))
                }
                close()
            }

            awaitClose {
                if (uploadTask.isInProgress) {
                    uploadTask.cancel()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during image upload initialization", e)
            val fallbackUrl = encodeImageUriToBase64(context, imageUri)
            if (fallbackUrl != null) {
                trySend(UploadStatus.Success(fallbackUrl))
            } else {
                trySend(UploadStatus.Error(e.localizedMessage ?: "Upload error"))
            }
            close()
        }
    }

    private fun encodeImageUriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Downscale to max 800px on longest side for lightweight data storage
            val maxDimension = 800
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                if (width > height) maxDimension.toFloat() / width.toFloat() else maxDimension.toFloat() / height.toFloat()
            } else 1.0f

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    (width * scale).toInt(),
                    (height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode image to base64 fallback", e)
            null
        }
    }

    sealed class UploadStatus {
        data class Progress(val progress: Float) : UploadStatus()
        data class Success(val imageUrl: String) : UploadStatus()
        data class Error(val message: String) : UploadStatus()
    }

    companion object {
        private const val TAG = "StorageRepository"
    }
}
