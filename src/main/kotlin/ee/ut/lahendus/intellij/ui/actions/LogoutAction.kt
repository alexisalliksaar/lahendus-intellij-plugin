package ee.ut.lahendus.intellij.ui.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import ee.ut.lahendus.intellij.AuthenticationService

class LogoutAction: AbstractAuthenticatedAction() {
    override fun actionPerformed(e: AnActionEvent) {
        service<AuthenticationService>().logout()
    }
}