/**
 * Generates JavaDoc website.  Jenkins picks this up as an artifact.
 * Allows standardized api usage docs.
 */
class JavadocTask extends org.gradle.api.tasks.javadoc.Javadoc {
    JavadocTask() {
        source project.android.sourceSets.main.java.srcDirs
        def androidJar = "${project.android.sdkDirectory}/platforms/${project.android.compileSdkVersion}/android.jar"
        classpath += project.files(project.android.getBootClasspath().join(File.pathSeparator), project.files(androidJar))
        destinationDir = project.file("../javadoc/")
        failOnError false
        options {
            links "http://docs.oracle.com/javase/7/docs/api/"
            linksOffline "http://d.android.com/reference", "${project.android.sdkDirectory}/docs/reference"
        }
        exclude '**/BuildConfig.java'
        exclude '**/R.java'
    }
}