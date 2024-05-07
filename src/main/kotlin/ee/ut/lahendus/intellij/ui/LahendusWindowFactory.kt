package ee.ut.lahendus.intellij.ui

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import ee.ut.lahendus.intellij.ui.language.LanguageProvider


private val LOG = logger<LahendusWindowFactory>()

class LahendusWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        LOG.info("Lahendus plugin startup")
        val contentManager = toolWindow.contentManager

        val authenticationTab = contentManager.factory.createContent(
            ExercisesTab(project),
            LanguageProvider.languageModel!!.exercisesTab.tabTitle,
            false
        )
        authenticationTab.isCloseable = false
        contentManager.addContent(authenticationTab)

    }

}