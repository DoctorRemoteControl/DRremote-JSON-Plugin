/* === JSONScanner.java === */
package de.drremote.jsonplugin.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.graphics.RGB;

import de.drremote.jsonplugin.Activator;
import de.drremote.jsonplugin.editor.preferences.PreferenceConstants;
import de.drremote.jsonplugin.editor.preferences.PreferenceStoreUtil;

public class JSONScanner extends RuleBasedScanner {

    /**
     * Detection of key strings: "key" <whitespace>* :
     * Uses only read() / unread().
     */
    private static class KeyStringRule implements IRule {

        private final IToken token;

        public KeyStringRule(IToken token) {
            this.token = token;
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner) {
            int c = scanner.read();
            if (c != '\"') {
                scanner.unread();
                return Token.UNDEFINED;
            }

            // We are at the beginning of a string → read the whole string
            boolean escaped = false;
            StringBuilder sb = new StringBuilder();
            sb.append('"');

            while (true) {
                c = scanner.read();
                if (c == ICharacterScanner.EOF) {
                    break;
                }
                sb.append((char) c);

                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '\"') {
                    break; // end of string
                }
            }

            // now consume whitespace
            StringBuilder ws = new StringBuilder();
            while (true) {
                c = scanner.read();
                if (c == ICharacterScanner.EOF)
                    break;
                if (!Character.isWhitespace(c)) {
                    scanner.unread();
                    break;
                }
                ws.append((char) c);
            }

            // check next character → KEY only if :
            c = scanner.read();
            if (c == ':') {
                // -> KEY detected
                return token;
            }

            // Not a key → roll everything back
            scanner.unread(); // last character

            // roll back whitespace
            for (int i = 0; i < ws.length(); i++) {
                scanner.unread();
            }

            // roll back whole string
            for (int i = 0; i < sb.length(); i++) {
                scanner.unread();
            }

            return Token.UNDEFINED;
        }
    }

    /**
     * Simple rule for single characters (or character groups).
     */
    private static class SingleCharRule implements IRule {

        private final char[] chars;
        private final IToken token;

        public SingleCharRule(char[] chars, IToken token) {
            this.chars = chars;
            this.token = token;
        }

        @Override
        public IToken evaluate(ICharacterScanner scanner) {
            int c = scanner.read();
            if (c == ICharacterScanner.EOF) {
                return Token.UNDEFINED;
            }
            for (char ch : chars) {
                if (c == ch) {
                    return token;
                }
            }
            scanner.unread();
            return Token.UNDEFINED;
        }
    }

    public JSONScanner(ColorManager colorManager) {

        // Defaults
        RGB stringRGB   = new RGB(42, 0, 255);
        RGB numberRGB   = new RGB(0, 128, 0);
        RGB boolRGB     = new RGB(127, 0, 85);
        RGB nullRGB     = new RGB(128, 128, 128);
        RGB defaultRGB  = new RGB(0, 0, 0);
        RGB keyRGB      = new RGB(0, 0, 192);
        RGB braceRGB    = new RGB(64, 64, 64);
        RGB bracketRGB  = new RGB(64, 64, 64);
        RGB colonRGB    = new RGB(64, 64, 64);
        RGB commaRGB    = new RGB(64, 64, 64);

        try {
            IPreferenceStore store = PreferenceStoreUtil.getStore();
            stringRGB   = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_STRING);
            numberRGB   = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_NUMBER);
            boolRGB     = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_BOOLEAN);
            nullRGB     = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_NULL);
            defaultRGB  = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_DEFAULT);
            keyRGB      = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_KEY);
            braceRGB    = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_BRACE);
            bracketRGB  = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_BRACKET);
            colonRGB    = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_COLON);
            commaRGB    = PreferenceConverter.getColor(store, PreferenceConstants.P_COLOR_COMMA);
        } catch (Exception e) {
            // keep defaults
        }

        IToken defaultToken  = new Token(new TextAttribute(colorManager.getColor(defaultRGB)));
        IToken stringToken   = new Token(new TextAttribute(colorManager.getColor(stringRGB)));
        IToken numberToken   = new Token(new TextAttribute(colorManager.getColor(numberRGB)));
        IToken boolToken     = new Token(new TextAttribute(colorManager.getColor(boolRGB)));
        IToken nullToken     = new Token(new TextAttribute(colorManager.getColor(nullRGB)));
        IToken keyToken      = new Token(new TextAttribute(colorManager.getColor(keyRGB)));
        IToken braceToken    = new Token(new TextAttribute(colorManager.getColor(braceRGB)));
        IToken bracketToken  = new Token(new TextAttribute(colorManager.getColor(bracketRGB)));
        IToken colonToken    = new Token(new TextAttribute(colorManager.getColor(colonRGB)));
        IToken commaToken    = new Token(new TextAttribute(colorManager.getColor(commaRGB)));

        setDefaultReturnToken(defaultToken);

        IRule[] rules = new IRule[9];

        int i = 0;

        // 1) Key strings: "key" <space>* :
        rules[i++] = new KeyStringRule(keyToken);

        // 2) Normal strings (values)
        rules[i++] = new SingleLineRule("\"", "\"", stringToken, '\\');

        // 3) Numbers
        rules[i++] = new NumberRule(numberToken);

        // 4) true / false / null
        WordRule wordRule = new WordRule(new IWordDetector() {
            @Override
            public boolean isWordStart(char c) {
                return Character.isLetter(c);
            }

            @Override
            public boolean isWordPart(char c) {
                return Character.isLetter(c);
            }
        });
        wordRule.addWord("true", boolToken);
        wordRule.addWord("false", boolToken);
        wordRule.addWord("null", nullToken);
        rules[i++] = wordRule;

        // 5) Object braces { }
        rules[i++] = new SingleCharRule(new char[] { '{', '}' }, braceToken);

        // 6) Array brackets [ ]
        rules[i++] = new SingleCharRule(new char[] { '[', ']' }, bracketToken);

        // 7) Colon :
        rules[i++] = new SingleCharRule(new char[] { ':' }, colonToken);

        // 8) Comma ,
        rules[i++] = new SingleCharRule(new char[] { ',' }, commaToken);

        // 9) Whitespace
        rules[i++] = new WhitespaceRule(c -> Character.isWhitespace(c));

        setRules(rules);
    }
}
