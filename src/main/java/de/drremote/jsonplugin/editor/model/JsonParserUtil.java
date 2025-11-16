/* === JsonParserUtil.java === */
package de.drremote.jsonplugin.editor.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.drremote.jsonplugin.editor.JSONCommentUtil;

public class JsonParserUtil {

	  private static final ObjectMapper mapper = new ObjectMapper();

	    public static JsonTreeNode parse(String text) {
	        try {
	            if (text == null || text.trim().isEmpty()) {
	                return new JsonTreeNode("empty", null, null);
	            }

	            // NEW: handle comments if enabled
	            String effectiveText = JSONCommentUtil.isCommentsEnabled()
	                    ? JSONCommentUtil.stripComments(text)
	                    : text;

	            JsonNode rootNode = mapper.readTree(effectiveText);
	            String raw = mapper.writeValueAsString(rootNode);
	            JsonTreeNode root = new JsonTreeNode("root", null, raw);
	            fill(root, rootNode);
	            return root;
	        } catch (Exception e) {
	            return new JsonTreeNode("Invalid JSON", e.getMessage(), null);
	        }
	    }

    private static void fill(JsonTreeNode parent, JsonNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                String rawChild = value.toString(); // JSON for this value
                JsonTreeNode child = new JsonTreeNode(
                        entry.getKey(),
                        value.isValueNode() ? value.toString() : null,
                        rawChild
                );
                parent.addChild(child);
                fill(child, value);
            });
        } else if (node.isArray()) {
            int index = 0;
            for (JsonNode value : node) {
                String rawChild = value.toString();
                JsonTreeNode child = new JsonTreeNode(
                        "[" + (index++) + "]",
                        value.isValueNode() ? value.toString() : null,
                        rawChild
                );
                parent.addChild(child);
                fill(child, value);
            }
        }
    }

    // NEW: decode a JSON string literal into a real Java String with \n, \t, etc.
    public static String decodeJsonStringLiteral(String jsonLiteral) {
        if (jsonLiteral == null) {
            return null;
        }
        try {
            // only if this is really a JSON string literal: "...."
            if (jsonLiteral.length() >= 2 &&
                jsonLiteral.charAt(0) == '"' &&
                jsonLiteral.charAt(jsonLiteral.length() - 1) == '"') {

                return mapper.readValue(jsonLiteral, String.class);
            }
        } catch (Exception e) {
            // ignore, just fall back to the original literal
        }
        return jsonLiteral;
    }
}
