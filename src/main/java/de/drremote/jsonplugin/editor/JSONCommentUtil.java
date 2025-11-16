package de.drremote.jsonplugin.editor;

public final class JSONCommentUtil {

    private static boolean commentsEnabled = true;

    private JSONCommentUtil() {
    }

    public static boolean isCommentsEnabled() {
        return commentsEnabled;
    }

    public static void setCommentsEnabled(boolean enabled) {
        commentsEnabled = enabled;
    }

    public static String stripComments(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        boolean inString = false;
        boolean escaped = false;
        boolean inSingleLineComment = false;
        boolean inMultiLineComment = false;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            char next = (i + 1 < len) ? text.charAt(i + 1) : '\0';
            if (inSingleLineComment) {
                if (c == '\n' || c == '\r') {
                    inSingleLineComment = false;
                    out.append(c);
                }
                continue;
            }
            if (inMultiLineComment) {
                if (c == '*' && next == '/') {
                    inMultiLineComment = false;
                    i++;
                }
                continue;
            }
            if (inString) {
                out.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
                continue;
            }
            if (c == '/' && next == '/') {
                inSingleLineComment = true;
                i++;
                continue;
            }
            if (c == '/' && next == '*') {
                inMultiLineComment = true;
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}
