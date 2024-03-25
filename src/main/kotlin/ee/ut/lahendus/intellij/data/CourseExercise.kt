package ee.ut.lahendus.intellij.data

import com.google.gson.annotations.SerializedName

data class CourseExercise(
    @SerializedName("id") val id: Int,
    @SerializedName("effective_title") val effectiveTitle: String,
    @SerializedName("status") val status: ExerciseStatus,
    @SerializedName("grader_type") val graderType: GraderType,
    )
