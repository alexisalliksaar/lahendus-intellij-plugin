package ee.ut.lahendus.intellij.ui.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.components.service
import ee.ut.lahendus.intellij.LahendusApiService
import ee.ut.lahendus.intellij.ui.SelectedExerciseTab

class RefreshSelectedExerciseAction: AbstractAuthenticatedAction() {
    override fun actionPerformed(e: AnActionEvent) {
        (e.dataContext.getData(PlatformCoreDataKeys.CONTEXT_COMPONENT) as? SelectedExerciseTab)?.let{
            if (it.selectedExercise?.id != null && it.selectedExercise?.courseId != null){
                service<LahendusApiService>().getDetailedExercise(
                    it.selectedExercise!!.courseId,
                    it.selectedExercise!!.id,
                    it.project
                    )
            }
        }
    }
}