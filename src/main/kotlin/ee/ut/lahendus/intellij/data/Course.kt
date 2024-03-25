package ee.ut.lahendus.intellij.data

import com.google.gson.annotations.SerializedName

data class Course(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("alias") val alias: String?,
)

