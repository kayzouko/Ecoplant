package com.example.ecoplant

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class MainActivity : AppCompatActivity() {

    private lateinit var seConnecterBtn: LinearLayout
    private lateinit var creerUnCompteBtn: LinearLayout
    private lateinit var googleBtn: LinearLayout
    private val PREFS_NAME = "login_prefs"
    private val KEY_REMEMBER = "remember_me"
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private companion object {
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accueil)

        seConnecterBtn = findViewById(R.id.se_connecter_btn)
        creerUnCompteBtn = findViewById(R.id.creer_un_compte_btn)
        googleBtn = findViewById(R.id.google_btn)
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Même ID que dans CreerUnCompte
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        seConnecterBtn.setOnClickListener {
            startActivity(Intent(this, SeConnecter::class.java))
        }

        creerUnCompteBtn.setOnClickListener {
            startActivity(Intent(this, CreerUnCompte::class.java))
        }

        googleBtn.setOnClickListener {
            signInWithGoogle()
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
                //connexion Google réussie, on authentifie avec Firebase
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
                    // Connexion réussie
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
