package ee.ut.lahendus.intellij.ui.language

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import ee.ut.lahendus.intellij.ResourceUtils
import ee.ut.lahendus.intellij.data.GraderType
import java.io.InputStreamReader

object LanguageProvider {
    private const val PERSISTENT_PLUGIN_LANGUAGE_KEY = "ee.ut.lahendus.intellij.ui.language"
    var languageModel: LanguageModel? = null

    fun startUp() {
        val languageId = PropertiesComponent.getInstance().getValue(PERSISTENT_PLUGIN_LANGUAGE_KEY, Language.ENG.name)
        setLanguageModel(Language.valueOf(languageId))
        setToolbarActionsLanguageTooltips()
    }
    fun setSelectedLanguage(language: Language) {
        PropertiesComponent.getInstance().setValue(PERSISTENT_PLUGIN_LANGUAGE_KEY, language.name)

        ApplicationManager.getApplication().invokeLater{ ApplicationManager.getApplication().restart() }
    }

    private fun setLanguageModel(language: Language) {
        val gson = Gson()
        val languageFileStream = ResourceUtils.getResourceStream("/languages/${language.languageFileName}")!!
        languageModel = gson.fromJson<LanguageModel>(
            JsonReader(InputStreamReader(languageFileStream, Charsets.UTF_8))
            , LanguageModel::class.java)
    }

    fun getGraderTypeLiteral(graderType: GraderType): String {
        return when (graderType){
            GraderType.AUTO -> languageModel!!.selectedExerciseTab.graderType.auto
            GraderType.TEACHER -> languageModel!!.selectedExerciseTab.graderType.teacher
        }
    }
    private fun setToolbarActionsLanguageTooltips() {
        val actionIdTooltipMap = mapOf(
            "ee.ut.lahendus.intellij.ui.actions.LoginAction" to languageModel!!.actions.login,
            "ee.ut.lahendus.intellij.ui.actions.LogoutAction" to languageModel!!.actions.logout,
            "ee.ut.lahendus.intellij.ui.actions.RefreshExercisesAction" to languageModel!!.actions.refresh,
            "ee.ut.lahendus.intellij.ui.language.actions.LanguageActions" to languageModel!!.actions.languageSelection,
            "ee.ut.lahendus.intellij.ui.actions.BackToExercisesAction" to languageModel!!.actions.backToExercises,
            "ee.ut.lahendus.intellij.ui.actions.RefreshSelectedExerciseAction" to languageModel!!.actions.refresh,
            "ee.ut.lahendus.intellij.ui.actions.SendEditorToLahendusAction" to languageModel!!.actions.sendEditorToLahendus,
            "ee.ut.lahendus.intellij.ui.actions.OpenInBrowserAction" to languageModel!!.actions.openInBrowser
            )
        actionIdTooltipMap.forEach{
            entry ->
            ActionManager.getInstance().getAction(entry.key).templatePresentation.text = entry.value
        }
    }

}

enum class Language(val languageFileName: String) {
    ENG("english.json"), EST("estonian.json")
}

data class LanguageModel(
    val exercisesTab: ExercisesTabLanguageModel,
    val selectedExerciseTab: SelectedExerciseTabLanguageModel,
    val common: CommonLanguageModel,
    val automaticFeedback: AutomaticFeedbackLanguageModel,
    val uiController: UiControllerLanguageModel,
    val authService: AuthServiceLanguageModel,
    val apiService: ApiServiceLanguageModel,
    val actions: ActionsLanguageModel
)
data class ExercisesTabLanguageModel(
    val tabTitle: String,
    val authReqMsg: String,
    val authFailMsg: String,
    val loggedOutMsg: String,
    val coursesBorderTitle: String,
    val noCoursesFoundMsg: String,
    val exercisesBorderTitle: String,
    val noCourseSelectedMsg: String,
    val noExercisesFoundMsg: String,
)
data class SelectedExerciseTabLanguageModel(
    val feedbackLabel: String,
    val exerciseClosedMsg: String,
    val noSolutionsMsg: String,
    val latestSubmissionLabel: String,
    val submissionTimePrefix: String,
    val pointsLabel: String,
    val gradingMethodLabelPrefix: String,
    val graderType: GraderTypeLanguageModel,
    val teacherFeedbackLabel: String,
)
data class GraderTypeLanguageModel(
    val auto: String,
    val teacher: String
)
data class CommonLanguageModel(
    val loadingMsg: String
)
data class AutomaticFeedbackLanguageModel(
    val inputsMessage: String,
    val outputsMessage: String,
    val exceptionMessage: String,
    val createdFilesMessage: String
)
data class UiControllerLanguageModel(
    val reAuthNotificationMsg: String,
    val connectErrorMsg: String,
    val solutionSubmittedMsg: String
)
data class AuthServiceLanguageModel(
    val authSuccessMsg: String,
    val authFailMsg: String
)
data class ApiServiceLanguageModel(
    val errStatusCodeStart: String,
    val getErrStatusCodeMiddle: String,
    val postErrStatusCodeMiddle: String,
    val getGeneralExceptionMessage: String,
    val postGeneralExceptionMessage: String,
    val detExErrPostfix: String,
    val courseExErrPostfix: String,
    val courseErrPostfix: String,
    val subsErrPostfix: String,
    val subErrPostfix: String,
    val solErrPostfix: String
)
data class ActionsLanguageModel(
    val backToExercises: String,
    val login: String,
    val logout: String,
    val openInBrowser: String,
    val refresh: String,
    val sendEditorToLahendus: String,
    val languageSelection: String
)