package com.jbirdvegas.mgerrit.build.tasks

import com.jbirdvegas.mgerrit.build.utils.GitUtil
import com.jbirdvegas.mgerrit.build.utils.VersionHelper
import org.ajoberstar.grgit.Branch
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import org.ajoberstar.grgit.operation.BranchListOp
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class CommitTask extends DefaultTask {
    def Grgit grgit;

    /**
     * Handles local git operations.
     *
     * 1) Checkout feature branch (create if required)
     * 2) Persist version.properties
     * 2) Add changed files to git staging
     * 3) Commit changed files from git staging
     * 4) Create a tag of the new version
     */
    @TaskAction
    def commitFiles() {
        /**
         * Checkout feature branch, if branch does not exist create the branch
         */
        def branchExists = false

        if (grgit == null) {
            grgit = Grgit.open(project.rootDir)
        }

        // get a List<Branch> of all local branches
        def branches = grgit.branch.list(mode: BranchListOp.Mode.LOCAL)

        // check if branch already exists
        for (Branch branch : branches) {
            if (branch.name.equals(GitUtil.branchName)) {
                branchExists = true
            }
        }

        // switch to the new branch
        grgit.checkout(branch: GitUtil.branchName, createBranch: !branchExists)
        Properties versionProps = new Properties()

        def versionPropsFile = new File(project.rootDir, 'version.properties')
        versionProps.load(new FileInputStream(versionPropsFile))

        // update version properties
        VersionHelper helper = new VersionHelper(project)
        helper.increment()
        println 'Current Branch: ' + GitUtil.getCurrentBranch(project)
        if (versionPropsFile.exists() && versionPropsFile.canRead()) {
            // Stage changed files for commit
            grgit.add(patterns: ['version.properties'])
            println "Grgit file staging: " + grgit.status()
            // Commit the updated version.properties
            grgit.commit(message: "Release: update version to: ${helper.versionName}", amend: false)

            // Check if the version tag already exists.  If it does, don't fail.
            // Just don't push the new tag
            // TODO: This was for debugging without creating a thousand tags.
            //       Should probably remove it and fail-fast here
            List<Tag> tags = grgit.tag.list();
            boolean tagExists = false;
            for (Tag tag : tags) {
                if (tag.name.contains("v${helper.versionName}")) {
                    tagExists = true;
                }
            }
            if (!tagExists) {
                // Create tag for version
                String message = "Release of v${helper.versionName}, ${new Date()}"
                grgit.tag.add(name: "v${helper.versionName}", message: message)
            }
        } else {
            throw new GradleException("FATAL: Missing ${versionPropsFile.absolutePath}")
        }
    }
}