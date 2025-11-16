/* === JSONOutlinePage.java === */
package de.drremote.jsonplugin.editor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import de.drremote.jsonplugin.Activator;
import de.drremote.jsonplugin.editor.model.JsonParserUtil;
import de.drremote.jsonplugin.editor.model.JsonTreeNode;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;

public class JSONOutlinePage extends ContentOutlinePage {

    private final JSONEditor editor;

    private IDocument document;

    // global copy action for Ctrl+C
    private Action copyAction;

    // Toolbar actions
    private Action expandAllAction;
    private Action collapseAllAction;
    private Action helpAction;

    private Action removeCommentsAction;
    private Action toggleCommentsAction;

    public JSONOutlinePage(JSONEditor editor) {
        this.editor = editor;
    }

    @Override
    public void createControl(org.eclipse.swt.widgets.Composite parent) {
        // Standard-Outline-Control + TreeViewer vom ContentOutlinePage erzeugen lassen
        super.createControl(parent);

        TreeViewer viewer = getTreeViewer();
        viewer.setContentProvider(new JSONOutlineContentProvider());
        viewer.setLabelProvider(new JSONOutlineLabelProvider());
        viewer.setInput(document);

        // Double-click -> select in editor
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                JsonTreeNode node = getSelectedNode();
                if (node != null) {
                    revealNodeInEditor(node);
                }
            }
        });

        // Kontextmenü
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> fillContextMenu(manager));
        viewer.getControl().setMenu(menuMgr.createContextMenu(viewer.getControl()));

        // globale COPY-Action (Ctrl+C)
        copyAction = new Action() {
            @Override
            public void run() {
                doCopy();
            }
        };
        IActionBars bars = getSite().getActionBars();
        bars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
        bars.updateActionBars();

        // Toolbar-Buttons
        createToolbarActions();
        contributeToActionBars();

        // zusätzliche Hotkeys: Ctrl+Shift+C, Enter
        viewer.getControl().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

                // CTRL + SHIFT + C -> copy JSON
                if ((e.stateMask & SWT.CTRL) != 0 && (e.stateMask & SWT.SHIFT) != 0 && e.keyCode == 'c') {
                    doCopyJson();
                }

                // ENTER -> replace value
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    doReplace();
                }
            }
        });
    }

    // Create toolbar buttons – only icon, no text
    private void createToolbarActions() {
        // Expand all
        expandAllAction = new Action() {
            @Override
            public void run() {
                if (getTreeViewer() != null) {
                    getTreeViewer().expandAll();
                }
            }
        };
        expandAllAction.setImageDescriptor(Activator.getImageDescriptor("icons/expandall.png"));
        expandAllAction.setToolTipText("Expand all");

        // Collapse all
        collapseAllAction = new Action() {
            @Override
            public void run() {
                if (getTreeViewer() != null) {
                    getTreeViewer().collapseAll();
                }
            }
        };
        collapseAllAction.setImageDescriptor(Activator.getImageDescriptor("icons/collapseall.png"));
        collapseAllAction.setToolTipText("Collapse all");

        // Remove comments from document
        removeCommentsAction = new Action() {
            @Override
            public void run() {
                removeAllCommentsFromDocument();
            }
        };
        removeCommentsAction.setImageDescriptor(Activator.getImageDescriptor("icons/remove_comments.png"));
        removeCommentsAction.setToolTipText("Remove all // and /* */ comments from this document");

        // Toggle comment support (AS_CHECK_BOX)
        toggleCommentsAction = new Action("Allow comments", Action.AS_CHECK_BOX) {
            @Override
            public void run() {
                boolean enabled = isChecked();
                JSONCommentUtil.setCommentsEnabled(enabled);

                // rebuild outline with new setting
                refresh();
            }
        };
        toggleCommentsAction.setChecked(JSONCommentUtil.isCommentsEnabled());
        toggleCommentsAction.setImageDescriptor(Activator.getImageDescriptor("icons/comments.png"));
        toggleCommentsAction.setToolTipText("Allow // and /* */ comments (ignored for parsing/validation)");

        // Help / shortcuts
        helpAction = new Action() {
            @Override
            public void run() {
                org.eclipse.jface.dialogs.MessageDialog.openInformation(getSite().getShell(),
                        "JSON Outline – Keyboard shortcuts", """
                                Keyboard shortcuts (when the JSON Outline has focus):

                                Enter          – Replace value
                                Ctrl+C        – Copy value
                                Ctrl+Shift+C  – Copy JSON subtree
                                """);
            }
        };
        helpAction.setImageDescriptor(Activator.getImageDescriptor("icons/help.png"));
        helpAction.setToolTipText("Show keyboard shortcuts");
    }

    // Add buttons to the toolbar
    private void contributeToActionBars() {
        IActionBars bars = getSite().getActionBars();
        IToolBarManager mgr = bars.getToolBarManager();
        if (mgr != null) {
            mgr.add(expandAllAction);
            mgr.add(collapseAllAction);
            mgr.add(removeCommentsAction);
            mgr.add(toggleCommentsAction);
            mgr.add(helpAction);
        }
    }

    public void setInput(IDocument document) {
        this.document = document;
        if (getTreeViewer() != null) {
            getTreeViewer().setInput(document);
        }
    }

    public void refresh() {
        if (getTreeViewer() != null) {
            getTreeViewer().refresh();
            getTreeViewer().expandToLevel(2);
        }
    }

    private void removeAllCommentsFromDocument() {
        IDocument doc = editor.getDocument();
        if (doc == null) {
            return;
        }

        String text = doc.get();
        if (text == null || text.isEmpty()) {
            return;
        }

        String withoutComments = JSONCommentUtil.stripComments(text);

        if (!withoutComments.equals(text)) {
            try {
                doc.set(withoutComments);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // rebuild outline
            refresh();
        }
    }

    public void updateSelectionFromEditor(ISelection selection) {
        // optional später für Editor -> Outline Sync
    }

    /**
     * Fill context menu (based on current selection).
     */
    private void fillContextMenu(IMenuManager manager) {
        IStructuredSelection sel = getStructuredSelection();
        if (sel == null || sel.isEmpty()) {
            return;
        }

        Object element = sel.getFirstElement();
        if (!(element instanceof JsonTreeNode node)) {
            return;
        }

        String value = node.getValue();

        // Copy value
        if (value != null) {
            manager.add(new Action("Copy value") {
                @Override
                public void run() {
                    String decoded = JsonParserUtil.decodeJsonStringLiteral(value);
                    copyToClipboard(decoded);
                }
            });

            manager.add(new Action("Replace value...") {
                @Override
                public void run() {
                    replaceValueInDocument(node);
                }
            });
        }

        // Copy path (JSONPath, e.g. $.invoice.items[0].price)
        manager.add(new Action("Copy path") {
            @Override
            public void run() {
                doCopyPath();
            }
        });

        // Copy JSON (whole subtree – exakt wie im Editor)
        manager.add(new Action("Copy JSON") {
            @Override
            public void run() {
                String text = getDocumentJsonForNode(node);
                if (text != null && !text.isEmpty()) {
                    copyToClipboard(text);
                }
            }
        });

        // Copy tree (selection or whole visible tree)
        manager.add(new Action("Copy tree") {
            @Override
            public void run() {
                doCopyTree();
            }
        });

        // Copy schema (selection or whole document)
        manager.add(new Action("Copy schema") {
            @Override
            public void run() {
                doCopySchema();
            }
        });
    }

    private IStructuredSelection getStructuredSelection() {
        if (getTreeViewer() == null) {
            return null;
        }
        ISelection selection = getTreeViewer().getSelection();
        if (selection instanceof IStructuredSelection structuredSelection) {
            return structuredSelection;
        }
        return null;
    }

    private JsonTreeNode getSelectedNode() {
        IStructuredSelection sel = getStructuredSelection();
        if (sel != null && !sel.isEmpty()) {
            Object o = sel.getFirstElement();
            if (o instanceof JsonTreeNode node) {
                return node;
            }
        }
        return null;
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        TextTransfer textTransfer = TextTransfer.getInstance();
        clipboard.setContents(new Object[] { text }, new Transfer[] { textTransfer });
        clipboard.dispose();
    }

    private void doCopy() {
        JsonTreeNode node = getSelectedNode();
        if (node == null || node.getValue() == null) {
            return;
        }
        String decoded = de.drremote.jsonplugin.editor.model.JsonParserUtil
                .decodeJsonStringLiteral(node.getValue());
        copyToClipboard(decoded);
    }

    private void doCopyJson() {
        JsonTreeNode node = getSelectedNode();
        if (node == null) {
            return;
        }

        String text = getDocumentJsonForNode(node);
        if (text != null && !text.isEmpty()) {
            copyToClipboard(text);
        }
    }

    // Copy JSONPath for the selected node, e.g. $.invoice.items[0].price
    private void doCopyPath() {
        JsonTreeNode node = getSelectedNode();
        if (node == null) {
            return;
        }

        String path;
        if (node.getParent() == null) {
            path = "$";
        } else {
            String relative = buildPath(node);
            if (relative == null || relative.isEmpty()) {
                path = "$";
            } else {
                path = "$." + relative;
            }
        }

        copyToClipboard(path);
    }

    // Copy tree depending on selection / expansion
    private void doCopyTree() {
        if (getTreeViewer() == null) {
            return;
        }

        TreeViewer viewer = getTreeViewer();

        java.util.List<JsonTreeNode> roots = new java.util.ArrayList<>();

        // a) wenn etwas selektiert ist → genau diese Nodes
        IStructuredSelection sel = getStructuredSelection();
        if (sel != null && !sel.isEmpty()) {
            for (Object o : sel.toArray()) {
                if (o instanceof JsonTreeNode node) {
                    roots.add(node);
                }
            }
        } else {
            // b) wenn nichts selektiert ist → alle sichtbaren Top-Level-Items
            for (TreeItem item : viewer.getTree().getItems()) {
                Object data = item.getData();
                if (data instanceof JsonTreeNode node) {
                    roots.add(node);
                }
            }
        }

        if (roots.isEmpty()) {
            return;
        }

        java.util.List<String> lines = new java.util.ArrayList<>();

        for (JsonTreeNode rootNode : roots) {
            String basePath = buildPath(rootNode);

            if (rootNode.getParent() == null || "root".equals(rootNode.getName())) {
                basePath = "";
            }

            collectTreeLinesVisible(rootNode, basePath, viewer, lines, true);
        }

        if (!lines.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                sb.append(line).append(System.lineSeparator());
            }
            copyToClipboard(sb.toString());
        }
    }

    // Copy JSON schema (paths only, optional: from selection)
    private void doCopySchema() {
        if (getTreeViewer() == null) {
            return;
        }

        java.util.List<String> lines = new java.util.ArrayList<>();

        IStructuredSelection sel = getStructuredSelection();
        if (sel != null && !sel.isEmpty()) {
            for (Object o : sel.toArray()) {
                if (o instanceof JsonTreeNode node) {
                    collectSchemaLines(node, "", lines);
                }
            }
        } else {
            for (TreeItem item : getTreeViewer().getTree().getItems()) {
                Object data = item.getData();
                if (data instanceof JsonTreeNode node) {
                    collectSchemaLines(node, "", lines);
                }
            }
        }

        if (lines.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append(System.lineSeparator());
        }
        copyToClipboard(sb.toString());
    }

    // Build a qualified path for a node using its parents
    private String buildPath(JsonTreeNode node) {
        StringBuilder sb = new StringBuilder();
        JsonTreeNode current = node;
        while (current != null && current.getParent() != null) {
            String name = current.getName();
            if (name != null && !"root".equals(name) && !"empty".equals(name)) {
                if (sb.length() == 0) {
                    sb.insert(0, name);
                } else {
                    if (name.startsWith("[")) {
                        sb.insert(0, name);
                    } else {
                        sb.insert(0, "." + name);
                    }
                }
            }
            current = current.getParent();
        }
        return sb.toString();
    }

    /**
     * Sammelt nur das, was im TreeViewer aufgeklappt ist.
     */
    private void collectTreeLinesVisible(JsonTreeNode node, String path, TreeViewer viewer,
            java.util.List<String> lines, boolean forceExpandedForThisNode) {

        String displayPath = (path == null || path.isEmpty())
                ? (node.getName() != null ? node.getName() : "(root)")
                : path;

        lines.add(displayPath);

        boolean expanded = forceExpandedForThisNode || viewer.getExpandedState(node);
        if (!expanded) {
            return;
        }

        for (JsonTreeNode child : node.getChildren()) {
            String childName = child.getName();
            String childPath;

            if (childName == null) {
                childPath = displayPath;
            } else if (childName.startsWith("[")) {
                childPath = displayPath + childName;
            } else if (displayPath.isEmpty()) {
                childPath = childName;
            } else {
                childPath = displayPath + "." + childName;
            }

            collectTreeLinesVisible(child, childPath, viewer, lines, false);
        }
    }

    // recursively collect schema paths (no values)
    private void collectSchemaLines(JsonTreeNode node, String parentPath, java.util.List<String> lines) {
        String name = node.getName();

        if (name == null || "root".equals(name) || "empty".equals(name)) {
            for (JsonTreeNode child : node.getChildren()) {
                collectSchemaLines(child, parentPath, lines);
            }
            return;
        }

        String segment;
        boolean isArrayElement = name.startsWith("[") && name.endsWith("]");

        if (isArrayElement) {
            segment = "[]";
        } else {
            segment = name;
        }

        String currentPath;
        if (parentPath == null || parentPath.isEmpty()) {
            currentPath = segment;
        } else if (isArrayElement) {
            currentPath = parentPath + "[]";
        } else {
            currentPath = parentPath + "." + segment;
        }

        if (!lines.contains(currentPath)) {
            lines.add(currentPath);
        }

        for (JsonTreeNode child : node.getChildren()) {
            collectSchemaLines(child, currentPath, lines);
        }
    }

    private void doReplace() {
        JsonTreeNode node = getSelectedNode();
        if (node == null || node.getValue() == null) {
            return;
        }
        replaceValueInDocument(node);
    }

    /**
     * Very simple replacement: searches in the document for "name": <oldValue> and
     * replaces only the value.
     */
    private void replaceValueInDocument(JsonTreeNode node) {
        IDocument doc = editor.getDocument();
        if (doc == null) {
            return;
        }

        String oldValue = node.getValue();
        if (oldValue == null) {
            return;
        }

        InputDialog dlg = new InputDialog(getSite().getShell(), "Replace value",
                "New JSON value (exactly as it should appear in JSON, e.g. \"Text\", 123, true):",
                oldValue, null);

        if (dlg.open() != Window.OK) {
            return;
        }

        String newValue = dlg.getValue();
        if (newValue == null || newValue.isEmpty()) {
            return;
        }

        String text = doc.get();

        String key = node.getName();
        String patternString = "\"" + Pattern.quote(key) + "\"\\s*:\\s*" + Pattern.quote(oldValue);
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            int start = matcher.start();
            String match = matcher.group();

            String newMatch = match.replace(oldValue, newValue);
            try {
                doc.replace(start, match.length(), newMatch);
                refresh();
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private int findJsonValueEnd(String text, int pos) {
        int len = text.length();
        if (pos >= len) {
            return -1;
        }

        char c = text.charAt(pos);

        if (c == '"') {
            boolean escaped = false;
            int i = pos + 1;
            while (i < len) {
                char ch = text.charAt(i);
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    i++;
                    break;
                }
                i++;
            }
            while (i < len && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            if (i < len && text.charAt(i) == ',') {
                i++;
            }
            return i;
        }

        if (c == '{' || c == '[') {
            char open = c;
            char close = (c == '{') ? '}' : ']';
            int depth = 1;
            boolean inString = false;
            boolean escaped = false;
            int i = pos + 1;

            while (i < len && depth > 0) {
                char ch = text.charAt(i);

                if (inString) {
                    if (escaped) {
                        escaped = false;
                    } else if (ch == '\\') {
                        escaped = true;
                    } else if (ch == '"') {
                        inString = false;
                    }
                } else {
                    if (ch == '"') {
                        inString = true;
                    } else if (ch == open) {
                        depth++;
                    } else if (ch == close) {
                        depth--;
                    }
                }
                i++;
            }

            while (i < len && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            if (i < len && text.charAt(i) == ',') {
                i++;
            }

            return i;
        }

        int i = pos;
        while (i < len) {
            char ch = text.charAt(i);
            if (ch == ',' || ch == '}' || ch == ']') {
                break;
            }
            i++;
        }

        int end = i;
        while (end > pos && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (i < len && text.charAt(i) == ',') {
            i++;
            return i;
        }

        return end;
    }

    private String buildLenientJsonRegex(String raw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (Character.isWhitespace(ch)) {
                sb.append("\\s*");
            } else if ("[]{}():,.".indexOf(ch) >= 0) {
                sb.append("\\s*").append("\\").append(ch).append("\\s*");
            } else if ("\\.^$|?*+(){}".indexOf(ch) >= 0) {
                sb.append("\\").append(ch);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private void revealNodeInEditor(JsonTreeNode node) {
        IDocument doc = editor.getDocument();
        if (doc == null) {
            return;
        }

        String text = doc.get();
        int docLen = text.length();

        try {
            if (node.getParent() == null) {
                editor.selectAndReveal(0, docLen);
                return;
            }

            String key = node.getName();
            String raw = node.getRawJson();

            int offset = -1;
            int length = 0;

            if (key != null && !key.startsWith("[") && !"root".equals(key) && !"empty".equals(key)) {

                if (raw != null && !raw.isEmpty()) {
                    String regex = buildPropertyRegex(key, raw);
                    Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
                    if (m.find()) {
                        offset = m.start();
                        length = m.end() - m.start();
                    }
                }

                if (offset < 0) {
                    String keyPattern = "\\\"" + Pattern.quote(key) + "\\\"";
                    Matcher m = Pattern.compile(keyPattern).matcher(text);
                    if (m.find()) {
                        int keyStart = m.start();
                        int pos = m.end();

                        while (pos < docLen && Character.isWhitespace(text.charAt(pos))) {
                            pos++;
                        }

                        if (pos < docLen && text.charAt(pos) == ':') {
                            pos++;
                            while (pos < docLen && Character.isWhitespace(text.charAt(pos))) {
                                pos++;
                            }

                            int valueStart = pos;
                            int valueEnd = findJsonValueEnd(text, valueStart);
                            if (valueEnd < 0) {
                                valueEnd = docLen;
                            }

                            offset = keyStart;
                            length = valueEnd - keyStart;
                        } else {
                            offset = keyStart;
                            length = m.end() - keyStart;
                        }
                    }
                }

            } else {
                if (raw != null && !raw.isEmpty()) {
                    String regex = buildLenientJsonRegex(raw);
                    Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
                    if (m.find()) {
                        offset = m.start();
                        length = m.end() - m.start();
                    }
                }
            }

            if (offset >= 0) {
                if (length <= 0) {
                    length = 1;
                }
                editor.selectAndReveal(offset, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildPropertyRegex(String key, String rawJson) {
        String keyPart = "\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*";
        String valuePart = buildLenientJsonRegex(rawJson);
        return keyPart + valuePart;
    }

    private String getDocumentJsonForNode(JsonTreeNode node) {
        IDocument doc = editor.getDocument();
        if (doc == null || node == null) {
            return null;
        }

        String text = doc.get();
        int docLen = text.length();

        try {
            if (node.getParent() == null) {
                return text;
            }

            String key = node.getName();
            String raw = node.getRawJson();

            int offset = -1;
            int length = 0;

            if (key != null && !key.startsWith("[") && !"root".equals(key) && !"empty".equals(key)) {

                if (raw != null && !raw.isEmpty()) {
                    String regex = buildPropertyRegex(key, raw);
                    Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
                    if (m.find()) {
                        offset = m.start();
                        length = m.end() - m.start();
                    }
                }

                if (offset < 0) {
                    String keyPattern = "\\\"" + Pattern.quote(key) + "\\\"";
                    Matcher m = Pattern.compile(keyPattern).matcher(text);
                    if (m.find()) {
                        int keyStart = m.start();
                        int pos = m.end();

                        while (pos < docLen && Character.isWhitespace(text.charAt(pos))) {
                            pos++;
                        }

                        if (pos < docLen && text.charAt(pos) == ':') {
                            pos++;
                            while (pos < docLen && Character.isWhitespace(text.charAt(pos))) {
                                pos++;
                            }

                            int valueStart = pos;
                            int valueEnd = findJsonValueEnd(text, valueStart);
                            if (valueEnd < 0) {
                                valueEnd = docLen;
                            }

                            offset = keyStart;
                            length = valueEnd - keyStart;
                        } else {
                            offset = keyStart;
                            length = m.end() - keyStart;
                        }
                    }
                }

            } else {
                if (raw != null && !raw.isEmpty()) {
                    String regex = buildLenientJsonRegex(raw);
                    Matcher m = Pattern.compile(regex, Pattern.DOTALL).matcher(text);
                    if (m.find()) {
                        offset = m.start();
                        length = m.end() - m.start();
                    }
                }
            }

            if (offset >= 0 && length > 0) {
                int end = Math.min(offset + length, docLen);
                return text.substring(offset, end);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (node.getRawJson() != null) {
            return node.getRawJson();
        }
        return node.getValue();
    }

    // ================== ContentProvider ==================

    private static class JSONOutlineContentProvider implements ITreeContentProvider {

        private JsonTreeNode root;

        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof IDocument document) {
                root = JsonParserUtil.parse(document.get());
                return root.getChildren().toArray();
            }
            return new Object[0];
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (parentElement instanceof JsonTreeNode node) {
                return node.getChildren().toArray();
            }
            return new Object[0];
        }

        @Override
        public Object getParent(Object element) {
            if (element instanceof JsonTreeNode node) {
                return node.getParent();
            }
            return null;
        }

        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof JsonTreeNode node) {
                return !node.getChildren().isEmpty();
            }
            return false;
        }

        @Override
        public void dispose() {
            root = null;
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // nothing needed
        }
    }

    // ================== LabelProvider (with icons) ==================

    private static class JSONOutlineLabelProvider extends LabelProvider {

        private final Map<String, Image> imageCache = new HashMap<>();

        @Override
        public String getText(Object element) {
            if (element instanceof JsonTreeNode node) {
                return node.toString();
            }
            return "";
        }

        @Override
        public Image getImage(Object element) {
            if (element instanceof JsonTreeNode node) {

                if (node.getParent() == null) {
                    return getIcon("json_root");
                }

                String raw = node.getRawJson();
                if (raw != null) {
                    String trimmed = raw.trim();
                    if (trimmed.startsWith("{")) {
                        return getIcon("json_object");
                    }
                    if (trimmed.startsWith("[")) {
                        return getIcon("json_array");
                    }
                }

                if (node.getValue() != null && node.getChildren().isEmpty()) {
                    return getIcon("json_value");
                }
            }
            return super.getImage(element);
        }

        private Image getIcon(String key) {
            Image img = imageCache.get(key);
            if (img != null && !img.isDisposed()) {
                return img;
            }

            String path = "icons/" + key + ".png";
            ImageDescriptor desc = Activator.getImageDescriptor(path);
            if (desc == null) {
                return null;
            }

            img = desc.createImage();
            if (img != null) {
                imageCache.put(key, img);
                return img;
            }

            return null;
        }

        @Override
        public void dispose() {
            for (Image img : imageCache.values()) {
                if (img != null && !img.isDisposed()) {
                    img.dispose();
                }
            }
            imageCache.clear();
            super.dispose();
        }
    }
}
