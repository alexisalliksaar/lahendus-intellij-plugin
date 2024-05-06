package ee.ut.lahendus.intellij.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import ee.ut.lahendus.intellij.AuthenticationService
import ee.ut.lahendus.intellij.LahendusApiService
import ee.ut.lahendus.intellij.LahendusApplicationActionNotifier
import ee.ut.lahendus.intellij.LahendusProjectActionNotifier
import ee.ut.lahendus.intellij.data.Course
import ee.ut.lahendus.intellij.data.CourseExercise
import ee.ut.lahendus.intellij.data.DetailedExercise
import ee.ut.lahendus.intellij.data.ExerciseStatus
import ee.ut.lahendus.intellij.data.GraderType
import ee.ut.lahendus.intellij.data.Submission
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object UiController {
    var userAuthenticated: Boolean = false

    init {
        ApplicationManager.getApplication().messageBus.connect(service<AuthenticationService>())
            .subscribe(LahendusApplicationActionNotifier.LAHENDUS_APPLICATION_ACTION_TOPIC,
                object : LahendusApplicationActionNotifier {
                    override fun authenticationSuccessful() {
                        userAuthenticated = true
                        requestCourses()
                    }
                })
    }

    fun connectExercisesTabToMessageBus(exercisesTab: ExercisesTab) {
        ApplicationManager.getApplication().messageBus.connect(exercisesTab)
            .subscribe(LahendusApplicationActionNotifier.LAHENDUS_APPLICATION_ACTION_TOPIC,
                object : LahendusApplicationActionNotifier {
                    override fun authenticationFailed() {
                        userAuthenticated = false
                        invokeLaterUI {
                            exercisesTab.showAuthenticationFailedMessage()
                        }
                    }

                    override fun reAuthenticationRequired() {
                        stopTabsIfLoading(exercisesTab.project)
                        userAuthenticated = false
                        invokeLaterUI {
                            toolWindowNotification(
                                "Please authenticate again",
                                exercisesTab.project, MessageType.WARNING
                            )
                            exercisesTab.showReAuthenticateMessage()
                        }
                    }

                    override fun courses(courses: List<Course>?) {
                        invokeLaterUI {
                            exercisesTab.showCourses(courses)
                        }
                    }

                    override fun loggedOut() {
                        userAuthenticated = false
                        invokeLaterUI {
                            exercisesTab.showLoggedOutMessage()
                        }
                    }

                    override fun networkErrorMessage() {
                        invokeLaterUI {
                            stopTabsIfLoading(exercisesTab.project)
                            showNetworkErrorMessage(exercisesTab.project)
                        }
                    }

                    override fun requestFailed(message: String) {
                        invokeLaterUI {
                        stopTabsIfLoading(exercisesTab.project)
                        showRequestFailedMessage(message, exercisesTab.project)
                            }
                    }
                })


        exercisesTab.project.messageBus.connect(exercisesTab)
            .subscribe(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC,
                object : LahendusProjectActionNotifier {
                    override fun courseExercises(courseExercises: List<CourseExercise>?) {
                        invokeLaterUI {
                            exercisesTab.showExercises(courseExercises)
                        }
                    }

                    override fun detailedExercise(detailedExercise: DetailedExercise) {

                        val toolWindow = getToolWindow(exercisesTab.project)
                        var selectedExerciseTabContent = getSelectedExerciseTabContent(toolWindow)
                        var selectedExerciseTab: SelectedExerciseTab? =
                            selectedExerciseTabContent?.component as? SelectedExerciseTab

                        if (selectedExerciseTab == null) {
                            selectedExerciseTabContent = toolWindow!!.contentManager.factory.createContent(
                                SelectedExerciseTab(exercisesTab.project),
                                detailedExercise.effectiveTitle,
                                false
                            )
                            selectedExerciseTab = selectedExerciseTabContent.component as? SelectedExerciseTab
                            selectedExerciseTab!!.populateExerciseInfoContent(detailedExercise)
                            invokeLaterUI {
                                toolWindow.contentManager.addContent(selectedExerciseTabContent)
                                toolWindow.contentManager.setSelectedContent(selectedExerciseTabContent)
                            }
                        } else {
                            invokeLaterUI {
                                selectedExerciseTab.populateExerciseInfoContent(detailedExercise)
                                selectedExerciseTabContent!!.displayName = detailedExercise.effectiveTitle
                                toolWindow!!.contentManager.setSelectedContent(selectedExerciseTabContent)
                            }
                        }
                    }

                    override fun networkErrorMessage() {
                        invokeLaterUI {
                            stopTabsIfLoading(exercisesTab.project)
                            showNetworkErrorMessage(exercisesTab.project)
                        }
                    }

                    override fun requestFailed(message: String) {
                        invokeLaterUI {
                            stopTabsIfLoading(exercisesTab.project)
                            showRequestFailedMessage(message, exercisesTab.project)
                        }
                    }

                })
    }

    fun showNetworkErrorMessage(project: Project) {
        toolWindowNotification(
            "A connection error occurred, please check your internet connection!",
            project, MessageType.ERROR
        )
    }
    fun showRequestFailedMessage(message: String, project: Project) {
        toolWindowNotification(
            message,
            project, MessageType.ERROR
        )
    }

    fun requestCourses() {
        service<LahendusApiService>().getCoursesBG()
    }

    fun refreshExercisesTab(project: Project?) {
        requestCourses()
        project?.let {
            val exercisesTab = getExercisesTab(project)
            exercisesTab?.selectedCourse?.id?.let {
                exercisesTab.requestExercises()
            }
        }
    }

    fun connectSelectedExerciseTabToMessageBus(selectedExerciseTab: SelectedExerciseTab) {
        selectedExerciseTab.project.messageBus.connect(selectedExerciseTab)
            .subscribe(LahendusProjectActionNotifier.LAHENDUS_PROJECT_ACTION_TOPIC,
                object : LahendusProjectActionNotifier {
                    override fun latestSubmissionOrNull(submission: Submission?) {
                        invokeLaterUI {
                            selectedExerciseTab.populateExerciseFeedbackContent(submission)
                        }
                    }

                    override fun awaitLatestSubmission(detailedExercise: DetailedExercise, submission: Submission) {
                        invokeLaterUI {
                            selectedExerciseTab.populateExerciseFeedbackContent(submission)
                            resolveExerciseStatusAfterSubmission(detailedExercise, submission)?.let { status ->
                                getExercisesTab(selectedExerciseTab.project)?.updateExerciseStatus(
                                    detailedExercise,
                                    status
                                )
                            }
                        }
                    }

                    override fun solutionSubmittedSuccessfully() {
                        toolWindowNotification("Solution submitted", selectedExerciseTab.project, MessageType.INFO)
                        invokeLaterUI {
                            selectedExerciseTab.exerciseFeedbackPanel!!.requestLatestSubmissionFeedback(true)
                        }
                    }
                })
    }

    fun resolveExerciseStatusAfterSubmission(
        detailedExercise: DetailedExercise,
        submission: Submission
    ): ExerciseStatus? {
        if (detailedExercise.graderType == GraderType.AUTO) {
            if (submission.gradeAuto != null && submission.gradeAuto >= (detailedExercise.threshold ?: 100))
                return ExerciseStatus.COMPLETED
            if (submission.gradeAuto != null)
                return ExerciseStatus.STARTED
        } else if (detailedExercise.graderType == GraderType.TEACHER) {
            if (submission.gradeTeacher == null) {
                return ExerciseStatus.UNGRADED
            }
        }
        return null
    }

    fun formattedDate(date: String): String {
        val dateTime = ZonedDateTime.parse(date)

        val localDateTime = dateTime.withZoneSameInstant(ZoneId.systemDefault())

        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
        return localDateTime.format(formatter)
    }

    fun toolWindowNotification(text: String, project: Project, messageType: MessageType = MessageType.INFO) {
        ToolWindowManager.getInstance(project)
            .notifyByBalloon("Lahendus", messageType, "<html><body>${text}</body></html>")
    }

    private fun getSelectedExerciseTab(project: Project): SelectedExerciseTab? {
        return getSelectedExerciseTabContent(project)?.component as? SelectedExerciseTab
    }

    private fun getSelectedExerciseTabContent(project: Project): Content? {
        return getSelectedExerciseTabContent(getToolWindow(project))
    }

    fun getSelectedExerciseTabContent(toolWindow: ToolWindow?): Content? {
        return toolWindow?.contentManager?.contents
            ?.firstOrNull { content -> content.component is SelectedExerciseTab }
    }

    fun getExercisesTab(project: Project): ExercisesTab? {
        return (getExercisesTabContent(project)?.component as? ExercisesTab)
    }

    private fun getExercisesTabContent(project: Project): Content? {
        return getExercisesTabContent(getToolWindow(project))
    }

    fun getExercisesTabContent(toolWindow: ToolWindow?): Content? {
        return toolWindow?.contentManager?.contents
            ?.firstOrNull { content -> content.component is ExercisesTab }
    }

    fun getToolWindow(project: Project): ToolWindow? {
        return ToolWindowManager.getInstance(project)
            .getToolWindow("Lahendus")
    }

    fun invokeLaterUI(callback: () -> Unit) {
        ApplicationManager.getApplication().invokeLater(
            { callback.invoke() },
            ModalityState.defaultModalityState()
        )
    }

    fun stopTabsIfLoading(project: Project){
        getExercisesTab(project)?.stopLoadingExercises()
        getSelectedExerciseTab(project)?.exerciseFeedbackPanel?.stopLoading()
    }
}