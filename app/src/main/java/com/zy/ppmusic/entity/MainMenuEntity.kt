package com.zy.ppmusic.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * @author ZhiTouPC
 */
class MainMenuEntity : Parcelable {
    var menuTitle: String? = null
    var menuRes: Int = 0

    constructor() {}

    constructor(menuTitle: String, menuRes: Int) {
        this.menuTitle = menuTitle
        this.menuRes = menuRes
    }

    protected constructor(`in`: Parcel) {
        menuTitle = `in`.readString()
        menuRes = `in`.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(menuTitle)
        dest.writeInt(menuRes)
    }

    companion object {

        val CREATOR: Parcelable.Creator<MainMenuEntity> = object : Parcelable.Creator<MainMenuEntity> {
            override fun createFromParcel(`in`: Parcel): MainMenuEntity {
                return MainMenuEntity(`in`)
            }

            override fun newArray(size: Int): Array<MainMenuEntity> {
                return newArray(size)
            }
        }
    }
}
