package com.example.footprint

import android.app.Application
import android.content.Context
import io.realm.Realm

class MyApplication: Application(){

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)

        mContext=this
    }

    companion object{
        lateinit var mContext: Context
    }
}