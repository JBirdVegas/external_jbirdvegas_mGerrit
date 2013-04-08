package com.aokp.gerrit;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/8/13 2:48 PM
 */
public class StaticWebAddress {
    private static String GERRIT_INSTANCE_WEBSITE = null;
    public static final String HTTP_GERRIT_SUDOSERVERS_COM = "http://gerrit.sudoservers.com/";
    private static final String CHANGES_QUERY = "changes/?q=";
    private static String STATUS_QUERY = CHANGES_QUERY + "status:";
    private static int CURRENT_GERRIT;

    public static String getGERRIT_INSTANCE_WEBSITE() {
        if (GERRIT_INSTANCE_WEBSITE == null) {
            return HTTP_GERRIT_SUDOSERVERS_COM;
        } else
            return GERRIT_INSTANCE_WEBSITE;
    }

    public static String getStatusQuery() {
        return STATUS_QUERY;
    }

    public static void setGERRIT_INSTANCE_WEBSITE(String gerrit_instance_website) {
        GERRIT_INSTANCE_WEBSITE = gerrit_instance_website;
    }

    public static String getChangesQuery() {
        return getGERRIT_INSTANCE_WEBSITE() + CHANGES_QUERY;
    }

    public static int getCURRENT_GERRIT() {
        if (CURRENT_GERRIT > 0) {
            return CURRENT_GERRIT;
        } else {
            return 1;
        }
    }

    public static void setCURRENT_GERRIT(int current_gerrit) {
        CURRENT_GERRIT = current_gerrit;
    }
}
