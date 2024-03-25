package ee.ut.lahendus.intellij.ui.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.project.DumbAware
import ee.ut.lahendus.intellij.AuthenticationService
import ee.ut.lahendus.intellij.RequestUtils
import ee.ut.lahendus.intellij.ui.SelectedExerciseTab

class OpenInBrowserAction: AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val selectedExerciseTab = (e.dataContext.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? SelectedExerciseTab)
        selectedExerciseTab?.selectedExercise?.let {
            val path = "${AuthenticationService.IDP_CLIENT_NAME}/courses/${it.courseId}/exercises/${it.id}/summary"
            BrowserUtil.open(RequestUtils.normaliseAddress(path))
        }
    }
}