package com.example.footprint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davemorrissey.labs.subscaleview.ImageSource
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.content_detail.*
import kotlinx.android.synthetic.main.content_edit.imageView

class DetailActivity : AppCompatActivity() {

    var selectedDB=PhotoInfoModel()
    var isCommentDisplayed=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        setSupportActionBar(toolbar)

        fab.setOnClickListener {
            displayMap(selectedDB.latitude,selectedDB.longitude)
        }

        toolbar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
            setNavigationOnClickListener {
                finish()
            }
        }

        selectedDB.apply {
            stringContentUrl=(intent.extras?.getString(PhotoInfoModel::stringContentUrl.name))!!
            dateTime=(intent.extras?.getString(PhotoInfoModel::dateTime.name))!!
            latitude=(intent.extras?.getDouble(PhotoInfoModel::latitude.name))!!
            longitude=(intent.extras?.getDouble(PhotoInfoModel::longitude.name))!!
            location=(intent.extras?.getString(PhotoInfoModel::location.name))!!
            comment=(intent.extras?.getString(PhotoInfoModel::comment.name))!!
        }

        imageView.setImage(ImageSource.uri(Uri.parse(selectedDB.stringContentUrl)))
    }

    private fun displayMap(latitude: Double, longitude: Double) {
        val geoString="geo:" + latitude + "," + longitude + "?z=" + ZOOM_LEVEL_DETAIL
        val gmmIntentUri=Uri.parse(geoString)
        val mapIntent= Intent(Intent.ACTION_VIEW,gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.apply {
            findItem(R.id.action_settings).isVisible=true
            findItem(R.id.action_share).isVisible=true
            findItem(R.id.action_commnet).isVisible=true
            findItem(R.id.action_delete).isVisible=true
            findItem(R.id.action_edit).isVisible=true
            findItem(R.id.action_camera).isVisible=false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        val id=item.itemId

        when(id){
            R.id.action_share->{
                shareSelectedPhoto()
            }
            R.id.action_commnet->{
                setComment()
            }
            R.id.action_delete->{
                deleteSelectedPhoto()
            }
            R.id.action_edit->{
                editSelectedPhoto()
            }
        }
        return true
    }

    private fun editSelectedPhoto() {
        val intent=Intent(this@DetailActivity,EditActivity::class.java).apply {
            putExtra(PhotoInfoModel::stringContentUrl.name,selectedDB.stringContentUrl)
            putExtra(PhotoInfoModel::dateTime.name,selectedDB.dateTime)
            putExtra(PhotoInfoModel::latitude.name,selectedDB.latitude)
            putExtra(PhotoInfoModel::longitude.name,selectedDB.longitude)
            putExtra(PhotoInfoModel::location.name,selectedDB.location)
            putExtra(PhotoInfoModel::comment.name,selectedDB.comment)
            putExtra(IntentKey.EDIT_MODE.name,ModeInEdit.EDIT)
        }
        startActivity(intent)
        finish()
    }

    private fun shareSelectedPhoto() {
        val shareTitle=getString(R.string.share_photo)
        val intent=Intent().apply {
            action=Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM,Uri.parse(selectedDB.stringContentUrl))
            type="image/jpeg"
        }
        startActivity(Intent.createChooser(intent,shareTitle))
    }

    private fun deleteSelectedPhoto() {
        contentResolver.delete(Uri.parse(selectedDB.stringContentUrl),null,null)

        deleteSelectedPhotoFromRealm()
        Toast.makeText(this@DetailActivity,getString(R.string.photo_info_deleted),Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteSelectedPhotoFromRealm() {
        val realm=Realm.getDefaultInstance()
        val selectedData=realm.where(PhotoInfoModel::class.java)
            .equalTo(PhotoInfoModel::stringContentUrl.name,selectedDB.stringContentUrl)
            .findFirst()
        realm.beginTransaction()
        selectedData?.deleteFromRealm()
        realm.commitTransaction()

    }

    private fun setComment() {
        isCommentDisplayed=!isCommentDisplayed
        if(isCommentDisplayed){
            textShootingDate.visibility=View.VISIBLE
            textComment.visibility=View.VISIBLE
            textShootingDate.text=selectedDB.dateTime
            textComment.text=selectedDB.comment
            return
        }
        textShootingDate.visibility=View.INVISIBLE
        textComment.visibility=View.INVISIBLE
    }

}
