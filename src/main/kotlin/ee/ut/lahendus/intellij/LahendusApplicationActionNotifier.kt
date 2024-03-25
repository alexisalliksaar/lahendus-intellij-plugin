package ee.ut.lahendus.intellij

import com.intellij.util.messages.Topic
import com.intellij.util.messages.Topic.AppLevel
import ee.ut.lahendus.intellij.data.Course


interface LahendusApplicationActionNotifier {

    fun authenticationSuccessful() {}
    fun authenticationFailed() {}
    fun reAuthenticationRequired() {}
    fun loggedOut() {}
    fun networkErrorMessage() {}
    fun requestFailed(message: String) {}
    fun courses(courses: List<Course>?) {}

    companion object {
        @AppLevel
        val LAHENDUS_APPLICATION_ACTION_TOPIC: Topic<LahendusApplicationActionNotifier> =
            Topic.create(
                "Lahendus api application topic",
                LahendusApplicationActionNotifier::class.java
        )
    }
}