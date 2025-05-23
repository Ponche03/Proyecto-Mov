package Services

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.firebase.storage.StorageException

sealed class FirebaseUploadResult {
    data class Success(val downloadUrl: String) : FirebaseUploadResult()
    data class Failure(val errorMessage: String, val exception: Exception? = null) : FirebaseUploadResult()
}

class FirebaseStorageService {
    private val storageInstance = FirebaseStorage.getInstance()

    suspend fun uploadFileSuspend(
        fileUri: Uri,
        storagePath: String
    ): FirebaseUploadResult = suspendCancellableCoroutine { continuation ->
        val fileName = "${UUID.randomUUID()}"
        val storageRef = storageInstance.reference.child("$storagePath/$fileName")

        val uploadTask = storageRef.putFile(fileUri)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                if (continuation.isActive) {
                    continuation.resume(FirebaseUploadResult.Success(downloadUri.toString()))
                }
            }.addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resume(FirebaseUploadResult.Failure("Error al obtener URL de descarga", exception))
                }
            }
        }.addOnFailureListener { exception ->
            if (continuation.isActive) {
                val errorMessage = if (exception is StorageException && exception.errorCode == StorageException.ERROR_CANCELED) {
                    "Subida cancelada"
                } else {
                    "Error al subir archivo: ${exception.message}"
                }
                continuation.resume(FirebaseUploadResult.Failure(errorMessage, exception))
            }
        }
        // No es necesario para progreso en este contexto suspendible, pero podría añadirse con channels
        // .addOnProgressListener { ... }

        continuation.invokeOnCancellation {
            if (uploadTask.isInProgress) {
                uploadTask.cancel()
            }
        }
    }
}