package com.example.ecoplant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var seConnecterBtn: LinearLayout
    private lateinit var creerUnCompteBtn: LinearLayout
    private lateinit var googleBtn: LinearLayout
    private lateinit var appleBtn: LinearLayout
    private lateinit var facebookBtn: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accueil)

        seConnecterBtn = findViewById(R.id.se_connecter_btn)
        creerUnCompteBtn = findViewById(R.id.creer_un_compte_btn)
        googleBtn = findViewById(R.id.google_btn)
        appleBtn = findViewById(R.id.apple_btn)
        facebookBtn = findViewById(R.id.facebook_btn)

        seConnecterBtn.setOnClickListener {
            startActivity(Intent(this, SeConnecter::class.java))
        }

        creerUnCompteBtn.setOnClickListener {
            startActivity(Intent(this, CreerUnCompte::class.java))
        }

        googleBtn.setOnClickListener {
            Toast.makeText(this, "Connexion Google non implémentée", Toast.LENGTH_SHORT).show()
        }
        appleBtn.setOnClickListener {
            Toast.makeText(this, "Connexion Apple non implémentée", Toast.LENGTH_SHORT).show()
        }
        facebookBtn.setOnClickListener {
            Toast.makeText(this, "Connexion Facebook non implémentée", Toast.LENGTH_SHORT).show()
        }
    }
}
