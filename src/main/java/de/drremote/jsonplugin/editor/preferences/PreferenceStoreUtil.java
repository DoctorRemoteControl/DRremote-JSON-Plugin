/* === PreferenceStoreUtil.java === */
package de.drremote.jsonplugin.editor.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import de.drremote.jsonplugin.Activator;

public final class PreferenceStoreUtil {

    private static IPreferenceStore store;

    private PreferenceStoreUtil() {
    }

    public static IPreferenceStore getStore() {
        if (store == null) {
            store = new ScopedPreferenceStore(InstanceScope.INSTANCE, Activator.PLUGIN_ID);
        }
        return store;
    }
}
