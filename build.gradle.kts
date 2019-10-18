import java.util.Date

val LIBRARY_VERSION_NAME = "0.1.3"
val GROUP_ID = "com.github.jershell"
val ARTIFACT_ID = rootProject.name
val BINTRAY_REPOSITORY = "generic"
val BINTRAY_ORGINIZATION = "jershell"
val KOTLINX_SERIALIZATION_RUNTIME = "0.11.1"
val SHORT_DESC = """
    This adapter adds BSON support to kotlinx.serialization.
""".trimIndent()
val VCS_URL = "https://github.com/jershell/kbson.git"
val WEBSITE_URL = "https://github.com/jershell/kbson"
val ISSUE_TRACKER_URL = "https://github.com/jershell/kbson/issues"
val CONTACT_EMAIL = "jershell@mail.ru"

buildscript {
    repositories { jcenter() }
    val kotlin_version = "1.3.50"
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlin_version")
    }
}

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM.
    id("org.jetbrains.kotlin.jvm").version("1.3.50")
    id("maven-publish")
    id("com.jfrog.bintray") version "1.8.4"
}

apply(plugin="kotlinx-serialization")

group = GROUP_ID
version = LIBRARY_VERSION_NAME

tasks.bintrayUpload {
    dependsOn("publishToMavenLocal")
}

tasks.register<Jar>("sourcesAll") {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

tasks.withType<GenerateMavenPom>().configureEach {
    val matcher = Regex("""generatePomFileFor(\w+)Publication""").matchEntire(name)
    val publicationName = matcher?.let { it.groupValues[1] }
    destination = file("$buildDir/poms/$publicationName-pom.xml")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesAll"])
            pom {
                name.set(provider { "$GROUP_ID:$ARTIFACT_ID" })
                description.set(provider { project.description ?: SHORT_DESC  })
                url.set(VCS_URL)
                developers {
                    developer {
                        name.set(BINTRAY_ORGINIZATION)
                        email.set(CONTACT_EMAIL)
                    }
                }
                scm {
                    connection.set(VCS_URL)
                    developerConnection.set(VCS_URL)
                    url.set(ISSUE_TRACKER_URL)
                }
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }
            }
        }
    }
}


bintray {
    user = project.property("user_name").toString()
    key = project.property("apikey").toString()
    publish = true
    override = false
    setPublications("mavenJava")
    pkg.apply {
        repo = BINTRAY_REPOSITORY
        name = ARTIFACT_ID
        userOrg = BINTRAY_ORGINIZATION
        desc = SHORT_DESC
        setLicenses("MIT")
        version.apply {
            name = LIBRARY_VERSION_NAME
            vcsTag = LIBRARY_VERSION_NAME
            released = Date().toString()
        }

        vcsUrl = VCS_URL
        websiteUrl = WEBSITE_URL
        issueTrackerUrl = ISSUE_TRACKER_URL

        setLabels("kotlin", "bson", "serialization", "jvm", "mongo", "mongodb", "kmongo")
    }
}

repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$KOTLINX_SERIALIZATION_RUNTIME")
    implementation("org.mongodb:bson:3.11.0")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}


