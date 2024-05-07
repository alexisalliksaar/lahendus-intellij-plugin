package ee.ut.lahendus.intellij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import java.io.InputStream

private val LOG = logger<ResourceUtils>()


object ResourceUtils {

    private fun getResource(path: String): String? {
        return ResourceUtils::class.java.getResource(path)?.readText()
    }

    fun getResourceStream(path: String): InputStream? {
        return ResourceUtils::class.java.getResourceAsStream(path)
    }

    private val injectionTemplate = Regex("\\{\\{\\s*(?<id>.*?)\\s*}}")
    fun getResourceWithInjection(path: String, map: Map<String, String>): String? {
        val fileText = getResource(path)
        if (fileText == null){
            LOG.warn("Couldn't find file at path: $path")
            return null
        }

        return injectionTemplate.replace(fileText) {
            matchResult -> val id = matchResult.groups["id"]?.value ?: ""
            if (map.containsKey(id)){
                map[id] ?: matchResult.value
            } else {
                LOG.warn("Didn't find injection id '${id}' in file '${path}' in the argument map")
                matchResult.value
            }
        }

    }

    fun invokeOnBackgroundThread(callback: () -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread { callback.invoke() }
    }
}