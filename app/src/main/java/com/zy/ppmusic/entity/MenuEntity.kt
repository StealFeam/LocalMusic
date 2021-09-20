package com.zy.ppmusic.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * @author stealfeam
 */
class MenuEntity() : Parcelable {
    var icon: Int = 0
    var title: String? = null

    constructor(parcel: Parcel) : this() {
        icon = parcel.readInt()
        title = parcel.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(icon)
        dest.writeString(title)
    }

    companion object CREATOR : Parcelable.Creator<MenuEntity> {
        override fun createFromParcel(parcel: Parcel): MenuEntity {
            return MenuEntity(parcel)
        }

        override fun newArray(size: Int): Array<MenuEntity?> {
            return arrayOfNulls(size)
        }
    }
}
