package com.example.footprint

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {



    lateinit var mapFragment: MapFragment

    lateinit var realm: Realm
    lateinit var results: RealmResults<PhotoInfoModel>
    lateinit var locationList: ArrayList<PhotoInfoModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        super.onResume()

        realm= Realm.getDefaultInstance()
        results=realm.where(PhotoInfoModel::class.java).findAll()
        mapFragment=MapFragment.newInstance()
        fragmentManager.beginTransaction().add(R.id.container_map, mapFragment).commit()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        map.uiSettings.isZoomControlsEnabled=true

        if(results.size>0){
            setUpLocationMarkers(map)
        }

        map.setOnMarkerClickListener(this)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val position=marker.position
        val intent=Intent(this@MainActivity,GalleryActivity::class.java).apply {
            putExtra(IntentKey.LATITUDE.name,position.latitude)
            putExtra(IntentKey.LONGITUDE.name,position.longitude)
        }
        startActivity(intent)
        return true
    }

    private fun setUpLocationMarkers(map: GoogleMap) {
        locationList=ArrayList<PhotoInfoModel>()
        locationList.add(results[0]!!)
        for(i in 1 until results.size-1){
            if(results[i]?.location!=results[i-1]?.location){
                locationList.add(results[i]!!)
            }
        }

        val lastIndexOfLocationList=locationList.size-1
        locationList.forEach {
            map.addMarker(MarkerOptions().position(LatLng(it.latitude,it.longitude)))
        }

        val cameraPosition=CameraPosition.builder()
            .target(LatLng(locationList[lastIndexOfLocationList].latitude,
                            locationList[lastIndexOfLocationList].longitude))
            .zoom(ZOOM_LEVEL_MASTER.toFloat())
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.apply {
            findItem(R.id.action_settings).isVisible=true
            findItem(R.id.action_share).isVisible=false
            findItem(R.id.action_commnet).isVisible=false
            findItem(R.id.action_delete).isVisible=false
            findItem(R.id.action_edit).isVisible=false
            findItem(R.id.action_camera).isVisible=true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        val id=item.itemId

        when(id){
            R.id.action_camera->{
                val intent=Intent(this@MainActivity,EditActivity::class.java).apply {
                    putExtra(IntentKey.EDIT_MODE.name,ModeInEdit.SHOOT)
                }
                startActivity(intent)
            }
            else->super.onOptionsItemSelected(item)
        }
        return true
    }
}
