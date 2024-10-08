package ee.ut.lahendus.intellij

import com.google.gson.JsonSyntaxException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import ee.ut.lahendus.intellij.data.AllSubmissionsDTO
import ee.ut.lahendus.intellij.data.CoursesDTO
import ee.ut.lahendus.intellij.data.DetailedExercise
import ee.ut.lahendus.intellij.data.ExercisesDTO
import ee.ut.lahendus.intellij.data.Submission
import ee.ut.lahendus.intellij.ui.language.LanguageProvider
import java.io.InputStream
import java.net.ConnectException
import java.net.http.HttpResponse

private val LOG = logger<LahendusApiService>()

@Service
class LahendusApiService {

    fun getDetailedExerciseBG(courseId: Int, exerciseId: Int, project: Project) {
        ResourceUtils.invokeOnBackgroundThread {
            getDetailedExercise(courseId, exerciseId, project)
        }
    }
    fun getCourseExercisesBG(courseId: Int, project: Project) {
        ResourceUtils.invokeOnBackgroundThread {
            getCourseExercises(courseId, project)
        }
    }
    fun getCoursesBG() {
        ResourceUtils.invokeOnBackgroundThread {
            getCourses()
        }
    }
    fun getLatestSubmissionOrNullBG(detailedExercise: DetailedExercise, project: Project) {
        ResourceUtils.invokeOnBackgroundThread {
            getLatestSubmissionOrNull(detailedExercise, project)
        }
    }
    fun awaitLatestSubmissionBG(detailedExercise: DetailedExercise, project: Project) {
        ResourceUtils.invokeOnBackgroundThread {
            awaitLatestSubmission(detailedExercise, project)
        }
    }
    fun postSolutionBG(detailedExercise: DetailedExercise, solution: String, project: Project) {
        ResourceUtils.invokeOnBackgroundThread {
            postSolution(detailedExercise, solution, project)
        }
    }

    private fun getDetailedExercise(courseId: Int, exerciseId: Int, project: Project) {
        val errorMessagePostfix = LanguageProvider.languageModel!!.apiService.detExErrPostfix
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

    private fun getCourseExercises(courseId: Int, project: Project) {
        val errorMessagePostfix = LanguageProvider.languageModel!!.apiService.courseExErrPostfix
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

    private fun getCourses() {
        val errorMessagePostfix = LanguageProvider.languageModel!!.apiService.courseErrPostfix
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

    private fun getLatestSubmissionOrNull(detailedExercise: DetailedExercise, project: Project) {
        val allSubmissions = getAllSubmissions(detailedExercise, 1, project)
        val submission = allSubmissions.firstOrNull()

        project.messageBus
            .syncPublisher(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC)
            .latestSubmissionOrNull(submission)

    }

    @Suppress("SameParameterValue")
    private fun getAllSubmissions(detailedExercise: DetailedExercise, limit: Int = -1, project: Project): List<Submission> {
        val errorMessagePostfix = LanguageProvider.languageModel!!.apiService.subsErrPostfix

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

    private fun awaitLatestSubmission(detailedExercise: DetailedExercise, project: Project) {
        val errorMessagePostfix = LanguageProvider.languageModel!!.apiService.subErrPostfix

        apiGetRequest(
            AWAIT_LATEST_EXERCISE_SUBMISSION_API_PATH.format(detailedExercise.courseId, detailedExercise.id, project),
            errorMessagePostfix
        ) {
            // await endpoint returns 200 and empty body, so need to perform the submission request again
            getLatestSubmissionOrNull(detailedExercise, project)
        }
    }

    private fun postSolution(detailedExercise: DetailedExercise, solution: String, project: Project) {
        val errorMessagePostfix = LanguageProvider.languageModel!!.apiService.solErrPostfix

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
                    RequestUtils.publishRequestFailedMessage(
                        LanguageProvider.languageModel!!.apiService.errStatusCodeStart +
                                " '${response.statusCode()}' " +
                                LanguageProvider.languageModel!!.apiService.getErrStatusCodeMiddle
                                + " $errorMessagePostfix!", project
                    )
                }
            } catch (e: AuthenticationService.AuthenticationRequiredException) {
                RequestUtils.publishAuthenticationRequired()
            } catch (e: ConnectException) {
                LOG.warn("Failed to fetch $path, connection error occurred")
                RequestUtils.publishNetworkErrorMessage(project)
            } catch (e: Exception) {
                LOG.warn("Failed get request against $path", e)
                RequestUtils.publishRequestFailedMessage(
                    LanguageProvider.languageModel!!.apiService.getGeneralExceptionMessage +
                            " $errorMessagePostfix!", project
                )
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
                    RequestUtils.publishRequestFailedMessage(LanguageProvider.languageModel!!.apiService.errStatusCodeStart +
                            " '${response.statusCode()}' " +
                            LanguageProvider.languageModel!!.apiService.postErrStatusCodeMiddle +
                            " $errorMessagePostfix!", project
                    )
                }
            } catch (e: AuthenticationService.AuthenticationRequiredException) {
                RequestUtils.publishAuthenticationRequired()
            } catch (e: ConnectException) {
                LOG.warn("Failed to post to $path, connection error occurred")
                RequestUtils.publishNetworkErrorMessage(project)
            }  catch (e: Exception) {
                LOG.warn("Failed post request against $path", e)
                RequestUtils.publishRequestFailedMessage(
                    LanguageProvider.languageModel!!.apiService.postGeneralExceptionMessage +
                            " $errorMessagePostfix!", project
                )
            }
        }
    }
}