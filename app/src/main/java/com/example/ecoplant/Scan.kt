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
import android.view.LayoutInflater
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.withContext
import com.google.android.gms.location.LocationServices
import java.util.Calendar
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat

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
    private lateinit var clocheBtn : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        //initialisation des vues
        btnPrendrePhoto = findViewById(R.id.prendre_une_photo)
        historiqueBtn = findViewById(R.id.historique_btn)
        mapBtn = findViewById(R.id.map_btn)
        profilBtn = findViewById(R.id.profil_btn)
        clocheBtn = findViewById<TextView>(R.id.cloche_btn)

        //la cloche pour les notifications
        clocheBtn.setOnClickListener {
            showNotificationSettings()
        }

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

        //on insère notre conteneur vide après le titre
        val titleIndex = content.indexOfChild(titleRecents)
        content.addView(recentContainer, titleIndex + 1)

        //on observe les analyses récentes en direct
        database.recentAnalysisDao().getAllLive().observe(this) { analyses ->
            recentContainer.removeAllViews()
            analyses.forEach { analysis ->
                addAnalysisToView(analysis)
            }
        }

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
        historiqueBtn.setOnClickListener { startActivity(Intent(this, Historique::class.java)) }
        //mapBtn.setOnClickListener       { startActivity(Intent(this, Map::class.java)) }
        profilBtn.setOnClickListener    { startActivity(Intent(this, Profil::class.java)) }
    }

    override fun onResume() {
        super.onResume()
    }

    /**
     * Gère le résultat de la capture photo.
     * @param requestCode Le code de la requête.
     * @param resultCode Le code de résultat de l'activité.
     * @param data Les données retournées par l'activité.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeFile(photoFile.absolutePath, options)?.let {
                sendToPlantNet(it)
            } ?: Toast.makeText(this, "Erreur de décodage", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Envoie l'image à l'API PlantNet pour identification.
     * @param bitmap L'image capturée par la caméra.
     */
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

    /**
     * Gère la réponse de l'API PlantNet.
     * @param response La réponse HTTP de l'API.
     * @param bitmap L'image capturée par la caméra.
     */
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
        val commonNames = species.getJSONArray("commonNames")
        val frenchName = if (commonNames.length() > 0) commonNames.getString(0) else null
        val score = firstResult.getDouble("score")

        withContext(Dispatchers.Main) {
            addRecentAnalysis(bitmap, sciName, frenchName, score)
            Toast.makeText(
                this@Scan,
                "Identification réussie : $sciName",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Affiche un message d'erreur dans un Toast.
     * @param message Le message d'erreur à afficher.
     */
    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@Scan, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Gère les résultats des demandes de permissions.
     * @param requestCode Le code de la requête de permission.
     * @param permissions Les permissions demandées.
     * @param grantResults Les résultats de la demande de permission.
     */
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

    /**
     * Ajoute une analyse récente à la base de données et à l'interface utilisateur.
     * @param photo La photo de la plante.
     * @param speciesName Le nom scientifique de l'espèce.
     * @param commonName Le nom commun de l'espèce, ou null si inconnu.
     * @param score Le score d'identification de l'espèce.
     */
    private fun addRecentAnalysis(photo: Bitmap, speciesName: String, commonName: String?, score: Double) {

        // Redimensionnement de la photo
        val thumbnail = Bitmap.createScaledBitmap(photo, 80.dp, 80.dp, true)

        // Sauvegarde en base de données
        saveAnalysisToDb(RecentAnalysis(
            scientificName = speciesName,
            commonName = commonName,
            score = score,
            imagePath = saveBitmapToStorage(thumbnail)
        ))
    }

    //extension pour convertir dp en px
    private val Int.dp: Int get() =
        (this * resources.displayMetrics.density).toInt()

    /**
     * Sauvegarde une image Bitmap dans le stockage externe et retourne le chemin du fichier.
     * @param bitmap L'image à sauvegarder.
     * @return Le chemin du fichier image sauvegardé, ou une chaîne vide en cas d'erreur.
     */
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

    /**
     * Accède à la base de données de l'application.
     */
    private val database by lazy {
        (application as EcoPlantApplication).database
    }

    /**
     * Enregistre une analyse récente dans la base de données.
     * @param analysis L'analyse à enregistrer.
     */
    private fun saveAnalysisToDb(analysis: RecentAnalysis) {
        lifecycleScope.launch(Dispatchers.IO) {
            database.recentAnalysisDao().insert(analysis)
        }
    }

    /**
     * Ajoute une vue d'analyse récente à l'interface utilisateur.
     * @param analysis L'analyse récente à afficher.
     */
    private fun addAnalysisToView(analysis: RecentAnalysis) {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_recent_analysis, recentContainer, false)

        val bitmap = loadImageFromStorage(analysis.imagePath)
        if (bitmap != null) {
            itemView.findViewById<ImageView>(R.id.ivPlant).setImageBitmap(bitmap)
        }

        itemView.findViewById<TextView>(R.id.tvScientificName).text = analysis.scientificName
        itemView.findViewById<TextView>(R.id.tvCommonName).text = analysis.commonName ?: "Nom commun inconnu"
        itemView.findViewById<TextView>(R.id.tvScoreValue).text = "%.2f".format(analysis.score)

        recentContainer.addView(itemView, 0)
    }

    //méthode complémentaire pour charger l'image
    private fun loadImageFromStorage(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Affiche une boîte de dialogue pour gérer les paramètres de notification.
     */
    private fun showNotificationSettings() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Gestion des notifications")
            .setView(R.layout.dialog_notifications)
            .setPositiveButton("Enregistrer", null)
            .setNegativeButton("Annuler", null)
            .create()

        dialog.setOnShowListener {
            val switchTips = dialog.findViewById<SwitchCompat>(R.id.switch_tips)
            val switchReminders = dialog.findViewById<SwitchCompat>(R.id.switch_reminders)
            val frequencySpinner = dialog.findViewById<Spinner>(R.id.frequency_spinner)
            val timePicker = dialog.findViewById<TimePicker>(R.id.time_picker)

            //charger les préférences existantes
            val prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE)
            switchTips?.isChecked = prefs.getBoolean("plant_tips", true)
            switchReminders?.isChecked = prefs.getBoolean("water_reminders", true)

            //bouton Enregistrer
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                with(prefs.edit()) {
                    putBoolean("plant_tips", switchTips?.isChecked ?: true)
                    putBoolean("water_reminders", switchReminders?.isChecked ?: true)
                    apply()
                }

                //planifier les notifications
                if (switchReminders?.isChecked == true) {
                    waterReminders(timePicker?.hour ?: 9, timePicker?.minute ?: 0)
                }

                Toast.makeText(this, "Préférences enregistrées", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    /**
     * Planifie les rappels d'arrosage des plantes à une heure spécifique.
     * @param hour L'heure à laquelle le rappel doit être déclenché.
     * @param minute La minute à laquelle le rappel doit être déclenché.
     */
    private fun waterReminders(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        //pour définir l'heure de l'alarme
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        //si l'heure est passée, le déclencher le lendemain
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}
