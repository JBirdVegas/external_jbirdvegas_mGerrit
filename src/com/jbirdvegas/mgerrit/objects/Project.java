package com.jbirdvegas.mgerrit.objects;

/**
 * Created by jbird on 5/23/13.
 */
public final class Project implements Comparable<Project>{
    private final String mPath;
    private final String mKind;
    private final String mId;

    private Project(String path, String kind, String id) {
        this.mPath = path;
        this.mKind = kind;
        this.mId = id;
    }

    public static Project getInstance(String path, String kind, String id) {
        return new Project(path, kind, id);
    }

    public String getmKind() {
        return mKind;
    }

    public String getmId() {
        return mId;
    }

    public String getmPath() {
        return mPath;
    }

    @Override
    public String toString() {
        return mPath;
    }

    @Override
    public int compareTo(Project project) {
        int i = mPath.compareTo(project.getmPath());
        if (i != 0) return i;

        i = mKind.compareTo(project.getmKind());
        if (i != 0) return i;

        i = mId.compareTo(project.getmId());
        return i;
    }
}
