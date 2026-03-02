package com.projectorbit.data.db.converter

import androidx.room.TypeConverter
import com.projectorbit.data.db.entity.BodyType
import com.projectorbit.data.db.entity.LinkType

class Converters {

    @TypeConverter
    fun fromBodyType(value: BodyType): String = value.name

    @TypeConverter
    fun toBodyType(value: String): BodyType = BodyType.valueOf(value)

    @TypeConverter
    fun fromLinkType(value: LinkType): String = value.name

    @TypeConverter
    fun toLinkType(value: String): LinkType = LinkType.valueOf(value)
}
