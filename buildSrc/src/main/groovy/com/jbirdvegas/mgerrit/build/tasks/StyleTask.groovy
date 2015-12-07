import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.tasks.TaskAction

class StyleTask extends Checkstyle {
    /**
     * Code style check task.  checkstyle task runs as part of the check task
     */
    @TaskAction
    def checkstyle() {
        configFile project.file("${project.rootDir}/config/checkstyle/checkstyle.xml")
        source 'src'
        include "**/*.java"
        exclude "**/gen/**"
        classpath = files()
    }
}