package ee.ut.lahendus.intellij.data

import com.google.gson.annotations.SerializedName

data class ExerciseGrade(
    @SerializedName("grade") val grade: Int,
    @SerializedName("is_autograde") val isAutoGrade: Boolean,
    @SerializedName("is_graded_directly") val isGradedDirectly: Boolean,
)
