package com.example.ecoplant

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class CreerUnCompte : AppCompatActivity() {

    private lateinit var nomEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var motDePasseEditText: EditText
    private lateinit var confirmerMotDePasseEditText: EditText
    private lateinit var retourBtn: ImageView

    private lateinit var toggleMotDePasse: ImageView
    private lateinit var toggleConfirmer: ImageView
    private lateinit var rememberCheckbox: ImageView

    private lateinit var checkboxError: TextView
    private lateinit var creerCompteBtn: LinearLayout
    private lateinit var googleBtn: LinearLayout
    private lateinit var connectezVousBtn: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private companion object {
        private const val RC_SIGN_IN = 9001
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creer_un_compte)

        //initialisation des vues
        nomEditText = findViewById(R.id.entrez_votre_nom_complet_btn)
        emailEditText = findViewById(R.id.entrez_votre_mail_btn)
        motDePasseEditText = findViewById(R.id.creez_votre_mot_de_passe_btn)
        confirmerMotDePasseEditText = findViewById(R.id.confirmer_votre_mot_de_passe_btn)

        toggleMotDePasse = findViewById(R.id.eye_mdp_icone)
        toggleConfirmer = findViewById(R.id.eye_confirmer_mdp_icone)
        rememberCheckbox = findViewById(R.id.checkboxIcon)

        checkboxError = findViewById(R.id.checkboxError)
        creerCompteBtn = findViewById(R.id.creez_le_compte_btn)
        googleBtn = findViewById(R.id.google_btn)
        connectezVousBtn = findViewById(R.id.connectez_vous_btn)
        retourBtn = findViewById(R.id.retourBtn)

        //Firebase Authentication
        auth = FirebaseAuth.getInstance()
        FirebaseAuth.getInstance().setLanguageCode("fr")
        //configuration de la connexion Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        //bouton retour
        retourBtn.setOnClickListener {startActivity(Intent(this, MainActivity::class.java))}

        //mot de passe visible ou pas
        var isMotDePasseVisible = false
        toggleMotDePasse.setOnClickListener {
            isMotDePasseVisible = !isMotDePasseVisible
            if (isMotDePasseVisible) {
                motDePasseEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleMotDePasse.setImageResource(R.drawable.eye)
            } else {
                motDePasseEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleMotDePasse.setImageResource(R.drawable.eye_off)
            }
            motDePasseEditText.setSelection(motDePasseEditText.text.length)
        }

        var isConfirmerVisible = false
        toggleConfirmer.setOnClickListener {
            isConfirmerVisible = !isConfirmerVisible
            if (isConfirmerVisible) {
                confirmerMotDePasseEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleConfirmer.setImageResource(R.drawable.eye)
            } else {
                confirmerMotDePasseEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleConfirmer.setImageResource(R.drawable.eye_off)
            }
            confirmerMotDePasseEditText.setSelection(confirmerMotDePasseEditText.text.length)
        }

        //checkbox conditions
        var isChecked = false
        rememberCheckbox.setOnClickListener {
            isChecked = !isChecked
            rememberCheckbox.setImageResource(
                if (isChecked) R.drawable.baseline_check_box_24
                else R.drawable.baseline_check_box_outline_blank_24
            )
            if (isChecked){
                checkboxError.visibility = View.GONE
            }
        }

        fun handleClickIfChecked(action: () -> Unit) {
            if (!isChecked) {
                checkboxError.visibility = View.VISIBLE
            } else {
                checkboxError.visibility = View.GONE
                action()
            }
        }

        //création du compte Firebase
        creerCompteBtn.setOnClickListener { handleClickIfChecked {
                val nom = nomEditText.text.toString().trim()
                val email = emailEditText.text.toString().trim()
                val mdp = motDePasseEditText.text.toString()
                val mdp2 = confirmerMotDePasseEditText.text.toString()

                when {
                    nom.isEmpty() -> {
                        nomEditText.error = "Entrez votre nom complet"
                        nomEditText.requestFocus()
                    }
                    email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                        emailEditText.error = "Entrez un email valide"
                        emailEditText.requestFocus()
                    }
                    mdp.length < 6 -> {
                        motDePasseEditText.error = "Au moins 6 caractères"
                        motDePasseEditText.requestFocus()
                    }
                    mdp != mdp2 -> {
                        confirmerMotDePasseEditText.error = "Les mots de passe ne correspondent pas"
                        confirmerMotDePasseEditText.requestFocus()
                    }
                    else -> {
                        //appel Firebase
                        auth.createUserWithEmailAndPassword(email, mdp)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this,
                                        "Compte créé avec succès !", Toast.LENGTH_LONG).show()
                                    //passe à l'activité d'accueil (scan)
                                    startActivity(Intent(this, Scan::class.java))
                                    finish()
                                }
                                else {
                                    Toast.makeText(this, "Erreur : ${task.exception?.message}",
                                        Toast.LENGTH_LONG).show()
                                }
                            }
                    }
                }
            }
        }

        googleBtn.setOnClickListener {
            handleClickIfChecked {
                signInWithGoogle()
            }
        }

        //vers l'écran de connexion
        connectezVousBtn.setOnClickListener {
            startActivity(Intent(this, SeConnecter::class.java))
        }
    }

    //connexion avec Google
    fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    //traitement du résultat de la connexion Google
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Échec de la connexion Google : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    //authentification Firebase avec le compte Google
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Connexion réussie avec Google", Toast.LENGTH_LONG).show()
                    //passe à l'activité d'accueil (scan)
                    startActivity(Intent(this, Scan::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Échec de la connexion : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}
