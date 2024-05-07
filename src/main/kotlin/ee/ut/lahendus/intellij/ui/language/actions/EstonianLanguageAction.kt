package ee.ut.lahendus.intellij.ui.language.actions

import ee.ut.lahendus.intellij.ui.language.Language
import ee.ut.lahendus.intellij.ui.language.LanguageProvider

class EstonianLanguageAction: AbstractLanguageAction() {

    override fun languageActionPerformed() {
        LanguageProvider.setSelectedLanguage(Language.EST)
    }
}