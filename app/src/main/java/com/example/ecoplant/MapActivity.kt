package com.example.ecoplant

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST = 1001
    private val database by lazy { (application as EcoPlantApplication).database }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        //initialisation de la carte
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //navigation footer
        setupFooterNavigation()
    }

    private fun setupFooterNavigation() {
        val scanBtn = findViewById<LinearLayout>(R.id.scan_btn)
        val historiqueBtn = findViewById<LinearLayout>(R.id.historique_btn)
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
            startActivity(Intent(this, Profil::class.java))
            finish()
        }
    }

    /** * appelée lorsque la carte est prête à être utilisée */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)

        //vérifie les permissions de localisation
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocationFeatures()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }

        //charger les analyses et afficher les pins
        loadPlantLocations()
    }

    /** * Active les fonctionnalités de localisation sur la carte */
    private fun enableLocationFeatures() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
        }
    }

    /** * Gère la réponse à la demande de permission de localisation
     * @param requestCode Le code de la demande de permission
     * @param permissions Les permissions demandées
     * @param grantResults Les résultats de la demande de permission
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocationFeatures()
        }
    }

    /** * Charge les emplacements des plantes à partir de la base de données et les affiche sur la carte */
    private fun loadPlantLocations() {
        GlobalScope.launch(Dispatchers.IO) {
            val analyses = database.recentAnalysisDao().getAllWithLocation()
            Log.d("MapDebug", "Analyses with location: ${analyses.size}")
            withContext(Dispatchers.Main) {
                var firstLocation: LatLng? = null

                analyses.forEach { analysis ->
                    analysis.latitude?.let { lat ->
                        analysis.longitude?.let { lng ->
                            val location = LatLng(lat, lng)
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(location)
                                    .title(analysis.commonName ?: analysis.scientificName)
                                    .snippet("Score: ${"%.2f".format(analysis.score)}")
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_GREEN
                                    ))
                            )

                            //stocke l'ID de l'analyse dans le tag du marqueur
                            marker?.tag = analysis.id

                            if (firstLocation == null) {
                                firstLocation = location
                            }
                        }
                    }
                }

                //centre la carte sur la première position
                firstLocation?.let {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 12f))
                }
            }
        }
    }

    /** * Gère le clic sur un marqueur de la carte
     * @param marker Le marqueur cliqué
     * @return true pour indiquer que l'événement a été traité
     */
    override fun onMarkerClick(marker: Marker): Boolean {
        //affiche le nom de la plante dans l'infobulle
        marker.showInfoWindow()
        return true
    }

    /** * Gère la reprise de l'activité pour recharger les emplacements des plantes */
    override fun onResume() {
        super.onResume()
        loadPlantLocations()
    }
}