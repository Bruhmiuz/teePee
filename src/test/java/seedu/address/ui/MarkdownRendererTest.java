package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import seedu.address.commons.util.HelpBookUtil;

public class MarkdownRendererTest {

    /**
     * Label and Hyperlink are JavaFX controls that require the toolkit to be initialized on
     * JavaFX 25, unlike scene-graph primitives such as Text and TextFlow.
     */
    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // Toolkit was already initialized by another test class
        }
    }

    @Test
    public void render_bundledUserGuide_rendersStructuredContentWithoutRawMarkdown() throws Exception {
        VBox rendered = MarkdownRenderer.render(HelpBookUtil.getBundledUserGuide());

        assertFalse(rendered.getChildren().isEmpty());
        assertTrue(hasHeading(rendered));
        assertTrue(hasTable(rendered));
        assertFalse(hyperlinks(rendered).isEmpty());

        // Raw markdown must never leak into visible text, whatever the guide currently contains
        String visible = visibleText(rendered);
        assertFalse(visible.contains("!["));
        assertFalse(visible.contains("<br"));
        assertFalse(visible.contains("`"));
        assertFalse(visible.contains("**"));
        assertTrue(visible.contains("help window"));
    }

    @Test
    public void render_frontmatterAndTableOfContents_areSkipped() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "---",
                "layout: UG",
                "---",
                "# LaiNurse User Guide",
                "Table of Contents",
                "* Table of Contents",
                "Real content here."));

        List<String> visibleLines = List.of(visibleText(rendered).split("\n"));
        assertTrue(visibleLines.contains("LaiNurse User Guide"));
        assertTrue(visibleLines.contains("Real content here."));
        assertTrue(visibleLines.stream().noneMatch(line ->
                line.contains("layout: UG") || line.equalsIgnoreCase("Table of Contents")));
    }

    @Test
    public void render_headings_becomeStyledLabels() {
        VBox rendered = MarkdownRenderer.render("# LaiNurse\n## Features\n### Adding a patient\nBody");

        List<Label> labels = labels(rendered);
        assertEquals(3, labels.size());
        assertEquals("LaiNurse", labels.get(0).getText());
        assertEquals("Features", labels.get(1).getText());
        assertEquals("Adding a patient", labels.get(2).getText());
    }

    @Test
    public void render_plainLines_mergeIntoSingleParagraph() {
        VBox rendered = MarkdownRenderer.render("Line one.\nLine two.");

        assertEquals(1, rendered.getChildren().size());
        assertTrue(rendered.getChildren().get(0) instanceof TextFlow);
    }

    @Test
    public void render_standaloneImage_rendersPlaceholderInsteadOfRawMarkdown() {
        VBox rendered = MarkdownRenderer.render(String.join("\n\n",
                "A GUI similar to the one below should appear.",
                "![Ui](images/Ui.png)",
                "Next section."));

        String visible = visibleText(rendered);
        assertTrue(visible.contains("Image omitted from the offline guide"));
        assertTrue(visible.contains("'Ui'"));
        assertTrue(visible.contains("Copy URL"));
        assertFalse(visible.contains("!["));
        assertFalse(visible.contains("images/Ui.png"));
    }

    @Test
    public void render_standaloneImageWithoutAltText_fallsBackToScreenshot() {
        VBox rendered = MarkdownRenderer.render("![](images/Ui.png)");

        assertTrue(visibleText(rendered).contains("'screenshot'"));
    }

    @Test
    public void render_inlineImage_substitutedAsPlainText() {
        VBox rendered = MarkdownRenderer.render("See ![Ui](images/Ui.png) above for the screenshot.");

        assertEquals(0, hyperlinks(rendered).size()); // inline image must not create any hyperlink
        String visible = visibleText(rendered);
        assertTrue(visible.contains("(image: Ui)"));
        assertFalse(visible.contains("["));
        assertFalse(visible.contains("!["));
    }

    @Test
    public void render_inlineImageBeforeLink_linkKeepsItsOwnLabel() {
        VBox rendered = MarkdownRenderer.render("See ![Ui](images/Ui.png) and get it [here](https://example.com).");

        // The substituted text must not merge with the following link into one hyperlink
        List<Hyperlink> links = hyperlinks(rendered);
        assertEquals(1, links.size());
        assertEquals("here", links.get(0).getText());
        assertTrue(visibleText(rendered).contains("(image: Ui)"));
    }

    @Test
    public void render_textImmediatelyBeforeImage_keepsSourceOrder() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "The weekly view looks like this:",
                "![Weekly view](images/AppointmentView.png)"));

        assertEquals(2, rendered.getChildren().size());
        Parent first = (Parent) rendered.getChildren().get(0);
        Parent second = (Parent) rendered.getChildren().get(1);
        assertTrue(visibleText(first).contains("The weekly view looks like this:"));
        assertTrue(visibleText(second).contains("Image omitted from the offline guide"));
        assertFalse(visibleText(second).contains("The weekly view"));
    }

    @Test
    public void render_httpLink_rendersAsHyperlink() {
        VBox rendered = MarkdownRenderer.render("Download it [here](https://example.com/releases) first.");

        List<Hyperlink> links = hyperlinks(rendered);
        assertEquals(1, links.size());
        assertEquals("here", links.get(0).getText());
    }

    @Test
    public void render_internalLink_rendersAsPlainText() {
        VBox rendered = MarkdownRenderer.render("See [Ui](images/Ui.png) picture.");

        assertEquals(0, hyperlinks(rendered).size());
        assertTrue(visibleText(rendered).contains("Ui"));
    }

    @Test
    public void render_internalLinkWithCodeSpanLabel_stripsBackticks() {
        VBox rendered = MarkdownRenderer.render("Use [`add`](#adding-a-patient-add) to add a patient.");

        assertEquals(0, hyperlinks(rendered).size());
        String visible = visibleText(rendered);
        assertTrue(visible.contains("add"));
        assertFalse(visible.contains("`"));
    }

    @Test
    public void render_inlineStyles_renderWithoutRawMarkdown() {
        VBox rendered = MarkdownRenderer.render("Press <kbd>F1</kbd> or **Help** > **Help**, then `help add`.");

        String visible = visibleText(rendered);
        assertTrue(visible.contains("[F1]"));
        assertTrue(visible.contains("Help"));
        assertTrue(visible.contains("help add"));
        assertFalse(visible.contains("<kbd>"));
        assertFalse(visible.contains("**"));
        assertFalse(visible.contains("`"));
    }

    @Test
    public void render_calloutEmojiCluster_collapsesToBadge() {
        VBox rendered = MarkdownRenderer.render("[:exclamation:] **Caution:** Data may be lost.");

        String visible = visibleText(rendered);
        assertTrue(visible.contains("Caution:"));
        assertTrue(visible.contains("Data may be lost."));
        assertFalse(visible.contains(":exclamation:"));
    }

    @Test
    public void render_boldWrappedInfoCallout_rendersBlueBadge() {
        VBox rendered = MarkdownRenderer.render(
                "**:information_source: Notes about the command format:** Body text.");

        String visible = visibleText(rendered);
        assertTrue(visible.contains("ℹ Note: "));
        assertTrue(visible.contains("Notes about the command format:"));
        assertTrue(visible.contains("Body text."));
        // the badge prefix keeps its info-blue styling even inside the bold match
        assertTrue(containsTextStyle(rendered, "-fx-fill: #58a6ff"));
    }

    @Test
    public void render_boldWrappedCautionCallout_rendersOrangeBadge() {
        VBox rendered = MarkdownRenderer.render("**:exclamation: Caution:** Data may be lost.");

        String visible = visibleText(rendered);
        assertTrue(visible.contains("⚠ Caution: "));
        assertTrue(visible.contains("Data may be lost."));
        // the badge prefix keeps the caution-orange styling inside the bold match
        assertTrue(containsTextStyle(rendered, "-fx-fill: #ef7c00"));
    }

    @Test
    public void render_italics_renderWithoutUnderscores() {
        VBox rendered = MarkdownRenderer.render("Copy the file to the folder you want to use as the "
                + "_home folder_ for LaiNurse.");

        String visible = visibleText(rendered);
        assertFalse(visible.contains("_"));
        assertTrue(visible.contains("home folder"));
        assertTrue(containsTextStyle(rendered, "-fx-font-style: italic"));
    }

    @Test
    public void render_divCallout_rendersAsCard() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "Before the callout.",
                "<div markdown=\"span\" class=\"alert alert-warning\">:exclamation: **Caution:**",
                "`clear` and `clearappt` delete much at once, and cannot be undone.",
                "</div>",
                "After the callout."));

        assertEquals(3, rendered.getChildren().size());
        assertTrue(rendered.getChildren().get(1) instanceof VBox);
        String visible = visibleText(rendered);
        assertTrue(visible.contains("⚠ Caution: "));
        assertTrue(visible.contains("delete much at once"));
        assertTrue(visible.contains("After the callout."));
        assertTrue(hasDescendantStyled(rendered, "-fx-background-color: #2b1d0e"));
        assertTrue(hasDescendantStyled(rendered, "-fx-border-color: #ef7c00"));
        assertFalse(visible.contains("<div"));
        assertFalse(visible.contains("callout-open"));
    }

    @Test
    public void render_infoBlockCallout_rendersMultiLineCard() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "<div markdown=\"block\" class=\"alert alert-info\">",
                "",
                "**:information_source: Notes about the command format:**",
                "",
                "* `UPPER_CASE` is a parameter.",
                "</div>"));

        // The whole input lives inside the callout, so the card is the container's only child
        assertEquals(1, rendered.getChildren().size());
        VBox card = (VBox) rendered.getChildren().get(0);
        String cardText = visibleText(card);
        assertTrue(cardText.contains("ℹ Note: "));
        assertTrue(cardText.contains("UPPER_CASE"));
        assertTrue(hasDescendantStyled(card, "-fx-background-color: #12233d"));
        assertTrue(hasDescendantStyled(card, "-fx-border-color: #58a6ff"));
        assertFalse(visibleText(rendered).contains("<div"));
        assertFalse(visibleText(rendered).contains("callout-open"));
    }

    @Test
    public void render_unterminatedCallout_stillRendersCard() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "<div markdown=\"block\" class=\"alert alert-info\">",
                "Content without a closing div."));

        assertEquals(1, rendered.getChildren().size());
        VBox card = (VBox) rendered.getChildren().get(0);
        assertTrue(visibleText(card).contains("Content without a closing div."));
        assertTrue(hasDescendantStyled(card, "-fx-background-color: #12233d"));
    }

    @Test
    public void render_oneLineCallout_closesOnItsOpeningLine() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "<div class=\"alert alert-info\">One line tip</div>",
                "After the callout."));

        assertEquals(2, rendered.getChildren().size());
        VBox card = (VBox) rendered.getChildren().get(0);
        assertTrue(visibleText(card).contains("One line tip"));
        assertTrue(rendered.getChildren().get(1) instanceof TextFlow);
        assertTrue(visibleText((Parent) rendered.getChildren().get(1)).contains("After the callout."));
        assertFalse(visibleText(rendered).contains("</div"));
    }

    @Test
    public void render_tableBeforeCalloutClose_staysInsideTheCard() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "<div markdown=\"block\" class=\"alert alert-info\">",
                "Option | Effect",
                "-------|-------",
                "`view` | shows patients",
                "</div>",
                "After the callout."));

        assertEquals(2, rendered.getChildren().size());
        VBox card = (VBox) rendered.getChildren().get(0);
        assertEquals(1, tables(card).size());
        assertTrue(visibleText(card).contains("shows patients"));
        assertTrue(visibleText((Parent) rendered.getChildren().get(1)).contains("After the callout."));
    }

    @Test
    public void render_underscoresInsideWords_stayLiteral() {
        VBox rendered = MarkdownRenderer.render("Plain a_b and c_d text. Use the _home folder_ for data.");

        String visible = visibleText(rendered);
        assertTrue(visible.contains("a_b"));
        assertTrue(visible.contains("c_d"));
        assertTrue(visible.contains("home folder"));
        assertTrue(containsTextStyle(rendered, "-fx-font-style: italic"));
    }

    private static boolean containsTextStyle(Parent root, String styleFragment) {
        return descendants(root).stream().anyMatch(n -> n instanceof Text t
                && t.getStyle().contains(styleFragment));
    }

    private static boolean hasDescendantStyled(Parent root, String styleFragment) {
        return descendants(root).stream().anyMatch(node -> node.getStyle() != null
                && node.getStyle().contains(styleFragment));
    }

    @Test
    public void render_table_rendersAsGridPaneWithAllCells() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "Action | Format",
                "-------|-------",
                "**Add** | `add --id ID`",
                "**Exit** | `exit`"));

        List<GridPane> tables = tables(rendered);
        assertEquals(1, tables.size());
        // Header plus two data rows, two columns each
        assertEquals(6, tables.get(0).getChildren().size());
    }

    @Test
    public void render_threeColumnTable_fitsViewerWithoutBoundingExplosion() throws Exception {
        // A copy of the three-column load-matrix table from the UG's "Editing the data file"
        // section, not the bundled resource itself: the layout behavior under test is generic,
        // and a copy keeps this test independent of guide rewording.
        VBox[] holder = new VBox[1];
        CountDownLatch layoutDone = new CountDownLatch(1);
        Platform.runLater(() -> {
            VBox rendered = MarkdownRenderer.render(String.join("\n",
                    "When LaiNurse launches, it loads its data as follows:",
                    "",
                    "| `lainurse.json` | `lainurse.backup.json` | LaiNurse starts with |",
                    "|---|---|---|",
                    "| Loads | Any | The data in `lainurse.json`. |",
                    "| Missing | Any | The sample data. Delete `lainurse.json` to start again. |",
                    "| Cannot be loaded | Loads | The data in the backup, which is the data as last saved. |",
                    "| Cannot be loaded | Missing, or cannot be loaded | No data. |",
                    "",
                    "A paragraph after the table."));
            holder[0] = rendered;
            rendered.resize(1040, 700);
            rendered.applyCss();
            rendered.layout();
            layoutDone.countDown();
        });

        assertTrue(layoutDone.await(5, TimeUnit.SECONDS));
        GridPane table = tables(holder[0]).get(0);

        // Symptom guards run first: against the pre-fix renderer the height assertion fails
        // here on its own, directly catching the oversized box, instead of being short-circuited
        // by an earlier column-count failure.
        // The broken renderer sized this grid to thousands of pixels of empty height
        assertTrue(table.getHeight() < 400,
                "table height exploded to " + table.getHeight());
        // and overflowed horizontally past the viewer's width
        assertTrue(table.getBoundsInParent().getWidth() <= 1041,
                "table width overflowed the viewer: " + table.getBoundsInParent().getWidth());
        // The constraints must cover all three columns of the load matrix
        assertEquals(3, table.getColumnConstraints().size());
    }

    @Test
    public void render_tableEscapedPipe_staysInsideOneCell() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "Action | Format",
                "-------|-------",
                "**Range** | write `a \\| b` carefully",
                "**Exit** | `exit`"));

        GridPane table = tables(rendered).get(0);
        // The escaped pipe must not add a third column
        assertEquals(2, table.getColumnConstraints().size());
        assertEquals(6, table.getChildren().size());
        assertTrue(visibleText(table).contains("a | b"));
    }

    @Test
    public void render_tableEscapedPipeAtRowEnd_staysInLastCell() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "Action | Format",
                "-------|-------",
                "**Add** | literal \\|",
                "**Exit** | `exit`"));

        GridPane table = tables(rendered).get(0);
        // A row-final escaped pipe is cell content, not the closing delimiter
        assertEquals(2, table.getColumnConstraints().size());
        assertEquals(6, table.getChildren().size());
        assertTrue(visibleText(table).contains("literal |"));
    }

    @Test
    public void render_tableBackslashRunParity_decidesSplitting() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "Action | Format",
                "-------|-------",
                "**A** | one \\\\| two",
                "**B** | three \\| four"));

        GridPane table = tables(rendered).get(0);
        // Two backslashes are an escaped backslash, so that pipe splits; one backslash escapes it
        assertEquals(3, table.getColumnConstraints().size());
        assertEquals(7, table.getChildren().size());
        assertTrue(visibleText(table).contains("three | four"));
    }

    @Test
    public void render_raggedTableRow_missingCellsKeepGridWellFormed() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "Action | Format | Example",
                "-------|--------|-------",
                "**List** |",
                "**Exit** | `exit` | shows all patients"));

        GridPane table = tables(rendered).get(0);
        // The widest row decides the column count; a short row fills fewer cells, and a
        // trailing pipe is a closing delimiter rather than an extra empty cell
        assertEquals(3, table.getColumnConstraints().size());
        assertEquals(7, table.getChildren().size());
    }

    @Test
    public void render_lineWithoutPipe_terminatesTheTable() {
        VBox rendered = MarkdownRenderer.render(String.join("\n",
                "Action | Format",
                "-------|-------",
                "A plain line ends the table.",
                "**Exit** | `exit`"));

        // A pipe-less line flushes the table so far and becomes its own paragraph, as on GitHub
        assertEquals(2, tables(rendered).size());
        assertTrue(visibleText(rendered).contains("A plain line ends the table."));
    }

    private static void collectNodes(Node node, List<Node> out) {
        out.add(node);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectNodes(child, out);
            }
        }
    }

    private static List<Node> descendants(Node root) {
        List<Node> nodes = new ArrayList<>();
        collectNodes(root, nodes);
        return nodes;
    }

    private static String visibleText(Parent root) {
        StringBuilder text = new StringBuilder();
        for (Node node : descendants(root)) {
            if (node instanceof Label label) {
                text.append(label.getText()).append('\n');
            } else if (node instanceof Hyperlink hyperlink) {
                text.append(hyperlink.getText()).append('\n');
            } else if (node instanceof Text textNode) {
                text.append(textNode.getText()).append('\n');
            }
        }
        return text.toString();
    }

    private static boolean hasHeading(VBox rendered) {
        return descendants(rendered).stream().anyMatch(node -> node instanceof Label label
                && label.getStyle().contains("-fx-font-weight: bold"));
    }

    private static boolean hasTable(VBox rendered) {
        return !tables(rendered).isEmpty();
    }

    private static List<GridPane> tables(VBox rendered) {
        List<GridPane> tables = new ArrayList<>();
        for (Node node : descendants(rendered)) {
            if (node instanceof GridPane grid) {
                tables.add(grid);
            }
        }
        return tables;
    }

    private static List<Hyperlink> hyperlinks(VBox rendered) {
        List<Hyperlink> links = new ArrayList<>();
        for (Node node : descendants(rendered)) {
            if (node instanceof Hyperlink hyperlink) {
                links.add(hyperlink);
            }
        }
        return links;
    }

    private static List<Label> labels(VBox rendered) {
        List<Label> labels = new ArrayList<>();
        for (Node node : descendants(rendered)) {
            if (node instanceof Label label) {
                labels.add(label);
            }
        }
        return labels;
    }
}
