import java.util.Properties

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("io.codearte.gradle.nexus:gradle-nexus-staging-plugin:0.30.0")
    }
}

val LIBRARY_VERSION_NAME = "0.5.1"
val GROUP_ID = "com.github.jershell"
val ARTIFACT_ID = rootProject.name
val BINTRAY_REPOSITORY = "generic"
val BINTRAY_ORGINIZATION = "jershell"
val KOTLINX_SERIALIZATION_RUNTIME = "1.4.1"
val SHORT_DESC = """
    This adapter adds BSON support to kotlinx.serialization.
""".trimIndent()
val VCS_URL = "https://github.com/jershell/kbson.git"
val WEBSITE_URL = "https://github.com/jershell/kbson"
val ISSUE_TRACKER_URL = "https://github.com/jershell/kbson/issues"
val CONTACT_EMAIL = "jershell@mail.ru"

val properties = Properties().apply {
    rootProject.file("local.properties").let { file ->
        if (file.canRead()) {
            load(file.reader())
        }
    }
}

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "1.7.20" // or kotlin("multiplatform") or any other kotlin plugin
    kotlin("plugin.serialization") version "1.7.20"
    id("maven-publish")
    signing
    id("io.codearte.nexus-staging") version "0.30.0"
}

//apply(from = "io.codearte.nexus-staging")


group = GROUP_ID
version = LIBRARY_VERSION_NAME



dependencies {
    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$KOTLINX_SERIALIZATION_RUNTIME")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$KOTLINX_SERIALIZATION_RUNTIME")
    implementation("org.mongodb:bson:4.8.2")

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
// publishing
publishing {
    repositories {

        val releasesUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        val snapshotsUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        maven {
            name = "sonatypeSnapshotRepository"
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl
            credentials {
                username = properties["mavencentral.username"] as String? ?: System.getenv("MVN_USERNAME")
                password = properties["mavencentral.password"] as String? ?: System.getenv("MVN_PASSWORD")
            }
        }
    }
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

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
    // sign(publishing.publications)
}

nexusStaging {
    serverUrl = "https://oss.sonatype.org/service/local/"
    username = properties["mavencentral.username"] as String? ?: System.getenv("MVN_USERNAME")
    password = properties["mavencentral.password"] as String? ?: System.getenv("MVN_PASSWORD")
}
