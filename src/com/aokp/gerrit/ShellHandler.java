package com.aokp.gerrit;

import com.aokp.gerrit.objects.JSONCommit;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 3/31/13
 * Time: 4:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ShellHandler {
    private static String GERRIT_WEBADDRESS = "http://gerrit.sudoservers.com/changes/";
    private static String GERRIT_PARAMS = "'Accept-Type: application/json'";
    public static String GERRIT_REVIEWABLE_COMMITS
            = new StringBuilder()
                .append("curl -H ")
                .append(GERRIT_PARAMS)
                .append(" ")
                .append(GERRIT_WEBADDRESS)
                .append("?q=status:open").toString();
    public List<JSONCommit> getAllReviewableCommits() {

        return new LinkedList<JSONCommit>();
    }
}
