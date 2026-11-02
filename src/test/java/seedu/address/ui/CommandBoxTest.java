package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.address.logic.parser.TabCompleter;
import seedu.address.model.patient.PatientId;

public class CommandBoxTest {

    private static final List<PatientId> IDS = List.of(new PatientId("S1234567A"), new PatientId("T7654321B"));

    private final List<PatientId> ids = new ArrayList<>(IDS);
    private CommandBox commandBox;
    private TextField textField;
    private Button otherNode;
    private StatusBarFooter statusBar;
    private Stage stage;

    /**
     * The command box is loaded from FXML with JavaFX controls, which need the toolkit.
     */
    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // Toolkit was already initialized by another test class
        }
        // Otherwise the toolkit stops when a test hides the last window, and the next test cannot run
        Platform.setImplicitExit(false);
    }

    @BeforeEach
    public void setUp() throws Exception {
        onFxThread(() -> {
            statusBar = new StatusBarFooter(Path.of("data", "lainurse.json"));
            commandBox = new CommandBox(text -> null, text -> TabCompleter.complete(text, ids), () -> { },
                    statusBar::showCompletions);
            textField = (TextField) commandBox.getRoot().lookup(".text-field");
            otherNode = new Button("Patients");
            // The hint shows only while the command box has the focus, so the test needs a focused window. It is
            // transparent, so that it does not flash up while the tests run.
            stage = new Stage();
            stage.setOpacity(0);
            stage.setScene(new Scene(new VBox(commandBox.getRoot(), otherNode, statusBar.getRoot())));
            stage.show();
            stage.requestFocus();
            commandBox.requestFocus();
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        onFxThread(() -> stage.hide());
    }

    @Test
    public void tab_severalCompletions_goesThroughThemInTurn() throws Exception {
        onFxThread(() -> {
            textField.setText("dele");
            pressTab();
            assertEquals("delete", textField.getText());
            pressTab();
            assertEquals("deleteappt", textField.getText());
            pressTab();
            assertEquals("delete", textField.getText());
        });
    }

    @Test
    public void tab_onlyCompletion_nextTabCompletesTheNewText() throws Exception {
        onFxThread(() -> {
            textField.setText("addap");
            pressTab();
            assertEquals("addappt", textField.getText());
            pressTab();
            assertEquals("addappt S1234567A", textField.getText());
            pressTab();
            assertEquals("addappt T7654321B", textField.getText());
        });
    }

    @Test
    public void tab_afterTyping_startsFromTheFirstCompletionOfTheNewText() throws Exception {
        onFxThread(() -> {
            textField.setText("dele");
            pressTab();
            textField.setText("delete T");
            pressTab();
            assertEquals("delete T7654321B", textField.getText());
        });
    }

    @Test
    public void tab_patientsChangedSinceTyping_usesThePatientsAsTheyAre() throws Exception {
        onFxThread(() -> {
            textField.setText("view T");
            ids.set(1, new PatientId("T1111111C"));
            pressTab();
            assertEquals("view T1111111C", textField.getText());
        });
    }

    @Test
    public void tab_nothingToComplete_keepsText() throws Exception {
        onFxThread(() -> {
            pressTab();
            assertEquals("", textField.getText());
            textField.setText("exit");
            pressTab();
            assertEquals("exit", textField.getText());
        });
    }

    @Test
    public void hint_whileTyping_showsWhatTabCanComplete() throws Exception {
        assumeTrue(waitForFocusedCommandBox(), "the display gave the test window no focus");
        onFxThread(() -> {
            textField.setText("dele");
            assertEquals("delete · deleteappt", statusBar.getCompletionHint());
            textField.setText("editappt S1234567A 2 --");
            assertEquals("--date · --details · --add-details", statusBar.getCompletionHint());
            textField.setText("view ");
            assertEquals("S1234567A · T7654321B", statusBar.getCompletionHint());
        });
    }

    @Test
    public void hint_whileTabGoesThroughCompletions_keepsShowingThem() throws Exception {
        assumeTrue(waitForFocusedCommandBox(), "the display gave the test window no focus");
        onFxThread(() -> {
            textField.setText("dele");
            pressTab();
            assertEquals("delete · deleteappt", statusBar.getCompletionHint());
            pressTab();
            assertEquals("delete · deleteappt", statusBar.getCompletionHint());
        });
    }

    @Test
    public void hint_afterOnlyCompletion_showsWhatTheNextTabCompletes() throws Exception {
        assumeTrue(waitForFocusedCommandBox(), "the display gave the test window no focus");
        onFxThread(() -> {
            textField.setText("addap");
            assertEquals("addappt", statusBar.getCompletionHint());
            pressTab();
            assertEquals("S1234567A · T7654321B", statusBar.getCompletionHint());
        });
    }

    @Test
    public void hint_nothingToComplete_isHidden() throws Exception {
        assumeTrue(waitForFocusedCommandBox(), "the display gave the test window no focus");
        onFxThread(() -> {
            textField.setText("exit");
            assertEquals("", statusBar.getCompletionHint());
            // the whole ID is typed, so Tab would not change the text
            textField.setText("view S1234567A");
            assertEquals("", statusBar.getCompletionHint());
            textField.setText("dele");
            textField.setText("");
            assertEquals("", statusBar.getCompletionHint());
        });
    }

    @Test
    public void hint_focusLeavesCommandBox_isHiddenUntilFocusComesBack() throws Exception {
        assumeTrue(waitForFocusedCommandBox(), "the display gave the test window no focus");
        onFxThread(() -> {
            textField.setText("dele");
            otherNode.requestFocus();
            assertEquals("", statusBar.getCompletionHint());
            commandBox.requestFocus();
            assertEquals("delete · deleteappt", statusBar.getCompletionHint());
        });
    }

    private void pressTab() {
        Event.fireEvent(textField, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB, false, false, false, false));
    }

    /**
     * Returns true once the command box has the focus, or false if it still has none after a few seconds. The
     * platform gives the focus some time after the window is shown.
     */
    private boolean waitForFocusedCommandBox() throws Exception {
        for (int i = 0; i < 50; i++) {
            boolean[] isFocused = new boolean[1];
            onFxThread(() -> isFocused[0] = stage.isFocused() && commandBox.isFocused());
            if (isFocused[0]) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    /**
     * Runs {@code action} on the JavaFX thread, where the command box must be created and used, and waits for it.
     * A failed assertion in {@code action} fails the test.
     */
    private static void onFxThread(Runnable action) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                done.complete(null);
            } catch (Throwable throwable) {
                done.completeExceptionally(throwable);
            }
        });
        try {
            done.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }
}
