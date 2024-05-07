package ee.ut.lahendus.intellij.ui.language.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


abstract class AbstractLanguageAction: AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {

        ConfirmRestartIdeActionDialogWrapper {
            languageActionPerformed()
        }.show()
    }
    abstract fun languageActionPerformed()

}
class ConfirmRestartIdeActionDialogWrapper(private val okCallback: () -> Unit) : DialogWrapper(true) {
    init {
        title = "Lahendus Plugin Language"
        init()
    }

    @Suppress("RedundantNullableReturnType")
    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel(BorderLayout())

        val label = JLabel("Changing the language of the Lahendus plugin requires an IDE restart." +
                " Are you sure you want to continue?")

        dialogPanel.add(label, BorderLayout.CENTER)

        return dialogPanel
    }

    override fun doOKAction() {
        super.doOKAction()
        okCallback.invoke()
    }
}