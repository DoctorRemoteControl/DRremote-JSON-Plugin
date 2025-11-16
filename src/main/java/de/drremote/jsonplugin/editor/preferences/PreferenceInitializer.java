/* === PreferenceInitializer.java === */
package de.drremote.jsonplugin.editor.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = PreferenceStoreUtil.getStore();

        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_DEFAULT, new RGB(0, 0, 0));

        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_STRING,  new RGB(42, 0, 255));
        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_NUMBER,  new RGB(0, 128, 0));
        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_BOOLEAN, new RGB(127, 0, 85));
        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_NULL,    new RGB(128, 128, 128));

        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_KEY,     new RGB(0, 0, 192));

        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_BRACE,   new RGB(64, 64, 64));
        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_BRACKET, new RGB(64, 64, 64));
        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_COLON,   new RGB(64, 64, 64));
        PreferenceConverter.setDefault(store, PreferenceConstants.P_COLOR_COMMA,   new RGB(64, 64, 64));
    }
}
