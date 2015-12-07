package com.jbirdvegas.mgerrit.build.plugins

import com.jbirdvegas.mgerrit.build.tasks.PushTask
import org.gradle.api.Plugin
import org.gradle.api.Project

public class CommonTasksPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        println "Adding git task"
        project.getTasks().create(TaskNames.PUSH_TASK, PushTask)
        project.getTasks().getByName(TaskNames.PUSH_TASK).description = "Commit version file to project $project.name"
        project.getTasks().create(TaskNames.COMMIT_TASK, CommitTask)
        project.getTasks().getByName(TaskNames.COMMIT_TASK).description = "Push the committed version file from project $project.name"
        project.afterEvaluate {
            println "Adding javadoc task"
            project.getTasks().create(TaskNames.JAVADOC_TASK, JavadocTask)
            project.getTasks().getByName(TaskNames.JAVADOC_TASK).description = "Create JavaDocs for project $project.name"
            println "Adding style task"
            project.getTasks().create(TaskNames.STYLE_TASK, StyleTask)
            project.getTasks().getByName(TaskNames.STYLE_TASK).description = "Checkstyle for project $project.name"
        }
    }
}