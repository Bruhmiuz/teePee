package seedu.address.ui;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.Settings;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentClashes;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.model.patient.PatientId;

/**
 * Panel that shows all the details of one patient, including their upcoming and latest past appointments.
 * It follows the list of all patients, so it shows the new details after the patient is edited, and goes back
 * to its starting message after they are deleted. One appointment can be marked, as the appointment view does
 * for the appointment selected there.
 */
public class PatientDetailPanel extends UiPart<Region> {

    private static final String FXML = "PatientDetailPanel.fxml";
    private static final String NO_MEDICAL_HISTORY = "None recorded";
    private static final String NO_NOTES = "None recorded";
    private static final String EMPTY_VALUE_STYLE_CLASS = "patient-detail-value-empty";
    private static final String ROW_STYLE_CLASS = "detail-appointment-row";
    private static final String TODAY_STYLE_CLASS = "detail-appointment-today";
    private static final String PAST_STYLE_CLASS = "detail-appointment-past";
    private static final String MARKED_STYLE_CLASS = "detail-appointment-marked";
    /**
     * The width of an appointment's date: enough for one with a year, such as "Mon 28 Dec 2026", and a little more,
     * so that the time stays close to a short date.
     */
    private static final double DATE_WIDTH = 130;
    /** The width that a date may shrink to beside the details: just enough for one such as "Mon 28 Dec 2026". */
    private static final double DATE_MIN_WIDTH = 125;
    /**
     * The width that a date may shrink to with the details under the time, in a narrow panel: enough for one such as
     * "Wed 28 Dec", with a year wrapping under it, so that the time keeps whole words.
     */
    private static final double DATE_NARROW_MIN_WIDTH = 90;
    /**
     * The width of an appointment's time beside its details, wide enough for "Unspecified" with room after it,
     * so that the details of all rows line up where they would after a wider date.
     */
    private static final double TIME_WIDTH = 140;
    /**
     * The width that an appointment's details need beside its time: that of a long word such as "OSTEOARTHRITIS",
     * as for the values above. In a narrower row, the details go under the time.
     */
    private static final double DETAILS_MIN_WIDTH = 120;
    /** The space between the parts of an appointment row. */
    private static final double ROW_SPACING = 20;
    /** The width of an appointment's number before its date, enough for one such as "#99". */
    private static final double NUMBER_WIDTH = 26;
    /** The space between an appointment's number and its date. */
    private static final double NUMBER_SPACING = 8;
    /** How far one Shift+Up or Shift+Down scrolls the details, in pixels: about as far as a mouse wheel step. */
    private static final double SCROLL_STEP = 40;

    private final ObservableList<Patient> allPatients;
    private final ObservableList<PatientAppointment> allAppointments;
    /** The settings that decide clashes and which appointments are over (#216). */
    private final ObjectProperty<Settings> settings = new SimpleObjectProperty<>(Settings.DEFAULT);
    /** The width of the date in every appointment row, so that the times line up. */
    private final DoubleProperty dateWidth = new SimpleDoubleProperty(DATE_WIDTH);
    /** The time and details of each appointment row shown, which {@link #updateRowLayout()} arranges. */
    private final List<TimeAndDetails> rowParts = new ArrayList<>();
    /** Whether the appointment rows show their details beside the time, rather than under it. */
    private boolean isDetailsBeside;

    /** The ID of the patient being shown, or null if no patient is shown. */
    private PatientId shownId;
    /** The appointment of the shown patient to mark, or null to mark none. */
    private Appointment markedAppointment;
    /**
     * The ID of the patient whose details fill the panel, or null after the panel showed no patient, so that a
     * patient shown again after that starts at the top of their details.
     */
    private PatientId filledId;

    @FXML
    private Label placeholder;
    @FXML
    private ScrollPane details;
    @FXML
    private Label name;
    @FXML
    private Label patientId;
    @FXML
    private Label phone;
    @FXML
    private Label address;
    @FXML
    private Label medicalHistory;
    @FXML
    private Label notes;
    @FXML
    private Label upcomingNote;
    @FXML
    private VBox upcomingRows;
    @FXML
    private VBox pastSection;
    @FXML
    private Label pastNote;
    @FXML
    private VBox pastRows;

    /**
     * Creates a {@code PatientDetailPanel} that looks patients up in {@code allPatients}, and shows
     * {@code placeholderText} while no patient is shown.
     */
    public PatientDetailPanel(ObservableList<Patient> allPatients, ObservableList<PatientAppointment> allAppointments,
            String placeholderText) {
        super(FXML);
        requireAllNonNull(allPatients, allAppointments, placeholderText);
        this.allPatients = allPatients;
        this.allAppointments = allAppointments;
        placeholder.setText(placeholderText);
        allPatients.addListener((ListChangeListener<Patient>) change -> refresh());
        allAppointments.addListener((ListChangeListener<PatientAppointment>) change -> refresh());
        settings.addListener((unused, oldSettings, newSettings) -> refresh());
        upcomingRows.widthProperty().addListener((unused, oldWidth, width) -> updateRowLayout());
        refresh();
    }

    /**
     * Shows the appointments by {@code newSettings} from now on, such as the settings that the user changes (#216),
     * in place of the default ones.
     */
    public void useSettings(ObservableValue<Settings> newSettings) {
        requireNonNull(newSettings);
        settings.bind(newSettings);
    }

    /**
     * Shows the details of the patient with {@code id}, with no appointment marked.
     */
    public void show(PatientId id) {
        show(id, Optional.empty());
    }

    /**
     * Shows the details of the patient with {@code id}, and marks {@code appointment} among their appointments
     * if it is given.
     */
    public void show(PatientId id, Optional<Appointment> appointment) {
        requireAllNonNull(id, appointment);
        shownId = id;
        markedAppointment = appointment.orElse(null);
        refresh();
    }

    /**
     * Stops showing a patient, and shows the starting message.
     */
    public void clear() {
        shownId = null;
        markedAppointment = null;
        refresh();
    }

    /**
     * Returns the ID of the patient being shown, or {@code Optional#empty()} if no patient is shown.
     */
    public Optional<PatientId> getShownId() {
        return Optional.ofNullable(shownId);
    }

    /**
     * Scrolls the details by {@code steps} steps, up if it is negative and down if it is positive. Does nothing
     * if no patient is shown, or if the details fit without scrolling.
     */
    public void scroll(int steps) {
        double scrollableHeight = details.getContent().getLayoutBounds().getHeight()
                - details.getViewportBounds().getHeight();
        if (!details.isVisible() || scrollableHeight <= 0) {
            return;
        }
        double range = details.getVmax() - details.getVmin();
        double vvalue = details.getVvalue() + steps * SCROLL_STEP / scrollableHeight * range;
        details.setVvalue(Math.clamp(vvalue, details.getVmin(), details.getVmax()));
    }

    /**
     * Returns true if the details have the keyboard focus, as they do after the user clicks them.
     */
    public boolean isFocused() {
        return details.isFocused();
    }

    /**
     * Shows the patient's details again as of now, after the patient changes, an appointment passes or a new day
     * begins. Keeps the scroll position.
     */
    public void refresh() {
        Optional<Patient> patient = Optional.ofNullable(shownId).flatMap(id -> allPatients.stream()
                .filter(p -> p.getId().equals(id)).findFirst());
        if (patient.isPresent()) {
            fill(patient.get());
        } else {
            shownId = null;
            markedAppointment = null;
            filledId = null;
            setShown(placeholder, true);
            setShown(details, false);
        }
    }

    private void fill(Patient patient) {
        // A newly shown patient starts at the top of their details, but an edit keeps the scroll position.
        if (!patient.getId().equals(filledId)) {
            details.setVvalue(0);
            filledId = patient.getId();
        }
        name.setText(patient.getName().fullName);
        patientId.setText(patient.getId().value);
        phone.setText(patient.getPhone().value);
        address.setText(patient.getAddress().value);
        setValue(medicalHistory, patient.getMedicalHistory().map(Object::toString), NO_MEDICAL_HISTORY);
        setValue(notes, patient.getNotes().map(Object::toString), NO_NOTES);

        // An edit can remove the marked appointment
        if (markedAppointment != null && !patient.getSchedule().contains(markedAppointment)) {
            markedAppointment = null;
        }
        LocalDateTime now = LocalDateTime.now();
        DetailAppointments appointments = DetailAppointments.of(patient.getSchedule(), now, settings.get());
        upcomingNote.setText(appointments.describeUpcoming());
        rowParts.clear();
        upcomingRows.getChildren().setAll(createRows(patient, appointments.getUpcoming(), now));
        pastNote.setText(appointments.describePast());
        pastRows.getChildren().setAll(createRows(patient, appointments.getPast(), now));
        // The new rows need their styles, such as the font of today's rows, before their times can be measured
        upcomingRows.applyCss();
        pastRows.applyCss();
        updateRowLayout();
        setShown(pastSection, !appointments.getPast().isEmpty());

        setShown(placeholder, false);
        setShown(details, true);
    }

    private List<Node> createRows(Patient patient, List<Appointment> appointments, LocalDateTime now) {
        return appointments.stream().map(appointment -> createRow(patient, appointment, now)).toList();
    }

    /**
     * Returns one row of the appointments: the number and date, the time, then optional details,
     * which wrap onto more lines if they are long. An appointment that has passed is dimmed, also one earlier
     * today. {@link #updateRowLayout()} decides how the parts share the row, so that it is never wider than the
     * panel.
     */
    private Node createRow(Patient patient, Appointment appointment, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        // The number has its own column before the date, as voted in #163 (P1)
        Label number = new Label("#" + appointment.getNumber());
        number.getStyleClass().add("detail-appointment-number");
        number.setMinWidth(NUMBER_WIDTH);
        Label date = new Label(AppointmentFormatter.formatDetailDate(appointment.getDate(), today));
        date.getStyleClass().add("detail-appointment-date");
        date.setWrapText(true);
        date.setMinHeight(Region.USE_PREF_SIZE);
        date.minWidthProperty().bind(dateWidth);
        date.prefWidthProperty().bind(dateWidth);
        Label time = new Label(AppointmentFormatter.formatDetailTime(appointment));
        // An appointment with no time takes the color of its row, as one with a time does
        time.getStyleClass().add("detail-appointment-time");
        time.setWrapText(true);
        time.setMinWidth(0);
        time.setMinHeight(Region.USE_PREF_SIZE);
        boolean isClashing = !AppointmentClashes.find(allAppointments,
                new PatientAppointment(patient, appointment), settings.get(), now).isEmpty();
        TimeMark.show(time, ClashMark.create(), PinMark.create(), isClashing, appointment.isDone());
        Label details = new Label(appointment.getDetails().map(Object::toString).orElse(""));
        details.getStyleClass().add("detail-appointment-details");
        details.setWrapText(true);
        details.setMinWidth(0);
        details.setMinHeight(Region.USE_PREF_SIZE);
        setShown(details, appointment.getDetails().isPresent());
        TimeAndDetails timeAndDetails = new TimeAndDetails(time, details);
        timeAndDetails.arrange(isDetailsBeside);
        rowParts.add(timeAndDetails);
        HBox.setHgrow(timeAndDetails.grid(), Priority.ALWAYS);

        HBox numberAndDate = new HBox(NUMBER_SPACING, number, date);
        numberAndDate.setAlignment(Pos.BASELINE_LEFT);
        HBox row = new HBox(ROW_SPACING, numberAndDate, timeAndDetails.grid());
        row.getStyleClass().add(ROW_STYLE_CLASS);
        if (AppointmentFormatter.isOver(appointment, now, settings.get())) {
            row.getStyleClass().add(PAST_STYLE_CLASS);
        } else if (AppointmentFormatter.isToday(appointment, today)) {
            row.getStyleClass().add(TODAY_STYLE_CLASS);
        }
        if (appointment.isSameAppointment(markedAppointment)) {
            row.getStyleClass().add(MARKED_STYLE_CLASS);
        }
        if (isClashing) {
            row.getStyleClass().add("detail-appointment-clash");
        }
        return row;
    }

    /**
     * Arranges the appointment rows for the width of the panel. With room, each row shows its date, time and
     * details side by side, and all rows give them the same widths, so that they line up. In a narrower panel, or
     * if no row has details, the details go under the time. The dates of all rows then shrink together, down to the
     * width of a date without its year, so that a time that wraps keeps whole words.
     */
    private void updateRowLayout() {
        if (rowParts.isEmpty()) {
            return;
        }
        Region row = (Region) rowParts.get(0).grid().getParent();
        double rowWidth = upcomingRows.getWidth() - row.snappedLeftInset() - row.snappedRightInset()
                - NUMBER_WIDTH - NUMBER_SPACING;
        double roomBesideDetails = rowWidth - 2 * ROW_SPACING - TIME_WIDTH - DETAILS_MIN_WIDTH;
        boolean isBeside = rowParts.stream().anyMatch(TimeAndDetails::hasDetails)
                && roomBesideDetails >= DATE_MIN_WIDTH;
        if (isBeside) {
            dateWidth.set(Math.min(roomBesideDetails, DATE_WIDTH));
        } else {
            double widestTime = rowParts.stream().mapToDouble(parts -> parts.time().prefWidth(-1)).max().orElse(0);
            dateWidth.set(Math.clamp(rowWidth - ROW_SPACING - widestTime, DATE_NARROW_MIN_WIDTH, DATE_WIDTH));
        }
        if (isBeside != isDetailsBeside) {
            isDetailsBeside = isBeside;
            rowParts.forEach(parts -> parts.arrange(isBeside));
        }
    }

    /**
     * Sets the text of {@code label} to {@code text}, or to {@code textIfEmpty} in a dimmed style.
     */
    private static void setValue(Label label, Optional<String> text, String textIfEmpty) {
        label.setText(text.orElse(textIfEmpty));
        label.getStyleClass().remove(EMPTY_VALUE_STYLE_CLASS);
        if (text.isEmpty()) {
            label.getStyleClass().add(EMPTY_VALUE_STYLE_CLASS);
        }
    }

    /**
     * The time and details of one appointment row, in a grid that puts the details beside the time or under it.
     */
    private record TimeAndDetails(GridPane grid, Label time, Label details) {

        TimeAndDetails(Label time, Label details) {
            this(new GridPane(), time, details);
            grid.setHgap(ROW_SPACING);
            grid.add(time, 0, 0);
            grid.add(details, 1, 0);
            // The time and the first line of the details stay level with the date, also when the details wrap
            GridPane.setValignment(time, VPos.TOP);
            GridPane.setValignment(details, VPos.TOP);
        }

        boolean hasDetails() {
            return details.isManaged();
        }

        /**
         * Puts the details beside the time, which then keeps {@link #TIME_WIDTH}, or under the time, which then
         * takes the room of both and wraps if it is too narrow.
         */
        void arrange(boolean isBeside) {
            GridPane.setConstraints(details, isBeside ? 1 : 0, isBeside ? 0 : 1);
            ColumnConstraints rest = new ColumnConstraints();
            rest.setHgrow(Priority.ALWAYS);
            if (isBeside) {
                grid.getColumnConstraints().setAll(new ColumnConstraints(TIME_WIDTH), rest);
            } else {
                grid.getColumnConstraints().setAll(rest);
            }
        }
    }
}
