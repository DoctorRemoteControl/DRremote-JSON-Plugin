/* === JSONValidationUtil.java === */
package de.drremote.jsonplugin.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IDocument;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JSONValidationUtil {

	 private static final ObjectMapper mapper = new ObjectMapper();

	    private JSONValidationUtil() {
	    }

	    public static void validateJson(IFile file, IDocument doc) {
	        if (file == null || doc == null) {
	            return;
	        }
	        try {
	            file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
	        } catch (Exception e) {
	        }
	        String text = doc.get();
	        if (text == null) {
	            return;
	        }

	        // NEW: handle comments if enabled
	        String effectiveText = JSONCommentUtil.isCommentsEnabled()
	                ? JSONCommentUtil.stripComments(text)
	                : text;

	        try {
	            mapper.readTree(effectiveText);
	        } catch (JsonProcessingException e) {
            try {
                JsonLocation loc = e.getLocation();
                int line = loc == null ? 1 : loc.getLineNr();
                long charOffset = (loc == null) ? 0L : loc.getCharOffset();
                int offset;
                if (charOffset >= 0 && charOffset < Integer.MAX_VALUE) {
                    offset = (int) charOffset;
                } else {
                    offset = 0;
                }
                int docLen = doc.getLength();
                if (docLen == 0) {
                    offset = 0;
                } else if (offset < 0) {
                    offset = 0;
                } else if (offset >= docLen) {
                    offset = docLen - 1;
                }
                int length = 1;
                IMarker marker = file.createMarker(IMarker.PROBLEM);
                marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                marker.setAttribute(IMarker.MESSAGE, e.getOriginalMessage());
                marker.setAttribute(IMarker.LINE_NUMBER, line);
                marker.setAttribute(IMarker.CHAR_START, offset);
                marker.setAttribute(IMarker.CHAR_END, offset + length);
            } catch (Exception markerEx) {
                markerEx.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
