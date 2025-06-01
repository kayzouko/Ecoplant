package com.example.ecoplant

import android.content.Intent
import android.health.connect.datatypes.ExerciseRoute
import android.health.connect.datatypes.ExerciseRoute.Location
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.location.LocationServices

class Profil : AppCompatActivity() {

    private lateinit var plantesCount: TextView
    private lateinit var especesCount: TextView
    private lateinit var endroitsCount: TextView
    private lateinit var userName: TextView
    private lateinit var userBio: TextView
    private lateinit var logoutLayout: LinearLayout
    private lateinit var helpLayout: LinearLayout
    private lateinit var infoPerso: LinearLayout
    private lateinit var notifications : LinearLayout

    private val database by lazy {
        (application as EcoPlantApplication).database
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil)

        // Initialisation des vues
        plantesCount = findViewById(R.id.r4taiigr8or)
        especesCount = findViewById(R.id.rriaxr7z66u7)
        endroitsCount = findViewById(R.id.rb5l1qsrh19c)
        userName = findViewById(R.id.rj6ywjgq2nq9)
        userBio = findViewById(R.id.r0k6xmh0anwyk)
        logoutLayout = findViewById(R.id.r7g3lz4te4xm)
        helpLayout = findViewById(R.id.r1dt7atjbvql)
        infoPerso = findViewById(R.id.info_perso)
        notifications = findViewById(R.id.notifications)

        // Navigation footer
        setupFooterNavigation()

        // Charger les données
        loadUserData()
        loadStatistics()

        // Gestion de la déconnexion
        logoutLayout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, SeConnecter::class.java))
            finishAffinity()
        }

        // Gestion de l'aide
        helpLayout.setOnClickListener {
            sendSupportEmail()
        }

        infoPerso.setOnClickListener {
            showUserInfoDialog()
        }

        notifications.setOnClickListener {
            showNotificationSettings()
        }
    }

    private fun setupFooterNavigation() {
        val scanBtn = findViewById<LinearLayout>(R.id.scan_btn)
        val historiqueBtn = findViewById<LinearLayout>(R.id.historique_btn)
        val mapBtn = findViewById<LinearLayout>(R.id.map_btn)
        val profilBtn = findViewById<LinearLayout>(R.id.profil_btn)

        scanBtn.setOnClickListener {
            startActivity(Intent(this, Scan::class.java))
            finish()
        }

        historiqueBtn.setOnClickListener {
            startActivity(Intent(this, Historique::class.java))
            finish()
        }

        profilBtn.setOnClickListener {
            //déjà sur le profil
        }
    }

    private fun loadUserData() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            userName.text = it.displayName ?: "Utilisateur EcoPlant"
            userBio.text = it.email ?: "Passionné de plantes"
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch(Dispatchers.IO) {
            val analyses = database.recentAnalysisDao().getAll()

            // 1. Nombre total de plantes
            val totalPlants = analyses.size

            // 2. Nombre d'espèces différentes
            val distinctSpecies = analyses.map { it.scientificName }.distinct().size

            // 3. Nombre d'endroits (placeholder pour l'instant)
            val locationsCount = 0

            withContext(Dispatchers.Main) {
                plantesCount.text = totalPlants.toString()
                especesCount.text = distinctSpecies.toString()
                endroitsCount.text = locationsCount.toString()
            }
        }
    }

    private fun sendSupportEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("nizar12354@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Support EcoPlant")
            putExtra(Intent.EXTRA_TEXT, "Bonjour,\n\nJ'ai besoin d'aide concernant l'application EcoPlant...")
        }

        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, "Envoyer un email"))
        } else {
            Toast.makeText(this, "Aucune application email installée", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUserInfoDialog() {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "Non connecté", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Informations personnelles")
            .setView(R.layout.dialog_user_info)
            .setPositiveButton("Sauvegarder", null) // On définit le listener plus tard
            .setNegativeButton("Annuler", null)
            .create()

        dialog.setOnShowListener {
            val nameField = dialog.findViewById<EditText>(R.id.edit_name)
            val emailField = dialog.findViewById<TextView>(R.id.edit_email)

            nameField?.setText(user.displayName ?: "")
            emailField?.text = user.email ?: "Non renseigné"

            // Gérer le clic sur Sauvegarder ici
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = nameField?.text.toString()
                if (newName.isNotBlank()) {
                    updateUserName(newName)
                    dialog.dismiss() // Fermer le dialogue après sauvegarde
                } else {
                    nameField?.error = "Le nom ne peut pas être vide"
                }
            }
        }

        dialog.show()
    }

    private fun updateUserName(newName: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profil mis à jour", Toast.LENGTH_SHORT).show()
                    //rafraîchir l'affichage
                    findViewById<TextView>(R.id.rj6ywjgq2nq9).text = newName
                } else {
                    Toast.makeText(this, "Erreur: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

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

                // Planifier les notifications
                if (switchReminders?.isChecked == true) {
                    //WaterReminders(timePicker?.hour ?: 9, timePicker?.minute ?: 0)
                }

                Toast.makeText(this, "Préférences enregistrées", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}