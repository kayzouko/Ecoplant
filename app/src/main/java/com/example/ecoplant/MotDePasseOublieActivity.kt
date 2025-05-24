package com.example.ecoplant

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MotDePasseOublieActivity : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var emailInput: EditText
    private lateinit var errorText: TextView
    private lateinit var confirmBtn: LinearLayout
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mot_de_passe_oublie)

        //initialisation des vues
        backBtn = findViewById(R.id.back_btn)
        emailInput = findViewById(R.id.reset_email_btn)
        errorText = findViewById(R.id.reset_error)
        confirmBtn = findViewById(R.id.reset_confirm_btn)
        auth = FirebaseAuth.getInstance()

        //bouton retour
        backBtn.setOnClickListener {finish()}

        //bouton confirmer
        confirmBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()

            //validation simple
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            } else {
                errorText.visibility = View.GONE
            }

            //envoi mail reset via Firebase Authentication
            auth.sendPasswordResetEmail(email).addOnCompleteListener {task -> if (task.isSuccessful) {
                        Toast.makeText(this,"Lien de récupération envoyé sur $email", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Erreur : impossible d’envoyer le mail.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
