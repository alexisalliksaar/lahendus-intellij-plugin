package ee.ut.lahendus.intellij.data

import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import ee.ut.lahendus.intellij.RequestUtils

data class Submission(
    @SerializedName("id") val id: Int,
    @SerializedName("autograde_status") val autoGradeStatus: AutoGradeStatus,
    @SerializedName("submission_status") val submissionStatus: ExerciseStatus,
    @SerializedName("solution") val solution: String,
    @SerializedName("submission_time") val submissionTime: String?,
    @SerializedName("grade") val grade: ExerciseGrade?,
    @SerializedName("auto_assessment") val autoAssessment: AutoAssessment?
) {
    data class AutoAssessment(
        @SerializedName("grade") val gradeInt: Int,

        @JsonAdapter(AutoFeedbackAdapter::class)
        @SerializedName("feedback") val autoFeedback: AutoFeedback
    )

    class AutoFeedbackAdapter: TypeAdapter<AutoFeedback>() {
        override fun write(jsonWriter: JsonWriter?, autoFeedback: AutoFeedback?) {
            throw NotImplementedError()
        }

        override fun read(jsonReader: JsonReader?): AutoFeedback? {
            return jsonReader?.nextString()?.let {
                try {
                    RequestUtils.fromJson<AutoFeedback>(it, true)
                } catch (ignored: JsonSyntaxException){
                    null
                }
            }
        }

    }
}
