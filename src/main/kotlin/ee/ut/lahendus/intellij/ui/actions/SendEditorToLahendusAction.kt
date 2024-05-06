package ee.ut.lahendus.intellij.ui.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import ee.ut.lahendus.intellij.LahendusApiService
import ee.ut.lahendus.intellij.ui.SelectedExerciseTab

class SendEditorToLahendusAction: AbstractAuthenticatedAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.project?.let {
            if (FileEditorManager.getInstance(it).selectedEditor == null) {
                e.presentation.isEnabled = false
            }
        }
        val selectedExerciseTab = (e.dataContext.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? SelectedExerciseTab)
        if (selectedExerciseTab?.selectedExercise == null || !selectedExerciseTab.selectedExercise!!.isOpen) {
            e.presentation.isEnabled = false
        }

    }

    override fun actionPerformed(e: AnActionEvent) {
        val selectedExerciseTab = (e.dataContext.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? SelectedExerciseTab)
        selectedExerciseTab?.let {
            val selectedEditor = FileEditorManager.getInstance(selectedExerciseTab.project).getSelectedEditor()
            if (selectedEditor != null) {
                val editorText = (selectedEditor as? TextEditor)?.editor?.document?.text
                if (editorText != null && selectedExerciseTab.selectedExercise != null) {
                    selectedExerciseTab.exerciseFeedbackPanel?.startLoading()
                    service<LahendusApiService>()
                        .postSolutionBG(selectedExerciseTab.selectedExercise!!, editorText, selectedExerciseTab.project)
                }
            }
        }
    }
}