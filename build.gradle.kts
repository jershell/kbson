val LIBRARY_VERSION_NAME = "0.4.5"
val GROUP_ID = "com.github.jershell"
val ARTIFACT_ID = rootProject.name
val BINTRAY_REPOSITORY = "generic"
val BINTRAY_ORGINIZATION = "jershell"
val KOTLINX_SERIALIZATION_RUNTIME = "1.3.3"
val SHORT_DESC = """
    This adapter adds BSON support to kotlinx.serialization.
""".trimIndent()
val VCS_URL = "https://github.com/jershell/kbson.git"
val WEBSITE_URL = "https://github.com/jershell/kbson"
val ISSUE_TRACKER_URL = "https://github.com/jershell/kbson/issues"
val CONTACT_EMAIL = "jershell@mail.ru"

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.6.21" // or kotlin("multiplatform") or any other kotlin plugin
    kotlin("plugin.serialization") version "1.6.21"
    id("maven-publish")
}


group = GROUP_ID
version = LIBRARY_VERSION_NAME



dependencies {
    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$KOTLINX_SERIALIZATION_RUNTIME")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$KOTLINX_SERIALIZATION_RUNTIME")
    implementation("org.mongodb:bson:4.6.0")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
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

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from("$rootDir/README.md")
}

fun printResults(desc: TestDescriptor, result: TestResult) {
    if (desc.parent != null) {
        val output = result.run {
            "Results: $resultType (" +
                    "$testCount tests, " +
                    "$successfulTestCount successes, " +
                    "$failedTestCount failures, " +
                    "$skippedTestCount skipped" +
                    ")"
        }
        val testResultLine = "|  $output  |"
        val repeatLength = testResultLine.length
        val seperationLine = "-".repeat(repeatLength)
        println(seperationLine)
        println(testResultLine)
        println(seperationLine)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesAll"])
            artifact(tasks["javadocJar"])
            pom {
                name.set(provider { "$GROUP_ID:$ARTIFACT_ID" })
                description.set(provider { project.description ?: SHORT_DESC })
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


