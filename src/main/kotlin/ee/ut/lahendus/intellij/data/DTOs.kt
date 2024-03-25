package ee.ut.lahendus.intellij.data

import com.google.gson.annotations.SerializedName

data class CoursesDTO(
    @SerializedName("courses") val courses: List<Course>?
)

data class ExercisesDTO(
    @SerializedName("exercises") val courseExercises: List<CourseExercise>?
)

data class AllSubmissionsDTO(
    @SerializedName("submissions") val submissions: List<Submission>?
)
