package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;

/**
 * A UI component that displays the result of a command execution.
 */
public class ResultDisplay extends UiPart<Region> {

    private static final String FXML = "ResultDisplay.fxml";
    /** How far one Shift+Up or Shift+Down scrolls the text, in pixels: about two lines. */
    private static final double SCROLL_STEP = 40;

    @FXML
    private TextArea resultDisplay;

    public ResultDisplay() {
        super(FXML);
    }

    public void setFeedbackToUser(String feedbackToUser) {
        requireNonNull(feedbackToUser);
        resultDisplay.setText(feedbackToUser);
    }

    /**
     * Scrolls the text by {@code steps}, up if negative and down if positive, without going past either end. Does
     * nothing if the text fits.
     */
    public void scroll(int steps) {
        if (!(resultDisplay.lookup(".scroll-pane") instanceof ScrollPane scrollPane)) {
            // The text area has not been shown yet, so it has nothing to scroll
            return;
        }
        double scrollableHeight = scrollPane.getContent().getLayoutBounds().getHeight()
                - scrollPane.getViewportBounds().getHeight();
        if (scrollableHeight <= 0) {
            return;
        }
        double range = scrollPane.getVmax() - scrollPane.getVmin();
        double vvalue = scrollPane.getVvalue() + steps * SCROLL_STEP / scrollableHeight * range;
        scrollPane.setVvalue(Math.clamp(vvalue, scrollPane.getVmin(), scrollPane.getVmax()));
    }

}
