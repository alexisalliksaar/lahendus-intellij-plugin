val pluginVersion = "1.1.1"
val pluginName = "Lahendus"

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "ee.ut.lahendus.intellij"
version = pluginVersion

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}
dependencies {
    intellijPlatform {
        create("PC", "2024.2.1")

        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        id = "ee.ut.lahendus.intellij"
        name = pluginName
        version = pluginVersion
        description = """
    Integrates the Intellij Platform development environments with
    <a href="https://lahendus.ut.ee/">Lahendus</a>,
    a service operated by the
    <a href="https://cs.ut.ee/en">Institute of Computer Science at the University of Tartu</a>,
    where students submit their solutions for various exercises, such as their homework.
    <br/>
    <br/>
    The plugin enables students to track their progress in completing exercises
    and to submit the contents of their active editor directly from the IDE for a solution.
    """.trimIndent()

        ideaVersion {
            sinceBuild = "242"
            untilBuild = "242.*"
        }

        vendor {
            name = "Alexis Alliksaar"
        }
    }

    signing {
        certificateChainFile = file("certificate/chain.crt")
        privateKeyFile = file("certificate/private.pem")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}


tasks {
    wrapper {
        gradleVersion = "8.10"
    }
}
