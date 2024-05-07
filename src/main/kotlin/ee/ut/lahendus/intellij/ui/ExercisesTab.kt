package ee.ut.lahendus.intellij.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalBox
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.util.maximumWidth
import com.intellij.ui.util.preferredHeight
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import ee.ut.lahendus.intellij.LahendusApiService
import ee.ut.lahendus.intellij.data.Course
import ee.ut.lahendus.intellij.data.CourseExercise
import ee.ut.lahendus.intellij.data.DetailedExercise
import ee.ut.lahendus.intellij.data.ExerciseStatus
import ee.ut.lahendus.intellij.ui.language.LanguageProvider
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator

class ExercisesTab(val project: Project) : SimpleToolWindowPanel(true), Disposable {
    var selectedCourse: Course? = null
    private var contentPanel: JComponent? = null
    private var exercisesPanel: JComponent? = null
    private var coursesPanel: JComponent? = null

    init {
        UiController.connectExercisesTabToMessageBus(this)

        val actionManager = ActionManager.getInstance()
        val actionToolbar = actionManager
            .createActionToolbar(
                "Lahendus Tool Window",
                actionManager.getAction("ee.ut.lahendus.intellij.ui.actions.ExercisesActions")
                        as DefaultActionGroup,
                true
            )
        actionToolbar.targetComponent = this
        toolbar = actionToolbar.component

        contentPanel = createContentPanel()

        if (UiController.userAuthenticated) {
            UiController.requestCourses()
        } else {
            showAuthenticationMessage()
        }
    }

    override fun dispose() {}
    fun showAuthenticationFailedMessage() {
        showMessage(LanguageProvider.languageModel!!.exercisesTab.authFailMsg)
    }

    private fun showAuthenticationMessage() {
        showMessage(LanguageProvider.languageModel!!.exercisesTab.authReqMsg)
    }

    fun showReAuthenticateMessage() {
        showAuthenticationMessage()
    }

    fun showLoggedOutMessage() {
        selectedCourse = null
        exercisesPanelListPanel?.removeAll()
        showMessage(LanguageProvider.languageModel!!.exercisesTab.loggedOutMsg)
    }

    private fun createContentPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(10)

        coursesPanel = createCoursesPanel()
        exercisesPanel = createExercisesPanel()

        panel.add(coursesPanel!!, BorderLayout.PAGE_START)
        panel.add(JSeparator())
        panel.add(exercisesPanel!!, BorderLayout.CENTER)

        return JBScrollPane(panel)
    }

    private fun createCoursesPanel(): JComponent {
        val panel = VerticalBox()

        panel.border = JBUI.Borders.compound(
            JBUI.Borders.emptyBottom(5),
            BorderFactory.createTitledBorder(LanguageProvider.languageModel!!.exercisesTab.coursesBorderTitle),
            JBUI.Borders.empty(10)
        )
        return panel
    }

    private fun populateCoursesPanel(courses: List<Course>?) {
        coursesPanel!!.removeAll()

        courses?.let {
            if (courses.isEmpty()) {
                coursesPanel!!.add(JBLabel(LanguageProvider.languageModel!!.exercisesTab.noCoursesFoundMsg))
            }

            courses.forEach { course ->
                val courseTitle = course.alias ?: course.title
                val courseLink = createLink(courseTitle, onClick = {
                    if ((selectedCourse?.id ?: -1) != course.id) {
                        selectedCourse = course
                        populateExercisesPanel()
                        requestExercises()
                    }
                })
                courseLink.border = JBUI.Borders.empty(1, 0)
                coursesPanel!!.add(courseLink)
            }
        }
        coursesPanel!!.revalidate()
    }

    private fun startLoadingExercises() {
        if (!exercisesPanelLoadingPanel!!.isLoading) {
            exercisesPanelListPanel!!.removeAll()
            exercisesPanelLoadingPanel!!.startLoading()
        }
    }

    fun stopLoadingExercises() {
        if (exercisesPanelLoadingPanel!!.isLoading) {
            exercisesPanelLoadingPanel!!.stopLoading()
        }
    }

    fun requestExercises(startLoading: Boolean = true) {
        if (startLoading) {
            startLoadingExercises()
        }
        service<LahendusApiService>().getCourseExercisesBG(selectedCourse!!.id, project)
        exercisesPanel!!.revalidate()
    }

    private var exercisesPanelLoadingPanel: JBLoadingPanel? = null
    private var exercisesPanelTitleLabel: JLabel? = null
    private var exercisesPanelListPanel: JComponent? = null
    private fun createExercisesPanel(): JComponent {
        val panel = VerticalBox()
        panel.border = JBUI.Borders.compound(
            JBUI.Borders.emptyTop(5),
            BorderFactory.createTitledBorder(LanguageProvider.languageModel!!.exercisesTab.exercisesBorderTitle),
            JBUI.Borders.empty(10)
        )
        exercisesPanelTitleLabel = JBLabel()
        exercisesPanelTitleLabel!!.border = JBUI.Borders.emptyBottom(5)
        panel.add(exercisesPanelTitleLabel)

        panel.add(JSeparator())

        exercisesPanelListPanel = VerticalBox()
        exercisesPanelListPanel!!.border = JBUI.Borders.emptyTop(5)

        exercisesPanelLoadingPanel = JBLoadingPanel(BorderLayout(), this)
        exercisesPanelLoadingPanel!!.setLoadingText(LanguageProvider.languageModel!!.common.loadingMsg)
        exercisesPanelLoadingPanel!!.add(exercisesPanelListPanel!!)
        panel.add(exercisesPanelLoadingPanel)

        populateExercisesPanel()

        return panel
    }

    private fun populateExercisesPanel() {
        if (selectedCourse == null) {
            exercisesPanelTitleLabel!!.text = LanguageProvider.languageModel!!.exercisesTab.noCourseSelectedMsg
        } else {
            exercisesPanelTitleLabel!!.text = selectedCourse!!.alias ?: selectedCourse!!.title
            exercisesPanelTitleLabel!!.font = JBFont.h3()
        }
    }

    private fun populateExercisesPanelList(courseExercises: List<CourseExercise>?) {
        stopLoadingExercises()
        exercisesPanelListPanel!!.removeAll()
        courseExercises?.let {
            courseExercises.ifEmpty {
                exercisesPanelListPanel!!.add(JBLabel(LanguageProvider.languageModel!!.exercisesTab.noExercisesFoundMsg))
            }

            courseExercises.forEach { courseExercise ->

                val exerciseLink = createLink(courseExercise.effectiveTitle, onClick = {
                    service<LahendusApiService>()
                        .getDetailedExerciseBG(selectedCourse!!.id, courseExercise.id, project)
                })

                val exerciseLinkPanel = ExerciseLinkPanel(courseExercise, exerciseLink)
                exerciseLinkPanel.maximumSize = JBDimension(exerciseLinkPanel.maximumWidth, exerciseLinkPanel.preferredHeight)

                exercisesPanelListPanel!!.add(exerciseLinkPanel)
            }
        }
    }

    class ExerciseLinkPanel(val courseExercise: CourseExercise, exerciseLink: JLabel) : JPanel() {
        private val iconLabel = JBLabel()

        init {
            layout = FlowLayout(FlowLayout.LEFT, 3, 2)
            border = JBUI.Borders.empty(1, 0)
            setStatusIcon(courseExercise.status)
            add(iconLabel)
            add(exerciseLink)
        }

        fun setStatusIcon(exerciseStatus: ExerciseStatus) {
            iconLabel.icon = StatusIcons.getIcon(exerciseStatus)
        }
    }

    fun updateExerciseStatus(detailedExercise: DetailedExercise, exerciseStatus: ExerciseStatus) {
        if (selectedCourse != null && selectedCourse!!.id == detailedExercise.courseId) {
            exercisesPanelListPanel?.components?.map { it as? ExerciseLinkPanel }
                ?.firstOrNull { it?.courseExercise?.id == detailedExercise.id }
                ?.setStatusIcon(exerciseStatus)
        }
    }

    fun showCourses(courses: List<Course>?) {
        populateCoursesPanel(courses)
        setContent(contentPanel!!)
    }

    fun showExercises(courseExercises: List<CourseExercise>?) {
        populateExercisesPanelList(courseExercises)
        exercisesPanel!!.revalidate()
    }

    private fun showMessage(message: String) {
        val panel = panel {
            row {
                label(message).resizableColumn()
            }
        }
        panel.border = JBUI.Borders.empty(10)
        setContent(panel)
    }

    private fun createLink(text: String, onClick: () -> Unit): JLabel {
        val link = JBLabel(text)
        val defaultColor = JBUI.CurrentTheme.Link.Foreground.ENABLED
        val hoveredColor = JBUI.CurrentTheme.Link.Foreground.HOVERED
        val hoveredFallbackColor = JBUI.CurrentTheme.Link.Foreground.ENABLED.brighter()

        link.foreground = defaultColor
        link.cursor = Cursor(Cursor.HAND_CURSOR)
        link.font = JBFont.regular().biggerOn(1F)

        link.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                onClick.invoke()
                link.foreground = defaultColor
            }

            override fun mouseEntered(e: MouseEvent?) {
                link.foreground = if (hoveredColor == defaultColor) hoveredFallbackColor else hoveredFallbackColor
            }

            override fun mouseExited(e: MouseEvent?) {
                link.foreground = defaultColor
            }
        })

        return link
    }

    companion object StatusIcons {
        @JvmField
        val completed = IconLoader.getIcon("/icons/completed.svg", ExercisesTab::class.java)

        @JvmField
        val started = IconLoader.getIcon("/icons/started.svg", ExercisesTab::class.java)

        @JvmField
        val unstarted = IconLoader.getIcon("/icons/unStarted.svg", ExercisesTab::class.java)

        @JvmField
        val ungraded = IconLoader.getIcon("/icons/unGraded.svg", ExercisesTab::class.java)

        fun getIcon(exerciseStatus: ExerciseStatus): Icon {

            return when (exerciseStatus) {
                ExerciseStatus.UNSTARTED -> unstarted
                ExerciseStatus.COMPLETED -> completed
                ExerciseStatus.STARTED -> started
                ExerciseStatus.UNGRADED -> ungraded
            }
        }
    }
}
