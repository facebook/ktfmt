package com.facebook.ktfmt.intellij;

class Notifications {

    static final String PARSING_ERROR_NOTIFICATION_GROUP = "ktfmt parsing error";
    static final String PARSING_ERROR_TITLE = PARSING_ERROR_NOTIFICATION_GROUP;

    static String parsingErrorMessage(String filename) {
        return "ktfmt failed. Does " + filename + " have syntax errors?";
    }
}
