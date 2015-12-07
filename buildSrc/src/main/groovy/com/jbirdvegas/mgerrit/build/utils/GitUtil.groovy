package com.jbirdvegas.mgerrit.build.utils

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project

class GitUtil {
    /**
     * Jenkins release bot runs on its own git branch then merges the result to master.
     * This is the branch jenkins bot will run in.
     */
    static String branchName = 'mgerrit.org-jenkins-bot'

    /**
     * Gets the name of the current git branch
     * @return git branch name
     */
    static String getCurrentBranch(Project project) {
        return Grgit.open(project.rootDir).branch.getCurrent().getName()
    }
}
