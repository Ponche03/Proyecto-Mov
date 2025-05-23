
package Services
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class FirebaseStorageService {

    private val storageInstance = FirebaseStorage.getInstance()

    fun uploadFile(
        fileUri: Uri,
        storagePath: String,
        onSuccess: (downloadUrl: String) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {

        val fileName = "${UUID.randomUUID()}"
        val storageRef = storageInstance.reference.child("$storagePath/$fileName")

        storageRef.putFile(fileUri)
            .addOnSuccessListener { taskSnapshot ->
                // La subida fue exitosa, ahora obtenemos la URL de descarga
                taskSnapshot.storage.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                    .addOnFailureListener { exception ->
                        // Falló al obtener la URL de descarga
                        onFailure("Error al obtener URL de descarga: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                onFailure("Error al subir imagen: ${exception.message}")
            }
            .addOnProgressListener { taskSnapshot ->

            }
    }
}