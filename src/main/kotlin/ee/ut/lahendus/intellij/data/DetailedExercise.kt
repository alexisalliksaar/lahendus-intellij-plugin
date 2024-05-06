package ee.ut.lahendus.intellij.data

import com.google.gson.annotations.SerializedName

data class DetailedExercise(
    @SerializedName("effective_title") val effectiveTitle: String,
    @SerializedName("text_html") val htmlText: String?,
    @SerializedName("solution_file_name") val solutionFileName: String,
    @SerializedName("grader_type") val graderType: GraderType,
    @SerializedName("is_open") val isOpen: Boolean,
    @SerializedName("threshold") val threshold: Int?,
    var id: Int,
    var courseId: Int,
)
