package com.example.ecoplant

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.TimeUnit
import android.graphics.Rect
import kotlin.toString
import android.net.Uri
import kotlin.collections.Map
import kotlin.collections.forEach
import kotlin.toString
import kotlinx.coroutines.withContext
import com.google.android.gms.location.LocationServices
import java.util.Calendar
import android.app.AlarmManager
import android.app.PendingIntent
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat

class Historique : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private val database by lazy { (application as EcoPlantApplication).database }
    private lateinit var scanBtn : LinearLayout
    private lateinit var btnSaveNotes : TextView
    private lateinit var profilBtn : LinearLayout
    private lateinit var clocheBtn : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historique)
        container = findViewById(R.id.historique_container)
        scanBtn = findViewById(R.id.scan_btn)
        profilBtn = findViewById(R.id.profil_btn)
        clocheBtn = findViewById<TextView>(R.id.cloche_btn)

        //la cloche pour les notifications
        clocheBtn.setOnClickListener {
            showNotificationSettings()
        }

        //on charge les analyses récentes depuis la base de données
        lifecycleScope.launch { loadAnalyses() }

        scanBtn.setOnClickListener { startActivity(Intent(this, Scan::class.java)) }
        profilBtn.setOnClickListener { startActivity(Intent(this, Profil::class.java)) }
    }

    /**
     * Charge les analyses récentes depuis la base de données et les affiche dans l'UI.
     */
    private suspend fun loadAnalyses() {
        withContext(Dispatchers.IO) {
            val analyses = database.recentAnalysisDao().getAll()
            withContext(Dispatchers.Main) {
                analyses.forEach { addAnalysisToView(it) }
            }
        }
    }

    /**
     * Ajoute une analyse récente à la vue.
     * @param analysis L'analyse à afficher.
     */
    private fun addAnalysisToView(analysis: RecentAnalysis) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_history, container, false)

        //image de la plante
        itemView.findViewById<ImageView>(R.id.ivPlant).apply {
            setImageBitmap(loadImageFromStorage(analysis.imagePath))
        }

        //noms et score
        itemView.findViewById<TextView>(R.id.tvScientificName).text = analysis.scientificName
        itemView.findViewById<TextView>(R.id.tvCommonName).text = analysis.commonName ?: "Nom inconnu"
        itemView.findViewById<TextView>(R.id.tvScoreValue).text = "Score : %.2f".format(analysis.score)
        itemView.findViewById<TextView>(R.id.tvTimestamp).text = getTimeAgo(analysis.timestamp)

        //scores des services écosystémiques
        val scores = loadScoresForSpecies(analysis.scientificName)
        val graphNitrogen = itemView.findViewById<ScoreGraphView>(R.id.graphNitrogen)
        val graphStructure = itemView.findViewById<ScoreGraphView>(R.id.graphStructure)
        val graphWater = itemView.findViewById<ScoreGraphView>(R.id.graphWater)
        graphNitrogen.score = scores["nitrogen_provision"] ?: 0f
        graphStructure.score = scores["soil_structuration"] ?: 0f
        graphWater.score = scores["storage_and_return_water"] ?: 0f

        //notes (vide par défaut)
        val etNotes = itemView.findViewById<EditText>(R.id.etNotes)
        etNotes.setText(analysis.notes)

        //on sauvegarde automatiquement quand l'utilisateur quitte le champ
        etNotes.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveNotesToDb(analysis, etNotes.text.toString())
            }
        }

        //ou sauvegarde quand "Done" est appuyé
        etNotes.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveNotesToDb(analysis, etNotes.text.toString())
                //retire le focus et ferme le clavier
                etNotes.clearFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(etNotes.windowToken, 0)
                true
            } else false
        }

        val btnEdit = itemView.findViewById<TextView>(R.id.btnEditNotes)
        btnEdit.setOnClickListener {
            etNotes.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(etNotes, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        val btnDelete = itemView.findViewById<TextView>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            //on supprime de la base de données
            lifecycleScope.launch(Dispatchers.IO) {
                database.recentAnalysisDao().deleteAnalysisById(analysis.id)
                //on supprime de l'UI Historique
                withContext(Dispatchers.Main) {
                    container.removeView(itemView)
                }
            }
        }

        //on sauvegarde quand l'utilisateur touche ailleurs
        itemView.setOnTouchListener { _, _ ->
            saveNotesToDb(analysis, etNotes.text.toString())
            false
        }

        //bouton pour sauvegarder les notes
        val btnSaveNotes = itemView.findViewById<TextView>(R.id.btnSaveNotes)
        btnSaveNotes.setOnClickListener {
            saveNotesToDb(analysis, etNotes.text.toString())
            etNotes.clearFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(etNotes.windowToken, 0)
        }

        //bouton pour en savoir plus qui amène àTela Botanica
        val btnLearnMore = itemView.findViewById<TextView>(R.id.btnLearnMore)
        btnLearnMore.setOnClickListener {
            //recherche sur Tela Botanica à partir du nom scientifique
            val url = "https://www.tela-botanica.org/?s=" + analysis.scientificName
            //crée un Intent en vue d'ouvrir la page dans le navigateur
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        container.addView(itemView)
    }

    /**
     * Sauvegarde les notes de l'analyse dans la base de données.
     * @param analysis L'analyse à mettre à jour.
     * @param notes Les notes à sauvegarder.
     */
    private fun saveNotesToDb(analysis: RecentAnalysis, notes: String) {
        analysis.notes = notes
        lifecycleScope.launch(Dispatchers.IO) {
            database.recentAnalysisDao().update(analysis)
        }
    }

    /**
     * Retourne une chaîne de caractères représentant le temps écoulé depuis le timestamp donné.
     * @param timestamp Le timestamp en millisecondes.
     * @return Une chaîne de caractères formatée.
     */
    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60000 -> "À l'instant"
            diff < 3600000 -> "Il y a ${diff / 60000} min"
            diff < 86400000 -> "Il y a ${diff / 3600000} h"
            else -> "Il y a ${diff / 86400000} jours"
        }
    }

    /**
     * Charge une image depuis le stockage local.
     * @param path Le chemin de l'image.
     * @return Un Bitmap de l'image chargée.
     */
    private fun loadImageFromStorage(path: String) = BitmapFactory.decodeFile(path)

    /**
     * Intercepte les événements de toucher pour fermer le clavier si l'utilisateur touche en dehors d'un EditText.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is EditText) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    view.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * Charge les scores des services écosystémiques pour une espèce donnée à partir du fichier csv.
     * @param species Le nom de l'espèce.
     * @return Un Map associant les services écosystémiques à leurs scores.
     */
    private fun loadScoresForSpecies(species: String): Map<String, Float> {
        val scores = mutableMapOf<String, Float>()
        try {
            assets.open("data.csv").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("service")) return@forEach
                    val tokens = line.split(";")
                    if (tokens.size >= 3) {
                        val service = tokens[0].trim()
                        val csvSpecies = tokens[1].trim()
                        val value = tokens[2].trim().toFloatOrNull() ?: 0f
                        if (csvSpecies.equals(species, ignoreCase = true)) {
                            scores[service] = value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return scores
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
     * @param hour L'heure à laquelle le rappel doit être envoyé.
     * @param minute La minute à laquelle le rappel doit être envoyé.
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