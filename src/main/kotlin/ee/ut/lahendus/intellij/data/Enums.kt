package ee.ut.lahendus.intellij.data

import com.google.gson.annotations.SerializedName

enum class ExerciseStatus {
    @SerializedName("UNSTARTED") UNSTARTED,
    @SerializedName("COMPLETED") COMPLETED,
    @SerializedName("STARTED") STARTED,
    @SerializedName("UNGRADED") UNGRADED,
}

enum class AutoGradeStatus {
    @SerializedName("NONE") NONE,
    @SerializedName("IN_PROGRESS") IN_PROGRESS,
    @SerializedName("COMPLETED") COMPLETED,
    @SerializedName("FAILED") FAILED,
}

enum class GraderType {
    @SerializedName("AUTO") AUTO,
    @SerializedName("TEACHER") TEACHER;

    override fun toString(): String {
        return when (this) {
            AUTO -> "Automatic"
            TEACHER -> "Teacher"
        }
    }
}

enum class AutoFeedbackResultType {
    @SerializedName("OK_V3") OK_V3,
    @SerializedName("OK_LEGACY") OK_LEGACY,
    @SerializedName("ERROR_V3") ERROR_V3,
}
enum class AutoFeedbackTestStatus {
    @SerializedName("FAIL") FAIL,
    @SerializedName("PASS") PASS,;

    override fun toString(): String {
        return when (this) {
            FAIL -> "❌"
            PASS -> "✔"
        }
    }}