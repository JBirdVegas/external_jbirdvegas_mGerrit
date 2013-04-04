package com.aokp.gerrit.objects;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/3/13 6:49 PM
 */
public class ChangedFile {
    private String path;
    private int inserted;
    private int deleted;

    private ChangedFile(String _path, JSONObject object) throws JSONException {
        path = _path;
        try {
            inserted = object.getInt(JSONCommit.KEY_INSERTED);
        } catch (JSONException noInserted) {
            inserted = Integer.MIN_VALUE;
        }
        try {
            deleted = object.getInt(JSONCommit.KEY_DELETED);
        } catch (JSONException noDeleted) {
            deleted = Integer.MIN_VALUE;
        }
    }

    public String getPath() {
        return this.path;
    }

    public int getInserted() {
        return this.inserted;
    }

    public int getDeleted() {
        return this.deleted;
    }

    public static ChangedFile parseFromJSONObject(String _path, JSONObject object) throws JSONException {
        return new ChangedFile(_path, object);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ChangedFile");
        sb.append("{path='").append(path).append('\'');
        sb.append(", inserted=").append(inserted);
        sb.append(", deleted=").append(deleted);
        sb.append('}');
        return sb.toString();
    }

}