package com.example.footprint

enum class ModeInEdit{
    SHOOT,EDIT
}

enum class IntentKey{
    EDIT_MODE,
    CONTENT_URI,
    LATITUDE,
    LONGITUDE
}

val REQ_CODE_PERMISSION=200

val REQ_CODE_CAMERA=100

val PHOTO_FOLDER_NAME="FOOTPRINT"

val ZOOM_LEVEL_DETAIL=15

val ZOOM_LEVEL_MASTER=5