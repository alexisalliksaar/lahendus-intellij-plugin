package ee.ut.lahendus.intellij

import com.intellij.util.messages.Topic
import ee.ut.lahendus.intellij.data.CourseExercise
import ee.ut.lahendus.intellij.data.DetailedExercise
import ee.ut.lahendus.intellij.data.Submission

interface LahendusProjectActionNotifier {

    fun courseExercises(courseExercises: List<CourseExercise>?) {}
    fun detailedExercise(detailedExercise: DetailedExercise) {}
    fun latestSubmissionOrNull(submission: Submission?) {}
    fun awaitLatestSubmission(detailedExercise: DetailedExercise, submission: Submission) {}
    fun solutionSubmittedSuccessfully() {}
    fun requestFailed(message: String) {}
    fun networkErrorMessage() {}


    companion object {
        @Topic.ProjectLevel
        val LAHENDUS_PROJECT_ACTION_TOPIC : Topic<LahendusProjectActionNotifier> =
            Topic.create(
                "Lahendus api project topic",
                LahendusProjectActionNotifier::class.java
            )

    }
}