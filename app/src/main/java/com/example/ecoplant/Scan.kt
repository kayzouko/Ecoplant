package com.example.ecoplant

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Scan : AppCompatActivity() {

    companion object {
        private const val CAMERA_REQUEST = 1001
        private const val PROJECT = "all"
        private const val API_KEY = "2b10EUr53rAFRsd5tUinlcPO"
        private const val API_URL = "https://my-api.plantnet.org/v2/identify/$PROJECT?api-key=$API_KEY&lang=fr"
    }

    private lateinit var btnPrendrePhoto: LinearLayout
    private lateinit var recentContainer: LinearLayout
    private lateinit var historiqueBtn: LinearLayout
    private lateinit var mapBtn: LinearLayout
    private lateinit var profilBtn: LinearLayout
    private lateinit var photoFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        //initialisation des vues
        btnPrendrePhoto = findViewById(R.id.prendre_une_photo)
        historiqueBtn = findViewById(R.id.historique_btn)
        mapBtn = findViewById(R.id.map_btn)
        profilBtn = findViewById(R.id.profil_btn)

        //on récupère le parent LinearLayout du ScrollView
        val scroll = findViewById<ScrollView>(R.id.scrollView)
        val content = scroll.getChildAt(0) as LinearLayout

        //TextView "Récentes Analyses"
        val titleRecents = findViewById<TextView>(R.id.recentesAnalyses)
        //on crée un conteneur neuf juste sous ce titre
        recentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(0, 0, 0, 41.dp)
            }
        }
        //on enlève les exemples statiques
        //content.removeView(findViewById(R.id.premierExemple))
        //content.removeView(findViewById(R.id.deuxiemeExemple))
        //on insère notre conteneur vide après le titre
        val titleIndex = content.indexOfChild(titleRecents)
        content.addView(recentContainer, titleIndex + 1)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 123)
        }

        //caméra
        btnPrendrePhoto.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePicture ->
                if (takePicture.resolveActivity(packageManager) != null) {
                    photoFile = File.createTempFile("photo_", ".jpg", cacheDir)
                    val photoURI = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.provider",
                        photoFile
                    )
                    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePicture, CAMERA_REQUEST)
                } else {
                    Toast.makeText(this, "Pas d'appareil photo trouvé", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //footer navigation
        //historiqueBtn.setOnClickListener { startActivity(Intent(this, Historique::class.java)) }
        //mapBtn.setOnClickListener       { startActivity(Intent(this, Map::class.java)) }
        //profilBtn.setOnClickListener    { startActivity(Intent(this, Profil::class.java)) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeFile(photoFile.absolutePath, options)?.let {
                sendToPlantNet(it)
            } ?: Toast.makeText(this, "Erreur de décodage", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendToPlantNet(bitmap: Bitmap) {
        lifecycleScope.launch {
            val file = File(cacheDir, "upload.jpg").apply {
                FileOutputStream(this).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            }

            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("organs", "auto")
                .addFormDataPart(
                    "images",
                    "plant.jpg",
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .build()

            withContext(Dispatchers.IO) {
                try {
                    client.newCall(request).execute().use { response ->
                        handleApiResponse(response, bitmap)
                    }
                } catch (e: IOException) {
                    showError("Erreur réseau : ${e.message}")
                } catch (e: JSONException) {
                    showError("Réponse API invalide : ${e.message}")
                } catch (e: Exception) {
                    showError("Erreur inattendue : ${e.localizedMessage}")
                } finally {
                    file.delete()
                    bitmap.recycle()
                }
            }
        }
    }

    private suspend fun handleApiResponse(response: Response, bitmap: Bitmap) {
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code} - ${response.message}")
        }

        val json = JSONObject(response.body!!.string())
        if (!json.has("results")) {
            throw JSONException("Aucun résultat trouvé")
        }

        val firstResult = json.getJSONArray("results").getJSONObject(0)
        val species = firstResult.getJSONObject("species")
        val sciName = species.getString("scientificNameWithoutAuthor")

        withContext(Dispatchers.Main) {
            addRecentAnalysis(bitmap, sciName)
            Toast.makeText(
                this@Scan,
                "Identification réussie : $sciName",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@Scan, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //fonction pour créer et ajouter dans Récentes Analyses
    private fun addRecentAnalysis(photo: Bitmap, speciesName: String) {
        val thumbnail = Bitmap.createScaledBitmap(photo, 200, 200, true)

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = getDrawable(R.drawable.rectangle_blanc)
            elevation = 4f
            setPadding(21.dp, 14.dp, 21.dp, 14.dp)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(31.dp, 0, 31.dp, 25.dp)
            }
        }

        ImageView(this).apply {
            setImageBitmap(thumbnail)
            layoutParams = LinearLayout.LayoutParams(80.dp, 80.dp).also {
                it.marginEnd = 24.dp
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }.let { item.addView(it) }

        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@Scan).apply {
                text = speciesName
                textSize = 16f
                setTextColor(0xFF000000.toInt())
            })
        }.let { item.addView(it) }

        recentContainer.addView(item, 0)
    }

    //extension pour convertir dp en px
    private val Int.dp: Int get() =
        (this * resources.displayMetrics.density).toInt()

    private fun saveBitmapToStorage(bitmap: Bitmap): String {
        val storageDir = File(getExternalFilesDir(null), "plant_images")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(storageDir, "PLANT_${timeStamp}.jpg")

        try {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            return imageFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }

    // Et la méthode complémentaire pour charger l'image
    private fun loadImageFromStorage(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
