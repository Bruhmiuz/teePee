package seedu.address.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

/**
 * Parses markdown text into native JavaFX nodes without external dependencies.
 */
public class MarkdownRenderer {

    private static final String NUS_ORANGE = "#ef7c00";
    private static final String NUS_BLUE = "#003d7c";
    private static final String NUS_BLUE_BORDER = "#1f5f9e";

    // Unique sentinels standing in for alert <div> boundaries after pre-processing; they never
    // appear in guide content, so the render loop can match them exactly
    private static final String CALLOUT_OPEN_WARNING = "<<<callout-open:warning>>>";
    private static final String CALLOUT_OPEN_INFO = "<<<callout-open:info>>>";
    private static final String CALLOUT_CLOSE = "<<<callout-close>>>";
    private static final String CALLOUT_INFO_ACCENT = "#58a6ff";
    private static final String CALLOUT_INFO_BACKGROUND = "#12233d";
    private static final String CALLOUT_WARNING_BACKGROUND = "#2b1d0e";

    // Matches links [text](url), <kbd>key</kbd>, **bold**, `code`, and _italics_ (not inside words)
    private static final Pattern INLINE_PATTERN =
            Pattern.compile("(\\[.*?\\]\\(.*?\\)|<kbd>.*?</kbd>|\\*\\*.*?\\*\\*|`.*?`|(?<!\\w)_[^_]+?_(?!\\w))");

    // Matches image syntax ![alt](url) so images resolve to offline placeholders instead of being skipped
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[(.*?)\\]\\((.*?)\\)");

    /**
     * Renders the given markdown text string into a JavaFX {@code VBox} container.
     */
    public static VBox render(String markdown) {
        VBox container = new VBox();
        container.setSpacing(6);
        container.setPadding(new Insets(15));
        container.setFillWidth(true);

        String[] lines = splitCalloutBoundaries(markdown.split("\\r?\\n"));
        boolean inFrontmatter = false;
        int frontmatterDashes = 0;

        List<String> currentTableLines = new ArrayList<>();
        StringBuilder currentParagraph = new StringBuilder();
        int listCounter = 0;

        // calloutCard is non-null while inside an alert callout, whose lines render into it;
        // target is the container the current line renders into
        VBox calloutCard = null;
        VBox target = container;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.equals("---")) {
                flushParagraph(target, currentParagraph);
                listCounter = 0;
                frontmatterDashes++;
                inFrontmatter = frontmatterDashes < 2;
                continue;
            }
            if (inFrontmatter || line.startsWith("{:")
                    || line.equalsIgnoreCase("Table of Contents")
                    || line.equalsIgnoreCase("* Table of Contents")) {
                continue;
            }

            // Alert callout boundaries pre-processed by splitCalloutBoundaries
            if (line.equals(CALLOUT_OPEN_WARNING) || line.equals(CALLOUT_OPEN_INFO)) {
                flushParagraph(target, currentParagraph);
                flushPendingTable(target, currentTableLines);
                boolean isWarning = line.equals(CALLOUT_OPEN_WARNING);
                calloutCard = buildCalloutCard(
                        isWarning ? NUS_ORANGE : CALLOUT_INFO_ACCENT,
                        isWarning ? CALLOUT_WARNING_BACKGROUND : CALLOUT_INFO_BACKGROUND);
                target = calloutCard;
                continue;
            }
            if (line.equals(CALLOUT_CLOSE)) {
                flushParagraph(target, currentParagraph);
                flushPendingTable(target, currentTableLines);
                if (calloutCard != null) {
                    container.getChildren().add(calloutCard);
                    calloutCard = null;
                    target = container;
                }
                continue;
            }

            // Table boundary
            boolean isTableLine = line.contains("|") && !line.startsWith("#");
            if (!isTableLine) {
                flushPendingTable(target, currentTableLines);
            }

            // Blank lines flush active paragraph without inserting blank nodes
            if (line.isEmpty()) {
                flushParagraph(target, currentParagraph);
                continue;
            }

            // Sanitize tags & resolve image references to offline placeholders
            line = sanitizeLine(line);
            if (line.isEmpty()) {
                continue;
            }

            Matcher imageMatcher = IMAGE_PATTERN.matcher(line);
            if (imageMatcher.matches()) {
                // Standalone image line: flush pending text first so the source order is kept
                flushParagraph(target, currentParagraph);
                // Show where the figure belongs and how to view it online
                target.getChildren().add(buildImagePlaceholder(imageMatcher.group(1)));
                continue;
            }
            // Inline image inside a paragraph: substitute it with bracket-free placeholder text
            // before the link pattern can match it
            line = imageMatcher.replaceAll("(image: $1)");

            // Collapse repeated callout emoji clusters (e.g. [:exclamation: :exclamation: ...])
            line = line.replaceAll("\\[?\\s*(:exclamation:(\\uFE0F)?\\s*)+\\]?\\s*(\\*\\*Caution:\\*\\*)?",
                    "⚠ Caution: ");
            line = line.replaceAll("\\[?\\s*(:information_source:(\\uFE0F)?\\s*)+\\]?\\s*"
                    + "(\\*\\*Note:\\*\\*|\\*\\*Info:\\*\\*)?", "ℹ Note: ");

            // Accumulate table rows
            if (isTableLine) {
                flushParagraph(target, currentParagraph);
                listCounter = 0;
                if (!line.matches("^[\\-\\s|:]+$")) {
                    currentTableLines.add(line);
                }
                continue;
            }

            // Headings and section rules reset numbering
            if (line.startsWith("### ")) {
                flushParagraph(target, currentParagraph);
                listCounter = 0;
                Label h3 = new Label(cleanHtmlLineBreaks(line.substring(4)).replaceAll("`", ""));
                h3.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                        + NUS_ORANGE + "; -fx-padding: 8 0 2 0;");
                h3.setWrapText(true);
                target.getChildren().add(h3);
            } else if (line.startsWith("## ")) {
                flushParagraph(target, currentParagraph);
                listCounter = 0;
                Label h2 = new Label(cleanHtmlLineBreaks(line.substring(3)).replaceAll("`", ""));
                h2.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: "
                        + NUS_ORANGE + "; -fx-padding: 12 0 4 0;");
                h2.setWrapText(true);
                target.getChildren().add(h2);
            } else if (line.startsWith("# ")) {
                flushParagraph(target, currentParagraph);
                listCounter = 0;
                Label h1 = new Label(cleanHtmlLineBreaks(line.substring(2)).replaceAll("`", ""));
                h1.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ffffff; "
                        + "-fx-padding: 14 0 6 0;");
                h1.setWrapText(true);
                target.getChildren().add(h1);
            } else if (line.matches("^-{3,}$")) {
                flushParagraph(target, currentParagraph);
                listCounter = 0;
                Separator separator = new Separator();
                separator.setStyle("-fx-padding: 6 0 6 0; -fx-opacity: 0.35;");
                target.getChildren().add(separator);
            } else if (line.startsWith(">")) {
                // Blockquote callout box
                flushParagraph(target, currentParagraph);
                String calloutText = cleanHtmlLineBreaks(line.substring(1).trim());
                TextFlow calloutFlow = buildInlineTextFlow(calloutText);
                calloutFlow.setStyle("-fx-background-color: #161b22; "
                        + "-fx-border-color: #30363d #30363d #30363d " + NUS_ORANGE + "; "
                        + "-fx-border-width: 0 0 0 3px; "
                        + "-fx-padding: 6 10 6 12; "
                        + "-fx-background-radius: 0 4 4 0;");
                target.getChildren().add(calloutFlow);
            } else if (line.startsWith("* ") || line.startsWith("- ")) {
                // Sub-bullets
                flushParagraph(target, currentParagraph);
                String text = cleanHtmlLineBreaks(line.substring(2));
                TextFlow bulletFlow = buildInlineTextFlow("•  " + text);
                int leftPadding = (listCounter > 0) ? 28 : 16;
                bulletFlow.setStyle("-fx-padding: 0 0 0 " + leftPadding + ";");
                target.getChildren().add(bulletFlow);
            } else if (line.matches("^\\d+\\.\\s+.*")) {
                // Numbered list item
                flushParagraph(target, currentParagraph);
                listCounter++;
                String itemText = cleanHtmlLineBreaks(line.replaceFirst("^\\d+\\.\\s+", ""));
                TextFlow listFlow = buildInlineTextFlow(listCounter + ".  " + itemText);
                listFlow.setStyle("-fx-padding: 0 0 0 16;");
                target.getChildren().add(listFlow);
            } else {
                // Indented notes or secondary descriptive lines
                boolean isSecondaryNote = line.startsWith("**Mac users:**")
                        || line.startsWith("LaiNurse keeps its data")
                        || line.startsWith("A GUI similar")
                        || line.startsWith("Some example commands");

                if (listCounter > 0 && isSecondaryNote) {
                    flushParagraph(target, currentParagraph);
                    TextFlow noteFlow = buildInlineTextFlow(cleanHtmlLineBreaks(line));
                    noteFlow.setStyle("-fx-padding: 0 0 0 28;");
                    target.getChildren().add(noteFlow);
                } else {
                    if (currentParagraph.length() > 0) {
                        currentParagraph.append(" ");
                    }
                    currentParagraph.append(cleanHtmlLineBreaks(line));
                }
            }
        }

        flushParagraph(target, currentParagraph);
        flushPendingTable(target, currentTableLines);

        // A callout left open at the end of the input still renders its collected lines
        if (calloutCard != null) {
            container.getChildren().add(calloutCard);
        }

        return container;
    }

    private static void flushParagraph(VBox container, StringBuilder paragraph) {
        String content = paragraph.toString().trim();
        paragraph.setLength(0);
        if (content.isEmpty()) {
            return;
        }
        TextFlow flow = buildInlineTextFlow(content);
        container.getChildren().add(flow);
    }

    /**
     * Renders and clears any table lines accumulated so far, so that a table directly before a
     * callout boundary lands inside the card and in source order.
     */
    private static void flushPendingTable(VBox target, List<String> currentTableLines) {
        if (!currentTableLines.isEmpty()) {
            target.getChildren().add(renderTable(currentTableLines));
            currentTableLines.clear();
        }
    }

    private static String sanitizeLine(String line) {
        return line.replaceAll("(?i)</?div[^>]*>", "")
                .replaceAll("(?i)</?span[^>]*>", "")
                .trim();
    }

    /**
     * Replaces alert {@code <div>} boundaries with unique sentinels so the render loop can open and
     * close callout cards exactly. Content trailing the opening tag stays as its own line. Callout
     * blocks are assumed not to be nested, which holds for the guide today.
     */
    private static String[] splitCalloutBoundaries(String[] lines) {
        List<String> processed = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("<div") && trimmed.contains("class=\"alert")) {
                processed.add(trimmed.contains("alert-warning")
                        ? CALLOUT_OPEN_WARNING : CALLOUT_OPEN_INFO);
                String remainder = trimmed.replaceFirst("^<div[^>]*>", "").trim();
                if (remainder.endsWith("</div>")) {
                    // A one-line callout closes on its own opening line
                    remainder = remainder.substring(0, remainder.length() - "</div>".length()).trim();
                    if (!remainder.isEmpty()) {
                        processed.add(remainder);
                    }
                    processed.add(CALLOUT_CLOSE);
                } else if (!remainder.isEmpty()) {
                    processed.add(remainder);
                }
            } else if (trimmed.equals("</div>")) {
                processed.add(CALLOUT_CLOSE);
            } else {
                processed.add(line);
            }
        }
        return processed.toArray(String[]::new);
    }

    /**
     * Builds the tinted card with a colored left border that visually groups the lines of one
     * alert callout.
     */
    private static VBox buildCalloutCard(String accent, String background) {
        VBox card = new VBox();
        card.setStyle("-fx-background-color: " + background + "; "
                + "-fx-border-color: " + accent + "; "
                + "-fx-border-width: 0 0 0 3px; "
                + "-fx-padding: 8 12 8 12; "
                + "-fx-background-radius: 0 4 4 0; "
                + "-fx-border-radius: 0 4 4 0; "
                + "-fx-spacing: 4;");
        return card;
    }

    /**
     * Builds a subdued placeholder describing an image that is omitted from the offline copy
     * of the guide, so the reader knows what it depicts and how to view it in the online
     * User Guide.
     */
    private static TextFlow buildImagePlaceholder(String altText) {
        String description = (altText == null || altText.isBlank()) ? "screenshot" : altText;
        TextFlow placeholder = new TextFlow();
        Text icon = new Text("🖼 ");
        icon.setStyle("-fx-fill: " + NUS_ORANGE + "; -fx-font-size: 13px;");
        Text message = new Text("Image omitted from the offline guide: '" + description + "'. "
                + "Use the Copy URL button above to view it in the online User Guide.");
        message.setStyle("-fx-fill: #8b949e; -fx-font-size: 12px; -fx-font-style: italic;");
        placeholder.getChildren().addAll(icon, message);
        placeholder.setStyle("-fx-padding: 4 0 4 16;");
        return placeholder;
    }

    private static GridPane renderTable(List<String> tableLines) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: " + NUS_BLUE + "; -fx-border-color: "
                + NUS_BLUE_BORDER + "; -fx-background-radius: 4; -fx-border-radius: 4;");

        // The constraints must cover every column the table actually has: a table wider
        // than the two hard-coded slots previously left its last columns without sizing
        // rules, and JavaFX then sized the grid to a pathological preferred height.
        int columnCount = countColumns(tableLines);
        for (int i = 0; i < columnCount; i++) {
            ColumnConstraints column = new ColumnConstraints();
            if (i == 0 && columnCount > 1) {
                // The first column keeps the original 28% so existing two-column
                // tables preserve their previous geometry exactly.
                column.setPercentWidth(28);
            } else if (columnCount > 1) {
                column.setPercentWidth((100 - 28.0) / (columnCount - 1));
            } else {
                column.setPercentWidth(100);
            }
            if (i == columnCount - 1) {
                column.setHgrow(Priority.ALWAYS);
            }
            grid.getColumnConstraints().add(column);
        }

        int rowIndex = 0;
        for (String rawRow : tableLines) {
            String[] cells = splitRow(rawRow);
            for (int colIndex = 0; colIndex < cells.length; colIndex++) {
                String cellContent = cleanHtmlLineBreaks(cells[colIndex].trim());
                TextFlow cellFlow = buildInlineTextFlow(cellContent);
                cellFlow.setPadding(new Insets(2, 4, 2, 4));
                grid.add(cellFlow, colIndex, rowIndex);
            }
            rowIndex++;
        }

        return grid;
    }

    /**
     * Returns the widest row's cell count. A markdown table may write rows with ragged
     * cell counts, so the column constraints must cover every column that appears.
     */
    private static int countColumns(List<String> tableLines) {
        int maxCells = 0;
        for (String rawRow : tableLines) {
            maxCells = Math.max(maxCells, splitRow(rawRow).length);
        }
        return maxCells;
    }

    /**
     * Splits a table row into cells on unescaped pipes. A pipe is escaped when the run of backslashes
     * directly before it has odd length, and then stays inside its cell together with the backslash.
     * Leading and trailing delimiter pipes do not become empty cells.
     */
    private static String[] splitRow(String rawRow) {
        String row = rawRow.trim();
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        int backslashRun = 0;
        boolean leadingDelimiter = row.startsWith("|");
        boolean trailingDelimiter = false;
        for (int i = leadingDelimiter ? 1 : 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '\\') {
                backslashRun++;
                cell.append(c);
            } else if (c == '|' && backslashRun % 2 == 0) {
                cells.add(cell.toString());
                cell.setLength(0);
                trailingDelimiter = true;
            } else {
                cell.append(c);
                backslashRun = 0;
                trailingDelimiter = false;
            }
        }
        cells.add(cell.toString());
        if (trailingDelimiter && cells.size() > 1) {
            cells.remove(cells.size() - 1);
        }
        return cells.stream()
                .map(content -> content.replace("\\|", "|"))
                .toArray(String[]::new);
    }

    private static String cleanHtmlLineBreaks(String text) {
        // 1. Remove trailing <br> tags at the end of lines so they don't produce dangling empty newlines
        String cleaned = text.replaceAll("(?i)(<br\\s*/?>\\s*)+$", "");
        // 2. Replace any internal <br> tags with a single newline
        return cleaned.replaceAll("(?i)(<br\\s*/?>\\s*)+", "\n");
    }

    private static TextFlow buildInlineTextFlow(String text) {
        TextFlow flow = new TextFlow();
        Matcher matcher = INLINE_PATTERN.matcher(text);
        int lastIdx = 0;

        while (matcher.find()) {
            if (matcher.start() > lastIdx) {
                appendFormattedText(flow, text.substring(lastIdx, matcher.start()));
            }

            String match = matcher.group();
            if (match.startsWith("[") && match.contains("](") && match.endsWith(")")) {
                int splitIndex = match.indexOf("](");
                String label = match.substring(1, splitIndex).replace("`", "");
                String target = match.substring(splitIndex + 2, match.length() - 1);

                if (target.startsWith("http://") || target.startsWith("https://")) {
                    Hyperlink link = new Hyperlink(label);
                    String defaultStyle = "-fx-text-fill: #58a6ff; -fx-padding: 0; "
                            + "-fx-border-color: transparent; -fx-font-size: 13px; -fx-underline: true;";
                    String copiedStyle = "-fx-text-fill: " + NUS_ORANGE + "; -fx-padding: 0; "
                            + "-fx-border-color: transparent; -fx-font-size: 13px; -fx-underline: false;";

                    link.setStyle(defaultStyle);

                    link.setOnAction(e -> {
                        Clipboard clipboard = Clipboard.getSystemClipboard();
                        ClipboardContent content = new ClipboardContent();
                        content.putString(target);
                        clipboard.setContent(content);

                        link.setText(label + " (Copied!)");
                        link.setStyle(copiedStyle);

                        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                        pause.setOnFinished(ev -> {
                            link.setText(label);
                            link.setStyle(defaultStyle);
                        });
                        pause.play();
                    });

                    flow.getChildren().add(link);
                } else {
                    Text internalLink = new Text(label);
                    internalLink.setStyle("-fx-fill: #79c0ff; -fx-font-weight: bold; -fx-font-size: 13px;");
                    flow.getChildren().add(internalLink);
                }
            } else if (match.startsWith("<kbd>") && match.endsWith("</kbd>")) {
                Text kbd = new Text(" [" + match.substring(5, match.length() - 6) + "] ");
                kbd.setStyle("-fx-fill: #79c0ff; -fx-font-family: monospace; -fx-font-size: 12px;");
                flow.getChildren().add(kbd);
            } else if (match.startsWith("**") && match.endsWith("**")) {
                String inner = match.substring(2, match.length() - 2);
                String badgePrefix = inner.startsWith("⚠ Caution: ") ? "⚠ Caution: "
                        : inner.startsWith("ℹ Note: ") ? "ℹ Note: " : null;
                if (badgePrefix == null) {
                    Text bold = new Text(inner);
                    bold.setStyle("-fx-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");
                    flow.getChildren().add(bold);
                } else {
                    // The collapsed callout kept its bold wrapper, so the badge prefix lands inside
                    // a bold match: style the prefix as the badge, and the rest of the text as bold.
                    Text badge = new Text(badgePrefix);
                    badge.setStyle("-fx-fill: " + (badgePrefix.startsWith("⚠") ? NUS_ORANGE : CALLOUT_INFO_ACCENT)
                            + "; -fx-font-weight: bold; -fx-font-size: 13px;");
                    flow.getChildren().add(badge);
                    String rest = inner.substring(badgePrefix.length());
                    if (!rest.isEmpty()) {
                        Text bold = new Text(rest);
                        bold.setStyle("-fx-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");
                        flow.getChildren().add(bold);
                    }
                }
            } else if (match.startsWith("`") && match.endsWith("`")) {
                Text code = new Text(" " + match.substring(1, match.length() - 1) + " ");
                code.setStyle("-fx-fill: " + NUS_ORANGE + "; -fx-font-family: monospace; "
                        + "-fx-font-size: 12px; -fx-font-weight: bold;");
                flow.getChildren().add(code);
            } else if (match.startsWith("_") && match.endsWith("_")) {
                Text italic = new Text(match.substring(1, match.length() - 1));
                italic.setStyle("-fx-fill: #c9d1d9; -fx-font-size: 13px; -fx-font-style: italic;");
                flow.getChildren().add(italic);
            }

            lastIdx = matcher.end();
        }

        if (lastIdx < text.length()) {
            appendFormattedText(flow, text.substring(lastIdx));
        }

        return flow;
    }

    private static void appendFormattedText(TextFlow flow, String text) {
        if (text.startsWith("⚠ Caution: ")) {
            Text cautionBadge = new Text("⚠ Caution: ");
            cautionBadge.setStyle("-fx-fill: " + NUS_ORANGE + "; -fx-font-weight: bold; -fx-font-size: 13px;");
            flow.getChildren().add(cautionBadge);

            String remainder = text.substring("⚠ Caution: ".length());
            if (!remainder.isEmpty()) {
                Text plain = new Text(remainder);
                plain.setStyle("-fx-fill: #c9d1d9; -fx-font-size: 13px;");
                flow.getChildren().add(plain);
            }
        } else if (text.startsWith("ℹ Note: ")) {
            Text infoBadge = new Text("ℹ Note: ");
            infoBadge.setStyle("-fx-fill: #58a6ff; -fx-font-weight: bold; -fx-font-size: 13px;");
            flow.getChildren().add(infoBadge);

            String remainder = text.substring("ℹ Note: ".length());
            if (!remainder.isEmpty()) {
                Text plain = new Text(remainder);
                plain.setStyle("-fx-fill: #c9d1d9; -fx-font-size: 13px;");
                flow.getChildren().add(plain);
            }
        } else {
            Text plain = new Text(text);
            plain.setStyle("-fx-fill: #c9d1d9; -fx-font-size: 13px;");
            flow.getChildren().add(plain);
        }
    }
}
