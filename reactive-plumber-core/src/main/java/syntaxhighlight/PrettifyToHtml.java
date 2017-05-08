/*******************************************************************************
 * Copyright 2013 Jeremie Bresson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package syntaxhighlight;

import prettify.PrettifyParser;
import prettify.theme.ThemeDefault;

import java.awt.*;
import java.util.List;
import java.util.Map.Entry;

public class PrettifyToHtml {
    private static final String MAIN_CLASS = "prettyprint";

    public static String parseAndConvert(String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("<style>\n");
        sb.append(PrettifyToHtml.toCss(new ThemeDefault()));
        sb.append("</style>\n");
        sb.append("<div class=\"source-block\">\n");
        PrettifyParser parser = new PrettifyParser();
        List<ParseResult> parseResults = parser.parse("scala", content);
        sb.append(PrettifyToHtml.toHtml(content, parseResults));
        sb.append("</div>\n");
        return sb.toString();
    }

    /**
     * Format the {@link ParseResult} into an HTML file.
     * @param content
     * @param parseResults
     * @return
     */
    public static String toHtml(String content, List<ParseResult> parseResults) {
        int endIndex = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"" + MAIN_CLASS + "\">");
        sb.append("<pre><div class=\"line\">");
        for (ParseResult pr : parseResults) {
            int startIndex = pr.getOffset();
            if(startIndex > endIndex) {
                sb.append(content.substring(endIndex, startIndex));
            }
            endIndex = startIndex + pr.getLength();
            sb.append("<span class=\""+pr.getStyleKeysString()+"\">");
            sb.append(content.substring(startIndex, endIndex));
            sb.append("</span>");
        }
        if(content.length() > endIndex) {
            sb.append(content.substring(endIndex, content.length()));
        }
        sb.append("</pre>");
        sb.append("</div>");
        return sb.toString()
                .replace("\n", "</div><div class=\"line\">")
                .replace("<div class=\"line\"></div>", "\n");
    }

    public static String toCss(Theme theme) {
        StringBuilder sb = new StringBuilder();
        sb.append("." + MAIN_CLASS + "  {");
        sb.append("font-family:monospace; ");
        appendCssColor(sb, "background-color", theme.getBackground());
        sb.append("}\n");
        for (Entry<String, Style> entry : theme.getStyles().entrySet()) {
            sb.append("." + MAIN_CLASS + " ."+entry.getKey()+"  {");
            Style style = entry.getValue();
            appendCssColor(sb, "background-color", style.getBackground());
            appendCssColor(sb, "color", style.getColor());
            appendCssText(sb, "font-weight: bold", style.isBold());
            appendCssText(sb, "font-style: italic", style.isItalic());
            appendCssText(sb, "text-decoration: underline", style.isUnderline());
            sb.append("}\n");
        }
        theme.getStyles();
        return sb.toString();
    }

    private static void appendCssColor(StringBuilder sb, String cssKey, Color color) {
        if(color != null) {
            sb.append(cssKey +": "+ encode(color) + "; ");
        }
    }

    private final static String encode(Color color) {
        String s = Integer.toHexString(color.getRGB() & 0xffffff);
        if (s.length() < 6) {
            s = "000000".substring(0, 6 - s.length()) + s;
        }
        return '#' + s;
    }

    private static void appendCssText(StringBuilder sb, String cssProp, boolean isSet) {
        if(isSet) {
            sb.append(cssProp + "; ");
        }
    }
}