package com.example.ecoplant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider


class SeConnecter : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var togglePassword: ImageView
    private lateinit var rememberCheckbox: ImageView
    private lateinit var loginButton: LinearLayout
    private lateinit var btnRetour: ImageView
    private lateinit var googleBtn: LinearLayout
    private lateinit var creezUnCompteBtn: TextView

    //clés SharedPreferences
    private val PREFS_NAME = "login_prefs"
    private val KEY_REMEMBER = "remember_me"
    private val KEY_EMAIL = "saved_email"
    private val KEY_PASSWORD = "saved_password"

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_se_connecter)

        //initialisation des vues
        emailEditText = findViewById(R.id.entrez_votre_mail_btn)
        passwordEditText = findViewById(R.id.entrez_votre_mot_de_passe_btn)
        togglePassword = findViewById(R.id.eyeoff_seconnecter_btn)
        rememberCheckbox = findViewById(R.id.checkbox)
        loginButton = findViewById(R.id.se_connecter_btn2)
        btnRetour = findViewById<ImageView>(R.id.BtnRetour)
        googleBtn = findViewById(R.id.google_btn)
        creezUnCompteBtn = findViewById(R.id.creer_un_compte_btn)

        auth = FirebaseAuth.getInstance()
        FirebaseAuth.getInstance().setLanguageCode("fr")
        //configuration de la connexion Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //bouton retour
        btnRetour.setOnClickListener {startActivity(Intent(this, MainActivity::class.java))}

        //mot de passe visible ou pas
        var isPasswordVisible = false
        togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePassword.setImageResource(R.drawable.eye)
            }
            else {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePassword.setImageResource(R.drawable.eye_off)
            }
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        //SharedPreferences
        val sp = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var isRemembered = sp.getBoolean(KEY_REMEMBER, false)

        //si on s'était déjà souvenu : restaurer email + mot de passe + icône
        if (isRemembered) {
            emailEditText.setText(sp.getString(KEY_EMAIL, ""))
            passwordEditText.setText(sp.getString(KEY_PASSWORD, ""))
            rememberCheckbox.setImageResource(R.drawable.baseline_check_box_24)
        } else {
            rememberCheckbox.setImageResource(R.drawable.baseline_check_box_outline_blank_24)
        }

        //box "Se souvenir"
        rememberCheckbox.setOnClickListener {
            isRemembered = !isRemembered
            rememberCheckbox.setImageResource(
                if (isRemembered) R.drawable.baseline_check_box_24
                else R.drawable.baseline_check_box_outline_blank_24
            )
            with(sp.edit()) {
                putBoolean(KEY_REMEMBER, isRemembered)
                if (isRemembered) {
                    putString(KEY_EMAIL, emailEditText.text.toString())
                    putString(KEY_PASSWORD, passwordEditText.text.toString())
                }
                else {
                    remove(KEY_EMAIL)
                    remove(KEY_PASSWORD)
                }
                apply()
            }
        }

        val forgotPasswordText = findViewById<TextView>(R.id.mot_de_passe_oublie_btn)
        forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, MotDePasseOublieActivity::class.java))
        }

        //bouton "Se connecter"
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.error = "Entrez un email valide"
                emailEditText.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordEditText.error = "Entrez votre mot de passe"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        //sauvegarder si "Se souvenir" est coché
                        with(sp.edit()) {
                            if (isRemembered) {
                                putString(KEY_EMAIL, email)
                                putString(KEY_PASSWORD, password)
                            }
                            else {
                                remove(KEY_EMAIL)
                                remove(KEY_PASSWORD)
                            }
                            apply()
                        }

                        Toast.makeText(this, "Bienvenue sur EcoPlant 🌱", Toast.LENGTH_LONG).show()
                        //on va vers l'activité principale (scan)
                        startActivity(Intent(this, Scan::class.java))
                        finish()
                    } else {
                        val errorCode = (task.exception as? FirebaseAuthException)?.errorCode
                        val errorMessage = when (errorCode) {
                            "ERROR_INVALID_EMAIL" ->
                                "L'adresse e-mail est invalide."
                            "ERROR_USER_NOT_FOUND" ->
                                "Aucun compte n'est associé à cette adresse e-mail."
                            "ERROR_WRONG_PASSWORD" ->
                                "Mot de passe incorrect."
                            "ERROR_USER_DISABLED" ->
                                "Ce compte a été désactivé."
                            "ERROR_TOO_MANY_REQUESTS" ->
                                "Trop de tentatives. Réessayez plus tard."
                            "ERROR_NETWORK_REQUEST_FAILED" ->
                                "Problème de connexion. Vérifiez votre réseau."
                            else ->
                                "Erreur de connexion. Veuillez réessayer."
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }

                }
        }


        googleBtn.setOnClickListener {
            signInWithGoogle()
        }

        //vers l'écran de création de compte
        creezUnCompteBtn.setOnClickListener {
            startActivity(Intent(this, CreerUnCompte::class.java))
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                //connexion Google réussie, authentifier avec Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Erreur Google Sign-In: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    //connexion réussie
                    Toast.makeText(this, "Connexion Google réussie!", Toast.LENGTH_SHORT).show()

                    //on sauvegarde l'état "Se souvenir" si nécessaire
                    val sp = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    with(sp.edit()) {
                        putBoolean(KEY_REMEMBER, true)
                        //pour Google, on ne sauvegarde pas d'email/mot de passe
                        apply()
                    }

                    //redirection vers l'activité principale
                    startActivity(Intent(this, Scan::class.java))
                    finish()
                } else {
                    //échec de la connexion
                    Toast.makeText(this, "Échec de l'authentification: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

}
