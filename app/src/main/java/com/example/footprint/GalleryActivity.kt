package com.example.footprint

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.content_gallery.*

class GalleryActivity : AppCompatActivity(), MyRecyclerViewAdapter.OnMyRecyclerViewClickListener {


    lateinit var realm: Realm
    lateinit var results: RealmResults<PhotoInfoModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        setSupportActionBar(toolbar)

        toolbar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
            setNavigationOnClickListener {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val selectedLocation: String=getSelectedLocation()

        realm= Realm.getDefaultInstance()
        results=realm.where(PhotoInfoModel::class.java)
            .equalTo(PhotoInfoModel::location.name,selectedLocation)
            .findAll()

        setGallery(results)
    }

    private fun setGallery(results: RealmResults<PhotoInfoModel>?) {
        val screenOrientation=resources.configuration.orientation
        myRecyclerView.layoutManager=if(screenOrientation==Configuration.ORIENTATION_PORTRAIT){
            GridLayoutManager(this,2)
        }else{
            GridLayoutManager(this,4)
        }

        val adapter=MyRecyclerViewAdapter(results!!)
        myRecyclerView.adapter=adapter
        adapter.mListener=this
    }

    //MyRecyclerViewAdapter.OnMyRecyclerViewClickListener
    override fun onSelectedPhotoClicked(selectedDB: PhotoInfoModel) {
        val intent=Intent(this@GalleryActivity,DetailActivity::class.java).apply {
            putExtra(PhotoInfoModel::stringContentUrl.name,selectedDB.stringContentUrl)
            putExtra(PhotoInfoModel::dateTime.name,selectedDB.dateTime)
            putExtra(PhotoInfoModel::latitude.name,selectedDB.latitude)
            putExtra(PhotoInfoModel::longitude.name,selectedDB.longitude)
            putExtra(PhotoInfoModel::location.name,selectedDB.location)
            putExtra(PhotoInfoModel::comment.name,selectedDB.comment)

        }
        startActivity(intent)
    }

    private fun getSelectedLocation(): String {
        val selectedLatitude=intent.extras?.getDouble(IntentKey.LATITUDE.name)
        val selectedLongitude=intent.extras?.getDouble(IntentKey.LONGITUDE.name)
        return selectedLatitude.toString() + selectedLongitude.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
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
        return super.onOptionsItemSelected(item)
        val id=item.itemId

        when(id){
            R.id.action_camera->{
                val intent= Intent(this@GalleryActivity,EditActivity::class.java).apply {
                    putExtra(IntentKey.EDIT_MODE.name,ModeInEdit.SHOOT)
                }
                startActivity(intent)
                finish()
            }
            else->super.onOptionsItemSelected(item)
        }
        return true
    }

}
