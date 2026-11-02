package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import seedu.address.model.LaiNurse;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Patient;
import seedu.address.testutil.PatientBuilder;

public class PatientDetailPanelTest {

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
    public void show_samePatientAfterClear_startsAtTop() throws Exception {
        onFxThread(() -> {
            Patient alex = new PatientBuilder().withId("S1234567A").withName("Alex Yeoh").build();
            Patient bernice = new PatientBuilder().withId("S2345678B").withName("Bernice Yu").build();
            LaiNurse laiNurse = new LaiNurse();
            laiNurse.addPatient(alex);
            laiNurse.addPatient(bernice);
            ModelManager model = new ModelManager(laiNurse, new UserPrefs());
            PatientDetailPanel panel = new PatientDetailPanel(model.getLaiNurse().getPatientList(),
                    model.getAppointmentList(), "Choose a patient");
            ScrollPane details = (ScrollPane) panel.getRoot().lookup("#details");

            panel.show(alex.getId());
            details.setVvalue(1);
            // a change to the shown patient keeps the scroll position
            panel.refresh();
            assertEquals(1, details.getVvalue());

            // another patient starts at the top
            panel.show(bernice.getId());
            assertEquals(0, details.getVvalue());

            // the same patient shown again after the panel showed no patient, as after an empty search (#201)
            details.setVvalue(1);
            panel.clear();
            panel.show(bernice.getId());
            assertEquals(0, details.getVvalue());
        });
    }

    @Test
    public void details_fittingOrOverflowing_keepTheSameWidth() throws Exception {
        onFxThread(() -> {
            Patient alex = new PatientBuilder().withId("S1234567A").withName("Alex Yeoh").build();
            LaiNurse laiNurse = new LaiNurse();
            laiNurse.addPatient(alex);
            ModelManager model = new ModelManager(laiNurse, new UserPrefs());
            PatientDetailPanel panel = new PatientDetailPanel(model.getLaiNurse().getPatientList(),
                    model.getAppointmentList(), "Choose a patient");
            StackPane root = new StackPane(panel.getRoot());
            new Scene(root);
            ScrollPane details = (ScrollPane) root.lookup("#details");
            panel.show(alex.getId());

            double widthWhenFitting = viewportWidthAt(root, details, 2000);
            double widthWhenOverflowing = viewportWidthAt(root, details, 100);

            // a width that changed with the height would change where the text wraps, and could make the panel flicker
            assertEquals(widthWhenFitting, widthWhenOverflowing);
        });
    }

    private static double viewportWidthAt(StackPane root, ScrollPane details, double height) {
        root.resize(500, height);
        root.applyCss();
        root.layout();
        // The scroll bar comes and goes in the pass after the one that finds whether it is needed
        details.requestLayout();
        root.layout();
        return details.getViewportBounds().getWidth();
    }

    private static void onFxThread(Runnable action) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        done.get(10, TimeUnit.SECONDS);
    }
}
