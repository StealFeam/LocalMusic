package com.zy.ppmusic.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * @author ZhiTouPC
 */
class MenuEntity : Parcelable {
    var icon: Int = 0
    var title: String? = null

    constructor(icon: Int, title: String) {
        this.icon = icon
        this.title = title
    }

    protected constructor(`in`: Parcel) {
        icon = `in`.readInt()
        title = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(icon)
        dest.writeString(title)
    }

    companion object {

        val CREATOR: Parcelable.Creator<MenuEntity> = object : Parcelable.Creator<MenuEntity> {
            override fun createFromParcel(`in`: Parcel): MenuEntity {
                return MenuEntity(`in`)
            }

            override fun newArray(size: Int): Array<MenuEntity> {
                return newArray(size)
            }
        }
    }
}
