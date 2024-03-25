package ee.ut.lahendus.intellij.data

import com.google.gson.annotations.SerializedName

data class Submission(
    @SerializedName("id") val id: Int,
    @SerializedName("autograde_status") val autoGradeStatus: AutoGradeStatus,
    @SerializedName("solution") val solution: String,
    @SerializedName("submission_time") val submissionTime: String?,
    @SerializedName("grade_auto") val gradeAuto: Int?,
    @SerializedName("grade_teacher") val gradeTeacher: Int?,
    @SerializedName("feedback_auto") val feedbackAutoStr: String?,
    @SerializedName("feedback_teacher") val feedbackTeacher: String?,
    var autoFeedback: AutoFeedback?,
)
