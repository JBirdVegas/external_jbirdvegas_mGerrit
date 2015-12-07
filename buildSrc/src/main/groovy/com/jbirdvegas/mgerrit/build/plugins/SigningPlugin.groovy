package com.jbirdvegas.mgerrit.build.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

class SigningPlugin implements Plugin<Project> {

    /**
     * Setup signing configuration.
     *
     * Requirements for release signing:
     * 1) storeFile file ${project.rootDir}, ie mGerrit_keystore.file, under the key:keyStoreFile
     * 2) storePassword set in the properties file under the key:keyStorePass
     * 3) keyStoreAlias set in the properties file under the key:keyStoreAlias
     * 4) keyStoreAliasPass set in the properties file under teh key:keyStoreAliasPass
     */
    @Override
    void apply(Project project) {
        println "Applying signing plugin"
        if (project.hasProperty('android')) {
            File keyStoreFile = new File(project.rootDir, 'mGerrit_release.keystore')
            if (keyStoreFile.exists()) {

                Properties props = new Properties()
                props.load(new FileInputStream(new File(project.rootDir, 'private.creds')))

                String keyStorePass = props['keyStorePass']
                String keyStoreAlias = props['keyStoreAlias']
                String keyStoreAliasPass = props['keyStoreAliasPass']
                if (keyStoreFile != null
                        && keyStorePass != null
                        && keyStoreAlias != null
                        && keyStoreAliasPass != null) {
                    project.android.signingConfigs.release.storeFile = keyStoreFile
                    project.android.signingConfigs.release.storePassword = keyStorePass
                    project.android.signingConfigs.release.keyAlias = keyStoreAlias
                    project.android.signingConfigs.release.keyPassword = keyStoreAliasPass
                } else {
                    println 'Not signing release.  Failed to resolve credentials'
                    project.android.buildTypes.release.signingConfig = null
                }
            } else {
                println 'Not signing release.  keyStoreFile was not defined in private.creds'
                project.android.buildTypes.release.signingConfig = null
            }
        }
    }
}