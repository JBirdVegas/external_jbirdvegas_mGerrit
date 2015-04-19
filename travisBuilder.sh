#!/usr/bin/env bash

#Simple script to dectect the current branch.  This is used by TavisCI server to determine if
#release actions should be performed.

if [[ $(git branch) == "master" ]]; then
    echo "Master build detected!  Preparing release build.";
    gradle clean fullRelease
fi

if [[ $(git branch) != "master" ]]; then
    echo "Branch build detected.  Compile, package and perform tests but in the end we don't care about the result";
    gradle clean build
fi