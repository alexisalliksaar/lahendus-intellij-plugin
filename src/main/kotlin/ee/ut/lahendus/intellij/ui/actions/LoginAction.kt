package ee.ut.lahendus.intellij.ui.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import ee.ut.lahendus.intellij.AuthenticationService
import ee.ut.lahendus.intellij.ui.UiController

class LoginAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        service<AuthenticationService>().startServerBG()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !UiController.userAuthenticated
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}