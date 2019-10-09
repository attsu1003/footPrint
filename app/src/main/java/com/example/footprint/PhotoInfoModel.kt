package com.example.footprint

import io.realm.RealmObject

open class PhotoInfoModel: RealmObject() {

    var stringContentUrl: String=""

    var dateTime: String=""

    var latitude: Double=0.0

    var longitude: Double=0.0

    var location: String=""

    var comment: String=""

}