package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import seedu.address.logic.commands.CommandResult.AppointmentRange;
import seedu.address.model.LaiNurse;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.testutil.PatientBuilder;

public class AppointmentListPanelTest {

    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 10, 1);
    private static final int DAYS = 40;

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
    public void pageDownAndPageUp_moveAboutOneScreenAndSelectOnlyAppointments() throws Exception {
        onFxThread(() -> {
            AtomicReference<Optional<PatientAppointment>> selected = new AtomicReference<>(Optional.empty());
            ListView<?> list = showLongPage(selected);
            int first = list.getSelectionModel().getSelectedIndex();
            int last = list.getItems().size() - 1;

            press(list, KeyCode.PAGE_DOWN);
            int afterOnePage = list.getSelectionModel().getSelectedIndex();
            // further than Down, which moves two rows past the next day's heading, but not to the end of the page
            assertTrue(afterOnePage > first + 2, "moved to row " + afterOnePage);
            assertTrue(afterOnePage < last, "moved to row " + afterOnePage);
            assertTrue(list.getItems().get(afterOnePage) instanceof AppointmentPage.Entry);
            assertEquals(afterOnePage, indexOfSelected(list, selected));

            press(list, KeyCode.PAGE_UP);
            assertTrue(list.getSelectionModel().getSelectedIndex() < afterOnePage);
            assertTrue(list.getItems().get(list.getSelectionModel().getSelectedIndex())
                    instanceof AppointmentPage.Entry);

            // each key stops at the appointment at its end of the page
            pressUntilSelectionStops(list, KeyCode.PAGE_DOWN);
            assertEquals(last, list.getSelectionModel().getSelectedIndex());
            pressUntilSelectionStops(list, KeyCode.PAGE_UP);
            assertEquals(first, list.getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void homeAndEnd_selectFirstAndLastAppointment() throws Exception {
        onFxThread(() -> {
            ListView<?> list = showLongPage(new AtomicReference<>(Optional.empty()));
            int first = list.getSelectionModel().getSelectedIndex();

            press(list, KeyCode.END);
            assertEquals(list.getItems().size() - 1, list.getSelectionModel().getSelectedIndex());
            press(list, KeyCode.HOME);
            assertEquals(first, list.getSelectionModel().getSelectedIndex());
        });
    }

    @Test
    public void weekArrows_firstAndLastWeek_hideArrowWithNoWeekBeyond() throws Exception {
        onFxThread(() -> {
            LaiNurse data = new LaiNurse();
            data.addPatient(new PatientBuilder().withId("S1234567A")
                    .withAppointments("0000-01-01", "9999-12-31").build());
            ModelManager model = new ModelManager(data, new UserPrefs());
            AppointmentListPanel panel = new AppointmentListPanel(model.getAppointmentList(),
                    selected -> { }, () -> { });
            Parent root = panel.getRoot();
            ListView<?> list = (ListView<?>) root.lookup(".list-view");
            Node previous = root.lookup("#previousWeekButton");
            Node next = root.lookup("#nextWeekButton");
            assertArrowsShown(previous, true, next, true);

            panel.showAppointment(model.getAppointmentList().get(0));
            assertArrowsShown(previous, false, next, true);
            press(list, KeyCode.RIGHT);
            assertArrowsShown(previous, true, next, true);

            panel.showAppointment(model.getAppointmentList().get(1));
            assertArrowsShown(previous, true, next, false);
            press(list, KeyCode.LEFT);
            assertArrowsShown(previous, true, next, true);
        });
    }

    private static void assertArrowsShown(Node previous, boolean isPreviousShown, Node next, boolean isNextShown) {
        assertEquals(isPreviousShown, previous.isVisible());
        assertEquals(isPreviousShown, previous.isManaged());
        assertEquals(isNextShown, next.isVisible());
        assertEquals(isNextShown, next.isManaged());
    }

    /**
     * Shows a page with one appointment on each of many days, too long to fit, and returns its list.
     */
    private static ListView<?> showLongPage(AtomicReference<Optional<PatientAppointment>> selected) {
        LaiNurse data = new LaiNurse();
        data.addPatient(new PatientBuilder().withId("S1234567A").withAppointments(IntStream.range(0, DAYS)
                .mapToObj(day -> FIRST_DAY.plusDays(day).toString()).toArray(String[]::new)).build());
        ModelManager model = new ModelManager(data, new UserPrefs());
        AppointmentListPanel panel = new AppointmentListPanel(model.getAppointmentList(), selected::set, () -> { });
        StackPane root = new StackPane(panel.getRoot());
        Scene scene = new Scene(root, 800, 400);
        scene.getStylesheets().add(AppointmentListPanelTest.class.getResource("/view/DarkTheme.css").toExternalForm());
        panel.showRange(new AppointmentRange(FIRST_DAY, FIRST_DAY.plusDays(DAYS - 1)));
        root.resize(800, 400);
        root.applyCss();
        root.layout();
        // Virtualized list rows are created during layout and need their own CSS pass.
        root.applyCss();
        root.layout();
        return (ListView<?>) root.lookup(".list-view");
    }

    private static int indexOfSelected(ListView<?> list, AtomicReference<Optional<PatientAppointment>> selected) {
        PatientAppointment chosen = selected.get().orElseThrow();
        return IntStream.range(0, list.getItems().size())
                .filter(i -> list.getItems().get(i) instanceof AppointmentPage.Entry entry
                        && entry.appointment().isSameEntry(chosen))
                .findFirst().orElseThrow();
    }

    private static void pressUntilSelectionStops(ListView<?> list, KeyCode key) {
        for (int i = 0; i < list.getItems().size(); i++) {
            press(list, key);
        }
    }

    private static void press(ListView<?> list, KeyCode key) {
        list.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
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
