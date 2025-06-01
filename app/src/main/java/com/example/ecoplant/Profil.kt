package com.example.ecoplant

import android.app.PendingIntent
import android.content.Context
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
import java.util.Calendar
import android.app.AlarmManager
import android.view.View
import android.graphics.Color
import android.text.format.Formatter.formatFileSize
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.SeekBar
import java.io.File

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
    private lateinit var clocheBtn : TextView
    private lateinit var appSettingsLayout: LinearLayout

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
        clocheBtn = findViewById<TextView>(R.id.cloche_btn)
        appSettingsLayout = findViewById<LinearLayout>(R.id.parametres_app)

        //la cloche pour les notifications
        clocheBtn.setOnClickListener {
            showNotificationSettings()
        }

        //navigation footer
        setupFooterNavigation()

        //charger les données
        loadUserData()
        loadStatistics()

        //gestion des informations personnelles
        infoPerso.setOnClickListener {
            showUserInfoDialog()
        }

        //gestion des notifications
        notifications.setOnClickListener {
            showNotificationSettings()
        }

        //gestion des paramètres de l'application
        appSettingsLayout.setOnClickListener {
            showAppSettingsDialog()
        }

        //gestion de l'aide
        helpLayout.setOnClickListener {
            sendSupportEmail()
        }

        //gestion de la déconnexion
        logoutLayout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, SeConnecter::class.java))
            finishAffinity()
        }
    }

    /**
     * Configure la navigation dans le footer de l'application.
     */
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

    /**
     * Charge les données de l'utilisateur connecté depuis Firebase.
     * Affiche le nom d'utilisateur et la bio dans les TextViews correspondants.
     */
    private fun loadUserData() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            userName.text = it.displayName ?: "Utilisateur EcoPlant"
            userBio.text = it.email ?: "Passionné de plantes"
        }
    }

    /**
     * Charge les statistiques de l'utilisateur depuis la base de données.
     * Affiche le nombre total de plantes, d'espèces différentes et d'endroits.
     */
    private fun loadStatistics() {
        lifecycleScope.launch(Dispatchers.IO) {
            val analyses = database.recentAnalysisDao().getAll()

            //nombre total de plantes
            val totalPlants = analyses.size

            //nombre d'espèces différentes
            val distinctSpecies = analyses.map { it.scientificName }.distinct().size

            //nombre d'endroits (placeholder pour l'instant)
            val locationsCount = 0

            withContext(Dispatchers.Main) {
                plantesCount.text = totalPlants.toString()
                especesCount.text = distinctSpecies.toString()
                endroitsCount.text = locationsCount.toString()
            }
        }
    }

    /**
     * Envoie un email de support à l'adresse spécifiée.
     * Utilise un Intent pour ouvrir l'application email par défaut de l'utilisateur.
     */
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

    /**
     * Affiche un dialogue pour modifier les informations personnelles de l'utilisateur.
     * Permet de changer le nom d'utilisateur et de voir l'email.
     */
    private fun showUserInfoDialog() {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "Non connecté", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Informations personnelles")
            .setView(R.layout.dialog_user_info)
            .setPositiveButton("Sauvegarder", null)
            .setNegativeButton("Annuler", null)
            .create()

        dialog.setOnShowListener {
            val nameField = dialog.findViewById<EditText>(R.id.edit_name)
            val emailField = dialog.findViewById<TextView>(R.id.edit_email)

            nameField?.setText(user.displayName ?: "")
            emailField?.text = user.email ?: "Non renseigné"

            //on gère le clic sur Sauvegarder ici
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = nameField?.text.toString()
                if (newName.isNotBlank()) {
                    updateUserName(newName)
                    dialog.dismiss() //on ferme le dialogue après sauvegarde
                } else {
                    nameField?.error = "Le nom ne peut pas être vide"
                }
            }
        }

        dialog.show()
    }

    /**
     * Met à jour le nom d'utilisateur dans Firebase.
     * @param newName Le nouveau nom à définir pour l'utilisateur.
     */
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

    /**
     * Affiche le dialogue de gestion des notifications.
     * Permet à l'utilisateur de configurer les préférences de notifications pour les conseils et rappels d'arrosage.
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
     * Planifie un rappel pour arroser les plantes à une heure spécifique.
     * @param hour L'heure à laquelle le rappel doit être déclenché (0-23).
     * @param minute Les minutes à laquelle le rappel doit être déclenché (0-59).
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

    /**
     * Affiche le sélecteur de thème pour l'application.
     */
    private fun showAppSettingsDialog() {
        val options = arrayOf(
            "Thème et apparence",
            "Affichage",
            "Gestion du stockage",
            "Paramètres experts",
            "Accessibilité"
        )

        AlertDialog.Builder(this)
            .setTitle("Paramètres de l'application")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showThemeSelector()
                    1 -> setupDisplaySettings()
                    2 -> showStorageManagement()
                    3 -> showAdvancedSettings()
                    4 -> showAccessibilityDialog()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Affiche un sélecteur de thème pour l'application.
     * Permet à l'utilisateur de choisir parmi plusieurs couleurs prédéfinies.
     */
    private fun showThemeSelector() {
        val colors = intArrayOf(
            Color.parseColor("#4CAF50"),  // Vert nature
            Color.parseColor("#2196F3"),  // Bleu océan
            Color.parseColor("#FF9800"),  // Orange soleil
            Color.parseColor("#9C27B0")   // Violet floral
        )

        AlertDialog.Builder(this)
            .setTitle("Choisissez votre thème")
            .setItems(arrayOf("Vert", "Bleu", "Orange", "Violet")) { _, which ->
                applyTheme(colors[which])
            }
            .show()
    }

    /**
     * Applique le thème sélectionné en changeant la couleur de la barre d'état et de l'en-tête.
     * Enregistre la préférence dans les SharedPreferences.
     * @param color La couleur à appliquer comme thème.
     */
    private fun applyTheme(color: Int) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putInt("theme_color", color).apply()

        //met à jour l'UI dynamiquement
        window.statusBarColor = darkenColor(color)
        findViewById<View>(R.id.header_rectangle).setBackgroundColor(color)

        Toast.makeText(this, "Thème appliqué!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Assombrit une couleur en réduisant sa valeur de luminosité.
     * @param color La couleur à assombrir.
     * @return La couleur assombrie.
     */
    private fun darkenColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.8f
        return Color.HSVToColor(hsv)
    }

    /**
     * Configure les paramètres d'affichage de l'application.
     * Permet à l'utilisateur de choisir le mode sombre, la vue compacte et la taille du texte.
     */
    private fun setupDisplaySettings() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_display_settings, null)
        val switchDarkMode = dialogView.findViewById<SwitchCompat>(R.id.switch_dark_mode)
        val switchCompactView = dialogView.findViewById<SwitchCompat>(R.id.switch_compact_view)
        val seekBarTextSize = dialogView.findViewById<SeekBar>(R.id.seekbar_text_size)

        //charger les préférences
        val prefs = getSharedPreferences("display_prefs", MODE_PRIVATE)
        switchDarkMode.isChecked = prefs.getBoolean("dark_mode", false)
        switchCompactView.isChecked = prefs.getBoolean("compact_view", false)
        seekBarTextSize.progress = prefs.getInt("text_size", 2)

        AlertDialog.Builder(this)
            .setTitle("Affichage")
            .setView(dialogView)
            .setPositiveButton("Appliquer") { _, _ ->
                with(prefs.edit()) {
                    putBoolean("dark_mode", switchDarkMode.isChecked)
                    putBoolean("compact_view", switchCompactView.isChecked)
                    putInt("text_size", seekBarTextSize.progress)
                    apply()
                }
                recreate() //on redémarre l'activité pour appliquer les changements
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Affiche un dialogue de gestion du stockage de l'application.
     * Permet à l'utilisateur de voir la taille du cache, de la base de données et des images,
     * ainsi que d'optimiser la base de données ou de vider le cache.
     */
    private fun showStorageManagement() {
        val cacheSize = formatFileSize(this, calculateCacheSize())
        val dbSize = formatFileSize(this, File(getDatabasePath("ecoplant_database").path).length())
        AlertDialog.Builder(this)
            .setTitle("Gestion du stockage")
            .setMessage("""
            Cache: $cacheSize
            Base de données: $dbSize
            Images: ${formatFileSize(this, calculateImagesSize())}
            
            Total: ${formatFileSize(this, calculateTotalStorage())}
        """.trimIndent())
            .setPositiveButton("Vider le cache") { _, _ ->
                clearApplicationCache()
                Toast.makeText(this, "Cache vidé", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Optimiser DB") { _, _ ->
                optimizeDatabase()
                Toast.makeText(this, "Base optimisée", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Calcule la taille du cache de l'application.
     * @return La taille totale du cache en octets.
     */
    private fun calculateCacheSize(): Long {
        return cacheDir.walk().filter { it.isFile }.map { it.length() }.sum()
    }

    /**
     * Vide le cache de l'application en supprimant tous les fichiers dans le répertoire cache.
     */
    private fun clearApplicationCache() {
        cacheDir.deleteRecursively()
    }

    /**
     * Calcule la taille des images stockées dans le répertoire "images".
     * @return La taille totale des images en octets.
     */
    private fun calculateImagesSize(): Long {
        val imagesDir = File(filesDir, "images")
        return if (imagesDir.exists()) {
            imagesDir.walk().filter { it.isFile }.map { it.length() }.sum()
        } else {
            0L
        }
    }

    /**
     * Calcule la taille totale du stockage utilisé par l'application.
     * Inclut la taille du cache, de la base de données et des images.
     * @return La taille totale en octets.
     */
    private fun calculateTotalStorage(): Long {
        return calculateCacheSize() + File(getDatabasePath("ecoplant_database").path).length() + calculateImagesSize()
    }

    /**
     * Optimise la base de données en exécutant une commande PRAGMA.
     * Affiche un Toast pour informer l'utilisateur que l'optimisation est terminée.
     */
    private fun optimizeDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = database.openHelper.writableDatabase
            db.execSQL("PRAGMA optimize")
            withContext(Dispatchers.Main) {
                Toast.makeText(this@Profil, "Base de données optimisée", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Affiche un dialogue de paramètres avancés.
     * Permet à l'utilisateur de configurer des options comme les noms scientifiques, la précision des données et la source des données.
     */
    private fun showAdvancedSettings() {
        val prefs = getSharedPreferences("advanced_prefs", MODE_PRIVATE)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_advanced_settings, null).apply {
            findViewById<SwitchCompat>(R.id.switch_scientific_names).isChecked =
                prefs.getBoolean("scientific_names", false)

            findViewById<SwitchCompat>(R.id.switch_high_accuracy).isChecked =
                prefs.getBoolean("high_accuracy", true)

            findViewById<Spinner>(R.id.spinner_data_source).setSelection(
                prefs.getInt("data_source", 0)
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Paramètres experts")
            .setView(dialogView)
            .setPositiveButton("Enregistrer") { _, _ ->
                with(prefs.edit()) {
                    putBoolean("scientific_names",
                        dialogView.findViewById<SwitchCompat>(R.id.switch_scientific_names).isChecked)
                    putBoolean("high_accuracy",
                        dialogView.findViewById<SwitchCompat>(R.id.switch_high_accuracy).isChecked)
                    putInt("data_source",
                        dialogView.findViewById<Spinner>(R.id.spinner_data_source).selectedItemPosition)
                    apply()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Affiche un dialogue de paramètres d'accessibilité.
     * Permet à l'utilisateur de configurer des options comme l'assistant vocal, le contraste élevé, la vitesse des animations et la taille du toucher.
     */
    private fun showAccessibilityDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_accessibility, null)
        val prefs = getSharedPreferences("accessibility_prefs", MODE_PRIVATE)

        // Initialisation des vues
        with(dialogView) {
            findViewById<SwitchCompat>(R.id.switch_voice_assistant).isChecked =
                prefs.getBoolean("voice_assistant", false)

            findViewById<SwitchCompat>(R.id.switch_high_contrast).isChecked =
                prefs.getBoolean("high_contrast", false)

            findViewById<SeekBar>(R.id.seekbar_animation_speed).progress =
                prefs.getInt("animation_speed", 50)

            when (prefs.getInt("touch_size", 0)) {
                1 -> findViewById<RadioButton>(R.id.radio_touch_large).isChecked = true
                else -> findViewById<RadioButton>(R.id.radio_touch_default).isChecked = true
            }
        }

        // Gestion du bouton Réinitialiser
        dialogView.findViewById<Button>(R.id.btn_reset_accessibility).setOnClickListener {
            with(dialogView) {
                findViewById<SwitchCompat>(R.id.switch_voice_assistant).isChecked = false
                findViewById<SwitchCompat>(R.id.switch_high_contrast).isChecked = false
                findViewById<SeekBar>(R.id.seekbar_animation_speed).progress = 50
                findViewById<RadioButton>(R.id.radio_touch_default).isChecked = true
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Paramètres d'accessibilité")
            .setView(dialogView)
            .setPositiveButton("Appliquer") { _, _ ->
                with(prefs.edit()) {
                    putBoolean("voice_assistant",
                        dialogView.findViewById<SwitchCompat>(R.id.switch_voice_assistant).isChecked)
                    putBoolean("high_contrast",
                        dialogView.findViewById<SwitchCompat>(R.id.switch_high_contrast).isChecked)
                    putInt("animation_speed",
                        dialogView.findViewById<SeekBar>(R.id.seekbar_animation_speed).progress)
                    putInt("touch_size",
                        if (dialogView.findViewById<RadioButton>(R.id.radio_touch_large).isChecked) 1 else 0)
                    apply()
                }
                applyAccessibilitySettings()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * Applique les paramètres d'accessibilité en fonction des préférences de l'utilisateur.
     * Désactive les animations, applique le contraste élevé, etc.
     */
    private fun applyAccessibilitySettings() {
        val prefs = getSharedPreferences("accessibility_prefs", MODE_PRIVATE)

        //désactiver les animations si nécessaire
        if (prefs.getInt("animation_speed", 50) < 30) {
            window.setWindowAnimations(0)
        }

        //appliquer le contraste élevé
        if (prefs.getBoolean("high_contrast", false)) {
            findViewById<TextView>(R.id.Ecoplant_title).setTextColor(Color.BLACK)
            findViewById<View>(R.id.header_rectangle).setBackgroundColor(Color.WHITE)
        }
    }
}