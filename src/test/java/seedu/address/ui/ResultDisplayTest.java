package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;

public class ResultDisplayTest {

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // Another UI test initialized the toolkit
        }
        Platform.setImplicitExit(false);
    }

    @Test
    public void scroll_longText_movesAndStopsAtEnds() throws Exception {
        onFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            StackPane root = show(display, "line\n".repeat(50));
            ScrollPane scrollPane = (ScrollPane) root.lookup(".scroll-pane");
            assertEquals(scrollPane.getVmin(), scrollPane.getVvalue());

            display.scroll(1);
            double afterOneStep = scrollPane.getVvalue();
            assertTrue(afterOneStep > scrollPane.getVmin());

            display.scroll(1);
            assertTrue(scrollPane.getVvalue() > afterOneStep);

            display.scroll(-5);
            assertEquals(scrollPane.getVmin(), scrollPane.getVvalue());

            display.scroll(1000);
            assertEquals(scrollPane.getVmax(), scrollPane.getVvalue());
        });
    }

    @Test
    public void scroll_textFits_doesNothing() throws Exception {
        onFxThread(() -> {
            ResultDisplay display = new ResultDisplay();
            StackPane root = show(display, "short");
            ScrollPane scrollPane = (ScrollPane) root.lookup(".scroll-pane");

            display.scroll(1);

            assertEquals(scrollPane.getVmin(), scrollPane.getVvalue());
        });
    }

    @Test
    public void scroll_notShownYet_doesNothing() throws Exception {
        onFxThread(() -> new ResultDisplay().scroll(1));
    }

    private static StackPane show(ResultDisplay display, String text) {
        display.setFeedbackToUser(text);
        StackPane root = new StackPane(display.getRoot());
        new Scene(root, 400, 100);
        root.resize(400, 100);
        root.applyCss();
        root.layout();
        root.applyCss();
        root.layout();
        return root;
    }

    private static void onFxThread(Runnable action) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                done.complete(null);
            } catch (Throwable e) {
                done.completeExceptionally(e);
            }
        });
        done.get(10, TimeUnit.SECONDS);
    }
}
