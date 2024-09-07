package ee.ut.lahendus.intellij.data

import com.google.gson.annotations.SerializedName
import ee.ut.lahendus.intellij.ui.language.LanguageProvider

@Suppress("RedundantExplicitType", "SimplifiableCallChain")
class AutoFeedback(
    @SerializedName("result_type") val resultType: AutoFeedbackResultType?,
    @SerializedName("pre_evaluate_error") val preEvaluateError: String?,
    @SerializedName("points") val points: Int?,
    @SerializedName("error") val error: String?,
    @SerializedName("feedback") val feedback: String?,
    @SerializedName("tests") val tests: List<AutoFeedbackTest>?,
) {

    data class AutoFeedbackTest(
        @SerializedName("title") val title: String?,
        @SerializedName("status") val status: AutoFeedbackTestStatus?,
        @SerializedName("user_inputs") val userInputs: List<String>?,
        @SerializedName("actual_output") val actualOutput: String?,
        @SerializedName("exception_message") val exceptionMessage: String?,
        @SerializedName("created_files") val createdFiles: List<AutoFeedbackTestCreatedFile>?,
        @SerializedName("checks") val checks: List<AutoFeedbackTestCheck>?,
    )
    data class AutoFeedbackTestCreatedFile(
        @SerializedName("name") val name: String?,
        @SerializedName("content") val content: String?,
    )

    data class AutoFeedbackTestCheck(
        @SerializedName("feedback") val feedback: String?,
        @SerializedName("status") val status: AutoFeedbackTestStatus?,
    )

    companion object Formatter {
        private const val TAB = "   * "
        private const val HALF_TAB = "  "

        private fun formatCheck(check: AutoFeedbackTestCheck): String {
            return "${check.status}: ${check.feedback}"
        }

        private fun formatChecks(checks: List<AutoFeedbackTestCheck>?): String {
            return checks?.map { formatCheck(it) }
                ?.joinToString(separator = "\n$TAB") ?: ""
        }

        private fun formatCreatedFile(createdFile: AutoFeedbackTestCreatedFile): String {
            return "$TAB---${createdFile.name}---\n${createdFile.content}"
                .replace("\n", "\n$TAB")
        }

        private fun formatCreatedFiles(createdFiles: List<AutoFeedbackTestCreatedFile>?): String {
            return createdFiles?.map { formatCreatedFile(it) }
                ?.joinToString(separator = "$HALF_TAB\n\n") ?: ""
        }

        private fun formatTest(test: AutoFeedbackTest): String {

            val checksFormatted = formatChecks(test.checks)

            var userInputsFormatted: String = ""
            test.userInputs?.let {
                if (test.userInputs.isNotEmpty()) {
                    userInputsFormatted = test.userInputs.joinToString(separator = "\n$TAB")
                    userInputsFormatted = HALF_TAB +
                            LanguageProvider.languageModel!!.automaticFeedback.inputsMessage +
                            ":\n${TAB}${userInputsFormatted}\n"
                }
            }

            var actualOutputFormatted: String = ""
            test.actualOutput?.let {
                if (test.actualOutput.isNotEmpty()) {
                    actualOutputFormatted = test.actualOutput.split("\n")
                        .joinToString(separator = "\n$TAB")
                    actualOutputFormatted = HALF_TAB +
                            LanguageProvider.languageModel!!.automaticFeedback.outputsMessage +
                            ":\n${TAB}${actualOutputFormatted}"
                }
            }

            var exceptionMessageFormatted: String = ""
            test.exceptionMessage?.let {
                exceptionMessageFormatted = "${TAB}${test.exceptionMessage.replace("\n", "\n${TAB}")}"
                exceptionMessageFormatted = HALF_TAB +
                        LanguageProvider.languageModel!!.automaticFeedback.exceptionMessage +
                        ":\n\n${exceptionMessageFormatted}\n"
            }

            var createdFilesMessageFormatted: String = ""
            test.createdFiles?.let {
                if (test.createdFiles.isNotEmpty()) {
                    createdFilesMessageFormatted = formatCreatedFiles(test.createdFiles)
                    createdFilesMessageFormatted = HALF_TAB +
                            LanguageProvider.languageModel!!.automaticFeedback.createdFilesMessage +
                            ":\n\n${createdFilesMessageFormatted}\n\n"
                }
            }

            return "${test.status}: ${test.title}\n" +
                    "${HALF_TAB}${checksFormatted}\n" +
                    exceptionMessageFormatted +
                    userInputsFormatted +
                    createdFilesMessageFormatted +
                    actualOutputFormatted
        }

        private fun formatAutoFeedback(autoFeedback: AutoFeedback): FormattedAutoFeedback? {
            val pointsString = if (autoFeedback.points != null) autoFeedback.points.toString()  else "-"

            if (autoFeedback.resultType == AutoFeedbackResultType.OK_V3) {
                if (autoFeedback.preEvaluateError == null) {
                    val formattedTests = autoFeedback.tests?.map { test -> formatTest(test) }
                        ?.joinToString(separator = "\n") ?: ""
                    return FormattedAutoFeedback(formattedTests, pointsString)
                } else {
                    return FormattedAutoFeedback(autoFeedback.preEvaluateError, pointsString)
                }
            } else if (autoFeedback.resultType == AutoFeedbackResultType.OK_LEGACY) {
                return FormattedAutoFeedback(autoFeedback.feedback.toString(), pointsString)
            } else if (autoFeedback.resultType == AutoFeedbackResultType.ERROR_V3) {
                return FormattedAutoFeedback(autoFeedback.error.toString(), "-")
            } else {
                return null
            }
        }

        fun Submission.formatAutoFeedback(): FormattedAutoFeedback? {
            return this.autoAssessment?.autoFeedback?.let { formatAutoFeedback(it) }
        }

        data class FormattedAutoFeedback(val autoFeedback: String?, val autoGrade: String)
    }

}
