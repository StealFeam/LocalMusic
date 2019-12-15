package com.zy.ppmusic.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * @author stealfeam
 */
class MainMenuEntity() : Parcelable {
    var menuTitle: String? = null
    var menuRes: Int = 0

    constructor(parcel: Parcel) : this() {
        menuTitle = parcel.readString()
        menuRes = parcel.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(menuTitle)
        dest.writeInt(menuRes)
    }

    companion object CREATOR : Parcelable.Creator<MainMenuEntity> {
        override fun createFromParcel(parcel: Parcel): MainMenuEntity {
            return MainMenuEntity(parcel)
        }

        override fun newArray(size: Int): Array<MainMenuEntity?> {
            return arrayOfNulls(size)
        }
    }
}
