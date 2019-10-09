package com.example.footprint

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.realm.RealmResults

class MyRecyclerViewAdapter(val results: RealmResults<PhotoInfoModel>)
    : RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder>() {

    var mListener: OnMyRecyclerViewClickListener?=null

    inner class ViewHolder(val v:View): RecyclerView.ViewHolder(v){
        val imageSelectedLocationPhoto: ImageView

        init {
            imageSelectedLocationPhoto=v.findViewById(R.id.imageSelectedLocationPhoto)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=LayoutInflater.from(parent.context).inflate(R.layout.gallery_photo,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return results.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selectedPhotoUri=results[position]?.stringContentUrl

        Glide.with(MyApplication.mContext).load(selectedPhotoUri).into(holder.imageSelectedLocationPhoto)

        val selectedDB=results[position]
        holder.v.setOnClickListener {
            mListener?.onSelectedPhotoClicked(selectedDB!!)
        }
    }

    interface OnMyRecyclerViewClickListener{
        fun onSelectedPhotoClicked(selectedDB: PhotoInfoModel)
    }

}