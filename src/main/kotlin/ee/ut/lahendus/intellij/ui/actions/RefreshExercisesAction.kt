package ee.ut.lahendus.intellij.ui.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import ee.ut.lahendus.intellij.ui.UiController

class RefreshExercisesAction: AbstractAuthenticatedAction() {
    override fun actionPerformed(e: AnActionEvent) {
        UiController.refreshExercisesTab(e.project)
    }
}