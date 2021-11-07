import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    groovy
    java
    checkstyle
    jacoco
    // application
    // distribution
    kotlin("jvm") version "1.5.20"
    id("com.github.spotbugs") version "4.7.9"
    id("com.diffplug.spotless") version "5.17.1"
    id("com.github.kt3k.coveralls") version "2.12.0"
    id("com.palantir.git-version") version "0.12.3" apply false
}

fun getProps(f: File): Properties {
    val props = Properties()
    try {
        props.load(FileInputStream(f))
    } catch (t: Throwable) {
        println("Can't read $f: $t, assuming empty")
    }
    return props
}

// we handle cases without .git directory
val home = System.getProperty("user.home")
val javaHome = System.getProperty("java.home")
val props = project.file("src/main/resources/version.properties")
val dotgit = project.file(".git")
if (dotgit.exists()) {
    apply(plugin = "com.palantir.git-version")
    val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
    val details = versionDetails()
    val baseVersion = details.lastTag.substring(1)
    if (details.isCleanTag) {  // release version
        version = baseVersion
    } else {  // snapshot version
        version = baseVersion + "-" + details.commitDistance + "-" + details.gitHash + "-SNAPSHOT"
    }
} else if (props.exists()) { // when version.properties already exist, just use it.
    version = getProps(props).getProperty("version")
}

tasks.register("writeVersionFile") {
    val folder = project.file("src/main/resources");
    if (!folder.exists()) {
        folder.mkdirs()
    }
    props.delete()
    props.appendText("version=" + project.version)
}

tasks.getByName("jar") {
    dependsOn("writeVersionFile")
}

group = "tokyo.northside"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

//application {
//    mainClass.set("")
//}
//application.applicationDistribution.into("") {
//    from("README.md", "COPYING")
//}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation("org.codehaus.groovy:groovy-all:3.0.9")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()

    // Test in headless mode with ./gradlew test -Pheadless
    if (project.hasProperty("headless")) {
        systemProperty("java.awt.headless", "true")
    }
}

jacoco {
    toolVersion="0.8.6"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true  // coveralls plugin depends on xml format report
        html.isEnabled = true
    }
}

coveralls {
    jacocoReportPath = "build/reports/jacoco/test/jacocoTestReport.xml"
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
    options.compilerArgs.add("-Xlint:unchecked")
}

// Disable .tar distributions
// tasks.getByName("distTar").enabled = false
// distributions {
//     create("source") {
//         contents {
//             from (".")
//             exclude ("out", "build", ".gradle", ".github", ".idea", ".gitignore")
//         }
//     }
// }
