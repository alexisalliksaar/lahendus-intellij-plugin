package ee.ut.lahendus.intellij.ui.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import ee.ut.lahendus.intellij.ui.UiController

abstract class AbstractAuthenticatedAction: AnAction(), DumbAware {

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = UiController.userAuthenticated
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}