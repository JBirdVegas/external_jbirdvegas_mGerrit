package com.jbirdvegas.mgerrit.build.utils

import org.gradle.api.Project

class VersionHelper {
    static final String KEY_VERSION = 'version'
    Project mProject
    Properties versionProps = new Properties()
    String[] parts = null
    File versionFile

    VersionHelper(Project project) {
        mProject = project
        versionFile = new File(project.rootDir, 'version.properties')
        versionProps.load(new FileInputStream(versionFile))
        parts = versionProps[KEY_VERSION].toString().split('\\.')
    }

    // Private method but gradle strips access modifiers and this will become public... don't call it
    // this class will manage its own version incrementing
    VersionHelper increment() {
        if (mProject.hasProperty('incrementVersion') && mProject.properties['incrementVersion']) {
            println("Original version: ${getVersionName()}")
            // increment last version part
            parts[parts.length - 1] = parts[parts.length - 1].toInteger() + 1
            println("Incremented version: ${getVersionName()}")

            // write the updated properties to file
            FileOutputStream outStream = null;
            try {
                versionProps.put(KEY_VERSION, getVersionName())
                // Persist. Since bot will always increment before release using the
                // version xxx.xxx.000 is impossible. Note this quirk in the version file
                versionProps.store(new FileOutputStream(versionFile),
                        "mGerrit build bot will increment this version")
            } finally {
                if (outStream != null) {
                    outStream.close()
                }
            }
        }
        return this
    }

    String getVersionName() {
        return join(Arrays.asList(parts), '.')
    }

    int getVersionCode() {
        // Build projectVersionCode from parts
        StringBuilder builder = new StringBuilder()
        for (int i = 0; i < parts.length; i++) {
            if (i == 0) {
                builder.append(parts[i])
            } else {
                builder.append(String.format("%03d", parts[i].toInteger()))
            }
        }
        return builder.toString().toInteger()
    }

    static String join(List<String> stringList, String separator) {
        Iterator<String> iterator = stringList.iterator()
        if (iterator == null) {
            return null
        } else if (!iterator.hasNext()) {
            return ""
        } else {
            String first = iterator.next()
            if (!iterator.hasNext()) {
                return first.toString()
            } else {
                StringBuilder builder = new StringBuilder(256);
                if (first != null) {
                    builder.append(first)
                }

                while (iterator.hasNext()) {
                    if (separator != null) {
                        builder.append(separator)
                    }
                    String next = iterator.next()
                    if (next != null) {
                        builder.append(next)
                    }
                }
                return builder.toString()
            }
        }
    }
}
