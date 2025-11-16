/* === JSONFormatUtil.java === */
package de.drremote.jsonplugin.editor;

import org.eclipse.jface.text.IDocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public final class JSONFormatUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ObjectWriter PRETTY = MAPPER.writerWithDefaultPrettyPrinter();

    private JSONFormatUtil() {
    }

    public static void formatDocument(IDocument doc) throws Exception {
        if (doc == null) {
            return;
        }
        String text = doc.get();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        // NEW: handle comments if enabled
        String effectiveText = JSONCommentUtil.isCommentsEnabled()
                ? JSONCommentUtil.stripComments(text)
                : text;

        JsonNode node = MAPPER.readTree(effectiveText);
        String formatted = PRETTY.writeValueAsString(node);
        doc.set(formatted);
    }

    public static void minifyDocument(IDocument doc) throws Exception {
        if (doc == null) {
            return;
        }
        String text = doc.get();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String effectiveText = JSONCommentUtil.isCommentsEnabled()
                ? JSONCommentUtil.stripComments(text)
                : text;

        JsonNode node = MAPPER.readTree(effectiveText);
        String minified = MAPPER.writeValueAsString(node);
        doc.set(minified);
    }
}
