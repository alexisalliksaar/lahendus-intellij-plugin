package ee.ut.lahendus.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import ee.ut.lahendus.intellij.data.AllSubmissionsDTO
import ee.ut.lahendus.intellij.data.CoursesDTO
import ee.ut.lahendus.intellij.data.DetailedExercise
import ee.ut.lahendus.intellij.data.ExercisesDTO
import ee.ut.lahendus.intellij.data.Submission
import java.io.InputStream
import java.net.ConnectException
import java.net.http.HttpResponse

private val LOG = logger<LahendusApiService>()

@Service
class LahendusApiService {

    fun getDetailedExercise(courseId: Int, exerciseId: Int, project: Project) {
        val errorMessagePostfix = "exercise information"
        apiGetRequest(
            DETAILED_EXERCISE_API_PATH.format(courseId, exerciseId),
            errorMessagePostfix,
            project
        ) {
            val detailedExercise = RequestUtils.fromJson<DetailedExercise>(it.body())
            detailedExercise.id = exerciseId
            detailedExercise.courseId = courseId
            project.messageBus
                .syncPublisher(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC)
                .detailedExercise(detailedExercise)
        }
    }

    fun getCourseExercises(courseId: Int, project: Project) {
        val errorMessagePostfix = "course exercises"
        apiGetRequest(
            COURSE_EXERCISES_API_PATH.format(courseId),
            errorMessagePostfix,
            project
        ) {
            val exercises = RequestUtils.fromJson<ExercisesDTO>(it.body())
            project.messageBus
                .syncPublisher(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC)
                .courseExercises(exercises.courseExercises)
        }
    }

    fun getCourses() {
        val errorMessagePostfix = "courses"
        apiGetRequest(
            COURSES_API_PATH,
            errorMessagePostfix
        ) {
            val courses = RequestUtils.fromJson<CoursesDTO>(it.body())
            ApplicationManager.getApplication().messageBus
                .syncPublisher(LahendusApplicationActionNotifier.LAHENDUS_APPLICATION_ACTION_TOPIC)
                .courses(courses.courses)
        }
    }

    fun getLatestSubmissionOrNull(detailedExercise: DetailedExercise, project: Project) {
        val allSubmissions = getAllSubmissions(detailedExercise, 1, project)
        val submission = allSubmissions.firstOrNull()

        submission?.feedbackAutoStr?.let {
            submission.autoFeedback = RequestUtils.fromJson(submission.feedbackAutoStr)
        }

        project.messageBus
            .syncPublisher(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC)
            .latestSubmissionOrNull(submission)

    }

    private fun getAllSubmissions(detailedExercise: DetailedExercise, limit: Int = -1, project: Project): List<Submission> {
        val errorMessagePostfix = "exercise submissions"

        var addr = ALL_EXERCISE_SUBMISSIONS_PATH
        if (limit > 0) {
            addr += "?limit=${limit}"
        }
        var result: List<Submission> = listOf()
        apiGetRequest(
            addr.format(detailedExercise.courseId, detailedExercise.id),
            errorMessagePostfix,
            project
        ) {
            val allSubmissions = RequestUtils.fromJson<AllSubmissionsDTO>(it.body())
            allSubmissions.submissions?.let { result = allSubmissions.submissions }
        }
        return result
    }

    fun awaitLatestSubmission(detailedExercise: DetailedExercise, project: Project) {
        val errorMessagePostfix = "exercise submission"

        apiGetRequest(
            AWAIT_LATEST_EXERCISE_SUBMISSION_API_PATH.format(detailedExercise.courseId, detailedExercise.id, project),
            errorMessagePostfix
        ) {
            val submission = RequestUtils.fromJson<Submission>(it.body())
            submission.feedbackAutoStr?.let {
                submission.autoFeedback = RequestUtils.fromJson(submission.feedbackAutoStr)
            }

            project.messageBus
                .syncPublisher(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC)
                .awaitLatestSubmission(detailedExercise, submission)
        }
    }

    fun postSolution(detailedExercise: DetailedExercise, solution: String, project: Project) {
        val errorMessagePostfix = "submit solution"

        val requestBody = RequestUtils.asJson(
            mapOf(
                "solution" to solution,
            )
        )
        apiPostRequest(
            POST_SOLUTION_PATH.format(detailedExercise.courseId, detailedExercise.id),
            requestBody,
            errorMessagePostfix,
            project
        ) {
            project.messageBus
                .syncPublisher(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC)
                .solutionSubmittedSuccessfully()
        }
    }

    companion object ApiProperties {
        val BASE_URL = RequestUtils.normaliseAddress("ems.lahendus.ut.ee/v2")

        const val COURSES_API_PATH = "/student/courses"
        const val COURSE_EXERCISES_API_PATH = "/student/courses/%d/exercises"
        const val DETAILED_EXERCISE_API_PATH = "/student/courses/%d/exercises/%d"
        const val AWAIT_LATEST_EXERCISE_SUBMISSION_API_PATH =
            "/student/courses/%d/exercises/%d/submissions/latest/await"
        const val ALL_EXERCISE_SUBMISSIONS_PATH = "/student/courses/%d/exercises/%d/submissions/all"
        const val POST_SOLUTION_PATH = "/student/courses/%d/exercises/%d/submissions"

        fun apiGetRequest(path: String, errorMessagePostfix: String, project: Project? = null, onSuccess: (HttpResponse<InputStream>) -> Unit) {
            try {
                val response = RequestUtils.sendGetRequest("${BASE_URL}${path}", RequestUtils.getAuthHeader())

                if (response.statusCode() == 200) {
                    onSuccess(response)
                } else if (response.statusCode() == 401) {
                    RequestUtils.publishAuthenticationRequired()
                } else {
                    RequestUtils.publishRequestFailedMessage("Received status code '${response.statusCode()}' from Lahendus when trying to fetch $errorMessagePostfix!", project)
                }
            } catch (e: AuthenticationService.AuthenticationRequiredException) {
                RequestUtils.publishAuthenticationRequired()
            } catch (e: ConnectException) {
                LOG.warn("Failed to fetch $path, connection error occurred")
                RequestUtils.publishNetworkErrorMessage(project)
            } catch (e: Exception) {
                RequestUtils.publishRequestFailedMessage("Something went wrong when trying to fetch $errorMessagePostfix!", project)
            }
        }

        fun apiPostRequest(
            path: String,
            body: String,
            errorMessagePostfix: String,
            project: Project? = null,
            onSuccess: (HttpResponse<InputStream>) -> Unit
        ) {
            try {
                val response = RequestUtils.sendPostRequest("${BASE_URL}${path}", body, RequestUtils.getAuthHeader())

                if (response.statusCode() == 200) {
                    onSuccess(response)
                } else if (response.statusCode() == 401) {
                    RequestUtils.publishAuthenticationRequired()
                } else {
                    RequestUtils.publishRequestFailedMessage("Received status code '${response.statusCode()}' from Lahendus when trying to $errorMessagePostfix!", project)
                }
            } catch (e: AuthenticationService.AuthenticationRequiredException) {
                RequestUtils.publishAuthenticationRequired()
            } catch (e: ConnectException) {
                RequestUtils.publishNetworkErrorMessage(project)
            }  catch (e: Exception) {
                RequestUtils.publishRequestFailedMessage("Something went wrong when trying to $errorMessagePostfix!", project)
            }
        }
    }
}