package ee.ut.lahendus.intellij.ui.language

import com.intellij.ide.AppLifecycleListener

class LanguageStartupListener: AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        LanguageProvider.startUp()
    }
}