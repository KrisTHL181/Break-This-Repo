/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.web.minecraft;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Turns the fetched EULA page into the agreement text the reader should see.
///
/// The page is a marketing shell around the document: a site header, the
/// "playing in China" banner, a "continue browsing" button, cookie notices.
/// Showing the raw HTML (or the shell's text) is what made the enable wizard
/// look broken — the agreement is somewhere in the middle, and the rest is
/// navigation. So: strip markup, then start at the agreement's own title and
/// keep everything after it.
///
/// This is deliberately not a general HTML parser. It handles the page Mojang
/// serves; a redesign that moves the title would make it fall back to the
/// whole stripped page, which is still readable text.
@NotNullByDefault
public final class McHtml {

    private static final Pattern DROP_BLOCKS = Pattern.compile(
            "(?is)<(script|style|noscript|head|svg)\\b[^>]*>.*?</\\1\\s*>");
    private static final Pattern COMMENTS = Pattern.compile("(?s)<!--.*?-->");
    private static final Pattern BLOCK_ENDS = Pattern.compile(
            "(?i)<br\\s*/?>|</p\\s*>|</div\\s*>|</li\\s*>|</h[1-6]\\s*>|</tr\\s*>");
    private static final Pattern TAGS = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern TITLE = Pattern.compile(
            "(?i)end\\(er\\)-?user license agreement|end user license agreement");

    private McHtml() {
    }

    /// Extracts the readable agreement.
    ///
    /// @param html the fetched page
    /// @return the text, never `null`; empty when nothing readable is left
    public static String legalText(String html) {
        String text = COMMENTS.matcher(html).replaceAll(" ");
        text = DROP_BLOCKS.matcher(text).replaceAll("\n");
        text = BLOCK_ENDS.matcher(text).replaceAll("\n");
        text = TAGS.matcher(text).replaceAll(" ");
        text = unescape(text);

        List<String> lines = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.replaceAll("[ \\t\\u00a0]+", " ").strip();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        int start = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (TITLE.matcher(lines.get(i)).find()) {
                start = i;
                break;
            }
        }
        List<String> kept = lines.subList(start, lines.size());
        if (kept.isEmpty()) {
            return "";
        }
        // The shell occasionally repeats a line (a mobile and a desktop copy of
        // the same notice); keep the first occurrence and keep the order.
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (String line : kept) {
            seen.putIfAbsent(line, Boolean.TRUE);
        }
        return String.join("\n", seen.keySet());
    }

    private static String unescape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c != '&') {
                out.append(c);
                i++;
                continue;
            }
            int end = text.indexOf(';', i);
            if (end < 0 || end - i > 10) {
                out.append(c);
                i++;
                continue;
            }
            String entity = text.substring(i + 1, end);
            String replacement = switch (entity) {
                case "amp" -> "&";
                case "lt" -> "<";
                case "gt" -> ">";
                case "quot" -> "\"";
                case "apos", "#39" -> "'";
                case "nbsp" -> " ";
                case "ldquo", "laquo" -> "\u201c";
                case "rdquo", "raquo" -> "\u201d";
                case "lsquo" -> "\u2018";
                case "rsquo" -> "\u2019";
                case "mdash" -> "\u2014";
                case "ndash" -> "\u2013";
                case "hellip" -> "\u2026";
                case "copy" -> "\u00a9";
                case "reg" -> "\u00ae";
                case "trade" -> "\u2122";
                default -> null;
            };
            if (replacement != null) {
                out.append(replacement);
            } else if (entity.startsWith("#")) {
                try {
                    int code = entity.startsWith("#x") || entity.startsWith("#X")
                            ? Integer.parseInt(entity.substring(2), 16)
                            : Integer.parseInt(entity.substring(1));
                    out.appendCodePoint(code);
                } catch (RuntimeException e) {
                    out.append('&').append(entity).append(';');
                }
            } else {
                out.append('&').append(entity).append(';');
            }
            i = end + 1;
        }
        return out.toString();
    }
}