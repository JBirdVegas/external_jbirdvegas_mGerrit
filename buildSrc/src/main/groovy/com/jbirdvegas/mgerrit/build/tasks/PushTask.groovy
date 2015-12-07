package com.jbirdvegas.mgerrit.build.tasks

import com.jbirdvegas.mgerrit.build.utils.GitUtil
import org.ajoberstar.grgit.Grgit
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PushTask extends DefaultTask {

    /**
     * Pushes the changed files to github.
     * 1) push to the feature branch `mgerrit.org-jenkins-bot`
     * 2) merge master to current branch
     * 3) checkout master
     * 4) merge feature branch to master
     * 5) push merged branch to master
     * 6) push release tags
     * 7) Remove feature branch
     */
    @TaskAction
    def push() {
        Grgit grgit = Grgit.open(project.rootDir)
        try {
            // push version update to remote
            grgit.push()
            // merge master's head into our bot branch
            grgit.merge(head: 'master')
            // switch to master branch
            grgit.checkout(branch: 'master')
            // merge our bot's branch into master
            grgit.merge(head: GitUtil.branchName)
            // push the merge to remote
            grgit.push()
            // push tags to remote
            grgit.push(tags: true)
        } finally {
            if (GitUtil.getCurrentBranch(project) == GitUtil.branchName) {
                println "Unexpected branch! {$GitUtil.branchName} Switching to master branch"
                grgit.checkout(branch: 'master')
            }
            grgit.branch.remove(names: [GitUtil.branchName], force: true)
            grgit.push()
        }
    }

}
