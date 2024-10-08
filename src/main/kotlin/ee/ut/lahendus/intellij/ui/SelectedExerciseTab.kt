package ee.ut.lahendus.intellij.ui

import com.intellij.collaboration.ui.SimpleHtmlPane
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import ee.ut.lahendus.intellij.LahendusApiService
import ee.ut.lahendus.intellij.data.AutoFeedback.Formatter.formatAutoFeedback
import ee.ut.lahendus.intellij.data.DetailedExercise
import ee.ut.lahendus.intellij.data.GraderType
import ee.ut.lahendus.intellij.data.Submission
import ee.ut.lahendus.intellij.ui.language.LanguageProvider
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.text.html.StyleSheet

class SelectedExerciseTab(val project: Project) : SimpleToolWindowPanel(true), Disposable {
    var selectedExercise: DetailedExercise? = null
    private var contentPanel: JComponent? = null
    private var exerciseInfoPanel: ExerciseInfoPanel? = null
    var exerciseFeedbackPanel: ExerciseFeedbackPanel? = null

    init {
        UiController.connectSelectedExerciseTabToMessageBus(this)
        val actionManager = ActionManager.getInstance()
        val actionToolbar = actionManager
            .createActionToolbar(
                "Lahendus Tool Window",
                actionManager.getAction("ee.ut.lahendus.intellij.ui.actions.SelectedExerciseActions")
                        as DefaultActionGroup,
                true
            )

        actionToolbar.targetComponent = this
        toolbar = actionToolbar.component

        contentPanel = createContentPanel()
        setContent(contentPanel!!)
    }

    private fun createContentPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10)

        exerciseInfoPanel = ExerciseInfoPanel()
        exerciseFeedbackPanel = ExerciseFeedbackPanel(this)
        panel.add(exerciseInfoPanel!!, BorderLayout.PAGE_START)
        panel.add(panel { separator() })
        panel.add(exerciseFeedbackPanel!!, BorderLayout.CENTER)

        val scrollPane = JBScrollPane(panel)
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

        return scrollPane
    }


    override fun dispose() {}

    fun populateExerciseInfoContent(detailedExercise: DetailedExercise) {
        selectedExercise = detailedExercise
        exerciseInfoPanel!!.showDetailedExerciseInfo(detailedExercise)
        exerciseInfoPanel!!.revalidate()
        exerciseFeedbackPanel!!.requestLatestSubmissionFeedback(false)
    }

    fun populateExerciseFeedbackContent(submission: Submission?) {
        exerciseFeedbackPanel!!.showSubmissionFeedback(submission)
        exerciseFeedbackPanel!!.revalidate()
    }

    class ExerciseInfoPanel : VerticalBox() {
        private var titleLabel: JBLabel? = null
        private var exerciseText: JEditorPane? = null

        init {

            border = JBUI.Borders.emptyBottom(10)

            titleLabel = JBLabel()
            titleLabel!!.setCopyable(true)
            titleLabel!!.font = JBFont.h2()
            titleLabel!!.border = JBUI.Borders.emptyBottom(5)

            val styleSheet = StyleSheet()
            styleSheet.addRule("div { margin-top: auto; }")

            exerciseText = SimpleHtmlPane(additionalStyleSheet = styleSheet)

            add(titleLabel!!)
            add(exerciseText!!)
        }

        fun showDetailedExerciseInfo(detailedExercise: DetailedExercise) {
            titleLabel!!.text = detailedExercise.effectiveTitle
            var exerciseDesc = detailedExercise.htmlText.orEmpty()
            // If exercise description contains <details> html element, then replace it, as it poses challenges in rendering correctly
            if (exerciseDesc.contains("</details>")) {
                val startDetailsRegex = Regex("<details.*?>")
                val endDetailsRegex = Regex("</details>")
                val summaryRegex = Regex("<summary .*?>(?<title>.*?)</summary>")
                exerciseDesc = exerciseDesc.replace(startDetailsRegex, "<div>")
                exerciseDesc = exerciseDesc.replace(endDetailsRegex, "</div>")
                exerciseDesc = exerciseDesc.replace(summaryRegex) {
                    matchResult -> val title = matchResult.groups["title"]?.value ?: ""
                    title
                }
            }
            exerciseText!!.text = "<html><body>${exerciseDesc}</body></html"
        }
    }

    class ExerciseFeedbackPanel(private val selectedExerciseTab: SelectedExerciseTab) : JPanel() {

        private var feedbackLoadingPanel: JBLoadingPanel? = null
        private var feedbackPanelContent: JComponent? = null

        init {
            layout = BorderLayout()

            val panel = VerticalBox()
            panel.border = JBUI.Borders.compound(
                JBUI.Borders.customLineTop(JBUI.CurrentTheme.Label.foreground()),
                JBUI.Borders.emptyTop(10)
            )

            val panelTitleLabel = JBLabel(LanguageProvider.languageModel!!.selectedExerciseTab.feedbackLabel)
            panelTitleLabel.font = JBFont.h3()
            panelTitleLabel.border = JBUI.Borders.emptyBottom(5)
            panel.add(panelTitleLabel)

            feedbackPanelContent = VerticalBox()

            feedbackLoadingPanel = JBLoadingPanel(BorderLayout(), selectedExerciseTab)
            feedbackLoadingPanel!!.setLoadingText(LanguageProvider.languageModel!!.common.loadingMsg)
            feedbackLoadingPanel!!.add(feedbackPanelContent!!)
            panel.add(feedbackLoadingPanel)

            add(panel, BorderLayout.CENTER)
        }

        fun startLoading() {
            if (!feedbackLoadingPanel!!.isLoading) {
                clearFeedbackPanelContent()
                feedbackLoadingPanel!!.startLoading()
            }
        }

        fun stopLoading() {
            if (feedbackLoadingPanel!!.isLoading) {
                feedbackLoadingPanel!!.stopLoading()
            }
        }

        fun requestLatestSubmissionFeedback(await: Boolean) {
            val selectedExercise = selectedExerciseTab.selectedExercise!!
            clearFeedbackPanelContent()

            if (await) {
                startLoading()
                service<LahendusApiService>()
                    .awaitLatestSubmissionBG(selectedExercise, selectedExerciseTab.project)
            } else {
                service<LahendusApiService>()
                    .getLatestSubmissionOrNullBG(selectedExercise, selectedExerciseTab.project)
            }
            this.revalidate()
        }

        private fun clearFeedbackPanelContent() {
            feedbackPanelContent!!.removeAll()
        }

        fun showSubmissionFeedback(submission: Submission?) {

            clearFeedbackPanelContent()
            stopLoading()

            val selectedExercise = selectedExerciseTab.selectedExercise!!
            if (!selectedExercise.isOpen) {
                val closedLabel = JBLabel(LanguageProvider.languageModel!!.selectedExerciseTab.exerciseClosedMsg)
                closedLabel.font = JBFont.h4()
                closedLabel.border = JBUI.Borders.emptyBottom(5)
                feedbackPanelContent!!.add(closedLabel)
            }

            if (submission == null) {
                feedbackPanelContent!!.add(JBLabel(LanguageProvider.languageModel!!.selectedExerciseTab.noSolutionsMsg))
                return
            }


            val submittedText = JEditorPane()
            submittedText.isEditable = false
            submittedText.contentType = "text/plain"
            submittedText.text = submission.solution


            val panel = panel {
                row {
                    val latestSubmissionLabel =
                        label(LanguageProvider.languageModel!!.selectedExerciseTab.latestSubmissionLabel)
                    latestSubmissionLabel.component.font = JBFont.h4()
                }
                if (submission.submissionTime != null) {
                    row {
                        label(
                            "${LanguageProvider.languageModel!!.selectedExerciseTab.submissionTimePrefix}: " +
                                    UiController.formattedDate(submission.submissionTime)
                        )
                    }
                }
                row {
                    cell(submittedText).align(AlignX.FILL)
                }

                row {
                    val pointsLabel = label(LanguageProvider.languageModel!!.selectedExerciseTab.pointsLabel)
                    pointsLabel.component.font = JBFont.h4()
                }
                val grade = submission.grade?.grade ?: "-"
                row {
                    label("${grade}/${selectedExercise.threshold ?: "100"}")
                }
                row {

                    val graderType = submission.grade?.isAutoGrade
                        ?.let { solveGraderType(it) }
                        ?: selectedExercise.graderType

                    label(
                        "${LanguageProvider.languageModel!!.selectedExerciseTab.gradingMethodLabelPrefix}: " +
                                LanguageProvider.getGraderTypeLiteral(graderType)
                    )
                }

                if (submission.autoAssessment?.autoFeedback != null) {
                    val formattedAutoFeedback = submission.formatAutoFeedback()
                    if (formattedAutoFeedback?.autoFeedback != null) {
                        val autoFeedbackText = JEditorPane()
                        autoFeedbackText.isEditable = false
                        autoFeedbackText.contentType = "text/plain"
                        autoFeedbackText.text = formattedAutoFeedback.autoFeedback
                        row {
                            cell(autoFeedbackText).align(AlignX.FILL)
                        }
                    }
                }

                // TODO: Teacher feedback
            }

            feedbackPanelContent!!.add(panel)
        }

        private fun solveGraderType(isAutoGrade: Boolean): GraderType {
            return if (isAutoGrade) GraderType.AUTO else GraderType.TEACHER
        }
    }
}