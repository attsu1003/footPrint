package com.example.footprint

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.davemorrissey.labs.subscaleview.ImageSource
import com.google.android.gms.location.LocationServices
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.content_edit.*
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class EditActivity : AppCompatActivity() {

    lateinit var mode: ModeInEdit

    val PERMISSION= arrayOf(android.Manifest.permission.CAMERA,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)

    var isCameraEnabled: Boolean=false
    var isWriteStorageEnabled: Boolean=false
    var isLocationAccessEnabled: Boolean=false

    var contentUri :Uri?=null

    var selectedPhotoInfo=PhotoInfoModel()

    var isGetLocation=false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        setSupportActionBar(toolbar)

        toolbar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
            setNavigationOnClickListener {
                finish()
            }
        }

        mode=intent.extras?.getSerializable(IntentKey.EDIT_MODE.name) as ModeInEdit

        if(mode==ModeInEdit.SHOOT){
            if(Build.VERSION.SDK_INT>=23) permissionCheck() else launchCamera()
        }else{

        }

        btnGoMap.setOnClickListener {
            if(mode==ModeInEdit.SHOOT && !isGetLocation){
                Toast.makeText(this@EditActivity,getString(R.string.location_not_set),Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            displayMap(selectedPhotoInfo.latitude,selectedPhotoInfo.longitude)
        }

        btnDone.setOnClickListener {
            writePhotoInfoToRealm()
        }
    }

    private fun writePhotoInfoToRealm() {
        val realm=Realm.getDefaultInstance()
        realm.beginTransaction()
        var photoInfoRecord=PhotoInfoModel()

        when(mode){
            ModeInEdit.SHOOT->{
                photoInfoRecord=realm.createObject(PhotoInfoModel::class.java)
            }

            ModeInEdit.EDIT->{
                photoInfoRecord = realm.where(PhotoInfoModel::class.java)
                    .equalTo(PhotoInfoModel::stringContentUrl.name,
                        selectedPhotoInfo.stringContentUrl)
                    .findFirst()!!
            }
        }
        photoInfoRecord.apply {
            stringContentUrl=selectedPhotoInfo.stringContentUrl
            dateTime=selectedPhotoInfo.dateTime
            latitude=selectedPhotoInfo.latitude
            longitude=selectedPhotoInfo.longitude
            location=latitude.toString() + longitude.toString()
            comment=inputComment.text.toString()
        }
        realm.commitTransaction()
        inputComment.setText("")
        Toast.makeText(this@EditActivity,getString(R.string.photo_info_written),Toast.LENGTH_SHORT).show()
        finish()

    }

    private fun displayPhotoInfo() {
        selectedPhotoInfo.apply {
            stringContentUrl=(intent.extras?.getString(PhotoInfoModel::stringContentUrl.name))!!
            dateTime=(intent.extras?.getString(PhotoInfoModel::dateTime.name))!!
            latitude=(intent.extras?.getDouble(PhotoInfoModel::latitude.name))!!
            longitude=(intent.extras?.getDouble(PhotoInfoModel::longitude.name))!!
            location=(intent.extras?.getString(PhotoInfoModel::location.name))!!
            comment=(intent.extras?.getString(PhotoInfoModel::comment.name))!!
        }
        imageView.setImage(ImageSource.uri(Uri.parse(selectedPhotoInfo.stringContentUrl)))
        inputComment.setText(selectedPhotoInfo.comment)
    }

    private fun displayMap(latitude: Double, longitude: Double) {
        val geoString="geo:" + latitude + "," + longitude + "?z=" + ZOOM_LEVEL_DETAIL
        val gmmIntentUri=Uri.parse(geoString)
        val mapIntent=Intent(Intent.ACTION_VIEW,gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
     }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(IntentKey.CONTENT_URI.name, contentUri)
    }

    private fun launchCamera() {
        val contentFileName=SimpleDateFormat("yyyyMMdd_HHmmss_z").format(Date())
        contentUri=generateContentUriFromFileName(contentFileName)

        val intent=Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT,contentUri)
        }


        //APIレベル２１未満の場合に必要な処理
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
            val context=applicationContext
            val resolveIntentActivities=context.packageManager
                .queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY)
            for(resolvedIntentInfo in resolveIntentActivities){
                val packageName=resolvedIntentInfo.activityInfo.packageName
                context.grantUriPermission(packageName,contentUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
        startActivityForResult(intent, REQ_CODE_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode!= Activity.RESULT_OK){
            Toast.makeText(this@EditActivity,getString(R.string.shoot_failed),Toast.LENGTH_SHORT).show()
            return
        }
        if(requestCode!= REQ_CODE_CAMERA){
            Toast.makeText(this@EditActivity,getString(R.string.shoot_failed),Toast.LENGTH_SHORT).show()
            return
        }

        if(contentUri==null){
            Toast.makeText(this@EditActivity,getString(R.string.shoot_failed),Toast.LENGTH_SHORT).show()
            return
        }

        imageView.setImage(ImageSource.uri(contentUri!!))
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
            applicationContext.revokeUriPermission(contentUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        selectedPhotoInfo.stringContentUrl=contentUri.toString()
        selectedPhotoInfo.dateTime=SimpleDateFormat("yyyyMMdd_HHmmss_z").format(Date())

        getLocation()
    }

    private fun getLocation() {
        val client=LocationServices.getFusedLocationProviderClient(this)

        try {
            client.lastLocation.addOnSuccessListener {
//                selectedPhotoInfo.latitude=it.latitude
//                selectedPhotoInfo.longitude=it.longitude
                selectedPhotoInfo.latitude=BigDecimal(it.latitude.toString()).setScale(3,BigDecimal.ROUND_DOWN).toDouble()
                selectedPhotoInfo.longitude=BigDecimal(it.longitude.toString()).setScale(3,BigDecimal.ROUND_DOWN).toDouble()


                Toast.makeText(this@EditActivity,
                    getString(R.string.location_get) + selectedPhotoInfo.latitude.toString() + " : "
                            + selectedPhotoInfo.longitude.toString(), Toast.LENGTH_SHORT).show()

                isGetLocation=true

            }
        }catch (e :SecurityException){

        }

    }

    private fun generateContentUriFromFileName(contentFileName: String): Uri? {
        val contentFolder=File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES), PHOTO_FOLDER_NAME
        )
        contentFolder.mkdirs()
        val contentFilePath=contentFolder.path + "/" + contentFileName + ".jpg"

        val contentFile=File(contentFilePath)
        return FileProvider.getUriForFile(
            this@EditActivity,
            applicationContext.packageName + ".fileprovider",
            contentFile
        )
    }


    private fun permissionCheck() {
        val permissionCheckCamera: Int=ContextCompat.checkSelfPermission(this@EditActivity, PERMISSION[0])
        val permissionCheckWriteStorage: Int=ContextCompat.checkSelfPermission(this@EditActivity, PERMISSION[1])
        val permissionCheckLocationAccess: Int=ContextCompat.checkSelfPermission(this@EditActivity, PERMISSION[2])

        if(permissionCheckCamera==PackageManager.PERMISSION_GRANTED) isCameraEnabled=true
        if(permissionCheckWriteStorage==PackageManager.PERMISSION_GRANTED) isWriteStorageEnabled=true
        if(permissionCheckLocationAccess==PackageManager.PERMISSION_GRANTED) isLocationAccessEnabled=true

        if(isCameraEnabled && isWriteStorageEnabled && isLocationAccessEnabled) launchCamera() else permissionRequest()
    }

    private fun permissionRequest() {
        val isNeedExplainForCameraPermission=
            ActivityCompat.shouldShowRequestPermissionRationale(this@EditActivity,PERMISSION[0])
        val isNeedExplainForWriteStoragePermission=
            ActivityCompat.shouldShowRequestPermissionRationale(this@EditActivity,PERMISSION[1])
        val isNeedExplainForLocationAccess=
            ActivityCompat.shouldShowRequestPermissionRationale(this@EditActivity,PERMISSION[2])
        val isNeedExplainPermission
            =if(isNeedExplainForCameraPermission || isNeedExplainForWriteStoragePermission || isNeedExplainForLocationAccess){
            true
        }else false

        val requestPermissionList=ArrayList<String>()

        if(!isCameraEnabled) requestPermissionList.add(PERMISSION[0])
        if(!isWriteStorageEnabled) requestPermissionList.add(PERMISSION[1])
        if(!isLocationAccessEnabled) requestPermissionList.add(PERMISSION[2])

        if(!isNeedExplainPermission){
            ActivityCompat.requestPermissions(
                this@EditActivity,
                requestPermissionList.toArray(arrayOfNulls(requestPermissionList.size)),
                REQ_CODE_PERMISSION
            )
            return
        }
        val dialog=AlertDialog.Builder(this@EditActivity).apply {
            setTitle(getString(R.string.permission_request_title))
            setMessage(getString(R.string.permission_request_title))
            setPositiveButton(getString(R.string.admit)){dialogInterface, i ->
                ActivityCompat.requestPermissions(
                this@EditActivity,
                requestPermissionList.toArray(arrayOfNulls(requestPermissionList.size)),
                REQ_CODE_PERMISSION
                )
            }
            setNegativeButton(getString(R.string.reject)){ dialogInterface, i ->
                Toast.makeText(this@EditActivity, getString(R.string.cannot_go_any_further),Toast.LENGTH_SHORT).show()
                finish()
            }
            show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != REQ_CODE_PERMISSION)
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        return
        if (grantResults.size <= 0) return

        for (i in 0.. permissions.size - 1) {
            when (permissions[i]) {
                PERMISSION[0] -> {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(
                            this@EditActivity, getString(R.string.cannot_go_any_further),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return
                    }
                    isCameraEnabled=true
                }
                PERMISSION[1] -> {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(
                            this@EditActivity, getString(R.string.cannot_go_any_further),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return
                    }
                    isWriteStorageEnabled=true
                }
                PERMISSION[2] -> {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(
                            this@EditActivity, getString(R.string.cannot_go_any_further),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                        return
                    }
                    isLocationAccessEnabled=true
                }
            }
        }
        if(isCameraEnabled && isWriteStorageEnabled && isLocationAccessEnabled) launchCamera() else finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.apply {
            findItem(R.id.action_settings).isVisible=true
            findItem(R.id.action_share).isVisible=false
            findItem(R.id.action_commnet).isVisible=false
            findItem(R.id.action_delete).isVisible=true
            findItem(R.id.action_edit).isVisible=false
            findItem(R.id.action_camera).isVisible=  if (mode==ModeInEdit.SHOOT) true else false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item!!.itemId){
            R.id.action_delete->{
               when(mode){
                   ModeInEdit.SHOOT->{
                       contentResolver.delete(Uri.parse(selectedPhotoInfo.stringContentUrl),null,null)
                       Toast.makeText(this@EditActivity,getString(R.string.photo_info_deleted),Toast.LENGTH_SHORT).show()
                       finish()
                       return true
                   }
                   ModeInEdit.EDIT-> {
                       contentResolver.delete(Uri.parse(selectedPhotoInfo.stringContentUrl),null,null)

                       deleteSelectedPhotoFromRealm()
                       Toast.makeText(this@EditActivity,getString(R.string.photo_info_deleted),Toast.LENGTH_SHORT).show()
                       finish()
                   }
               }
            }
            R.id.action_camera->{
                inputComment.setText("")
                if(Build.VERSION.SDK_INT>=23) permissionCheck() else launchCamera()
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true

    }

    private fun deleteSelectedPhotoFromRealm() {
        val realm=Realm.getDefaultInstance()
        val selectedData=realm.where(PhotoInfoModel::class.java)
            .equalTo(PhotoInfoModel::stringContentUrl.name,selectedPhotoInfo.stringContentUrl)
            .findFirst()
        realm.beginTransaction()
        selectedData?.deleteFromRealm()
        realm.commitTransaction()

    }

}
