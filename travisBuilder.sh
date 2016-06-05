#!/usr/bin/env bash

#Simple script to detect the current branch.  This is used by TavisCI server to determine if
#release actions should be performed.

# Use the gradle wrapper (gradlew) to build with Travis to ensure we have the correct gradle version
# See: https://docs.gradle.org/current/userguide/gradle_wrapper.html

if [[ $(git branch) == "master" ]]; then
    echo "Master build detected!  Preparing release build.";
    ./gradlew clean fullRelease
fi

if [[ $(git branch) != "master" ]]; then
    echo "Branch build detected.  Compile, package and perform tests but in the end we don't care about the result";
    ./gradlew clean assembleGooglePlay
fi