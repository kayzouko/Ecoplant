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

class Historique : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private val database by lazy { (application as EcoPlantApplication).database }
    private lateinit var scanBtn : LinearLayout
    private lateinit var btnSaveNotes : TextView
    private lateinit var profilBtn : LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historique)
        container = findViewById(R.id.historique_container)
        scanBtn = findViewById(R.id.scan_btn)
        profilBtn = findViewById(R.id.profil_btn)

        lifecycleScope.launch { loadAnalyses() }
        // Optionnel : ajouter un bouton pour rafraîchir l'historique
        //findViewById<TextView>(R.id.tvRefresh).setOnClickListener {
        //    container.removeAllViews() // Efface l'historique actuel
        //    lifecycleScope.launch { loadAnalyses() } // Recharge les analyses
        //}
        scanBtn.setOnClickListener { startActivity(Intent(this, Scan::class.java)) }
        profilBtn.setOnClickListener { startActivity(Intent(this, Profil::class.java)) }
    }

    private suspend fun loadAnalyses() {
        withContext(Dispatchers.IO) {
            val analyses = database.recentAnalysisDao().getAll()
            withContext(Dispatchers.Main) {
                analyses.forEach { addAnalysisToView(it) }
            }
        }
    }

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

        val btnSaveNotes = itemView.findViewById<TextView>(R.id.btnSaveNotes)
        btnSaveNotes.setOnClickListener {
            saveNotesToDb(analysis, etNotes.text.toString())
            etNotes.clearFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(etNotes.windowToken, 0)
        }

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

    private fun saveNotesToDb(analysis: RecentAnalysis, notes: String) {
        analysis.notes = notes
        lifecycleScope.launch(Dispatchers.IO) {
            database.recentAnalysisDao().update(analysis)
        }
    }

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

    private fun loadImageFromStorage(path: String) = BitmapFactory.decodeFile(path)

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
}