/* === JSONPreferencePage.java === */
package de.drremote.jsonplugin.editor.preferences;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class JSONPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public JSONPreferencePage() {
        super(GRID);
        setPreferenceStore(PreferenceStoreUtil.getStore());
        setDescription("""
                Colors for the DRremote JSON Editor.

                Keyboard shortcuts in the JSON Outline (when the outline has focus):
                - Enter         : Replace value
                - Ctrl+C        : Copy value
                - Ctrl+Shift+C  : Copy JSON subtree
                """);
    }

    @Override
    public void init(IWorkbench workbench) {
        // nothing to do
    }

    @Override
    protected void createFieldEditors() {

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_DEFAULT,
                "Default text (everything else)",
                getFieldEditorParent()));

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_KEY,
                "Key strings (\"key\" before :)",
                getFieldEditorParent()));

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_STRING,
                "String values",
                getFieldEditorParent()));

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_NUMBER,
                "Numbers",
                getFieldEditorParent()));

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_BOOLEAN,
                "Booleans (true/false)",
                getFieldEditorParent()));

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_NULL,
                "null",
                getFieldEditorParent()));

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_BRACE,
                "Object braces { }",
                getFieldEditorParent()));

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_BRACKET,
                "Array brackets [ ]",
                getFieldEditorParent()));

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_COLON,
                "Colon :",
                getFieldEditorParent()));

        addField(new ColorFieldEditor(
                PreferenceConstants.P_COLOR_COMMA,
                "Comma ,",
                getFieldEditorParent()));
    }
}
