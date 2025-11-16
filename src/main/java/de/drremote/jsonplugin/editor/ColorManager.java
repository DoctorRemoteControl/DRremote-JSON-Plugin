/* === ColorManager.java === */
package de.drremote.jsonplugin.editor;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorManager {

    private final Map<RGB, Color> colorTable = new HashMap<>();

    public Color getColor(RGB rgb) {
        Color color = colorTable.get(rgb);
        if (color == null) {
            color = new Color(Display.getCurrent(), rgb);
            colorTable.put(rgb, color);
        }
        return color;
    }

    public void dispose() {
        for (Color color : colorTable.values()) {
            if (color != null && !color.isDisposed()) {
                color.dispose();
            }
        }
        colorTable.clear();
    }
}
