package ee.ut.lahendus.intellij.ui.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import ee.ut.lahendus.intellij.ui.UiController

class BackToExercisesAction: AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let {
            val toolwindow = UiController.getToolWindow(it)
            UiController.getExercisesTabContent(toolwindow)?.let {
                content -> toolwindow?.contentManager?.setSelectedContent(content)
            }
        }
    }
}