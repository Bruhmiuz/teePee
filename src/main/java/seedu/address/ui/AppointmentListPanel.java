package seedu.address.ui;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import seedu.address.logic.commands.CommandResult.AppointmentRange;
import seedu.address.model.PatientScope;
import seedu.address.model.Settings;
import seedu.address.model.patient.AppointmentClashes;
import seedu.address.model.patient.PatientAppointment;

/**
 * Panel that lists appointments day by day: the appointment view's list. It shows either one week at a time,
 * or, after {@code viewappt}, the appointments in a range of dates on one page.
 * With the focus in the list, Up and Down move between appointments, and in the weekly view, Left and Right go
 * to the previous or next week. Moving the selection runs no command, so the rest of the window keeps its state.
 */
public class AppointmentListPanel extends UiPart<Region> {

    private static final String FXML = "AppointmentListPanel.fxml";
    private static final String THIS_WEEK_STYLE_CLASS = "list-header-note-this";
    /**
     * The space after the last column of a day heading and an appointment row, as set in the style sheet. The list
     * header lines up with it.
     */
    private static final double ROW_END_PADDING = 12;
    private static final String DAY_CELL_STYLE_CLASS = "appointment-day-cell";
    private static final String ENTRY_CELL_STYLE_CLASS = "appointment-entry-cell";

    private final ObservableList<PatientAppointment> allAppointments;
    /** The settings that decide clashes and which appointments are over (#216). */
    private final ObjectProperty<Settings> settings = new SimpleObjectProperty<>(Settings.DEFAULT);
    private final Consumer<Optional<PatientAppointment>> onAppointmentChosen;
    private final Runnable onUpAtTop;
    private final ObservableList<AppointmentPage.Row> rows = FXCollections.observableArrayList();

    /** The week of the weekly view. While a range is shown, the week that Esc goes back to. */
    private AppointmentWeek week;
    /** The range shown on one page, or null in the weekly view. */
    private AppointmentRange range;
    /** Whose appointments show, in the range or the week: part of the filter that {@code viewappt} sets. */
    private PatientScope scope = PatientScope.DEFAULT;
    private AppointmentPage page;
    /** The room that each row leaves empty at its end, so that it ends where the header's count ends. */
    private ReadOnlyDoubleProperty rowGutter;
    /** When the page was built, which decides the appointments that have passed. */
    private LocalDateTime pageTime = LocalDateTime.now();

    @FXML
    private HBox header;
    @FXML
    private Button previousWeekButton;
    @FXML
    private Button nextWeekButton;
    @FXML
    private Label weekDates;
    @FXML
    private Label weekRelative;
    @FXML
    private Label weekCount;
    @FXML
    private ListView<AppointmentPage.Row> appointmentListView;

    /**
     * Creates an {@code AppointmentListPanel} that shows the appointments in {@code allAppointments}, starting
     * with this week. {@code onAppointmentChosen} receives each appointment that the user selects, or
     * {@code Optional#empty()} when no appointment is selected any more. {@code onUpAtTop} runs when the user
     * presses Up at the first appointment of the page, or on a page with none, so that the focus can move on.
     */
    public AppointmentListPanel(ObservableList<PatientAppointment> allAppointments,
            Consumer<Optional<PatientAppointment>> onAppointmentChosen, Runnable onUpAtTop) {
        super(FXML);
        requireAllNonNull(allAppointments, onAppointmentChosen, onUpAtTop);
        this.allAppointments = allAppointments;
        this.onAppointmentChosen = onAppointmentChosen;
        this.onUpAtTop = onUpAtTop;
        appointmentListView.setItems(rows);
        rowGutter = ListHeaders.alignEndWithRows(header, appointmentListView, ROW_END_PADDING);
        appointmentListView.setCellFactory(listView -> new RowCell());
        Label placeholder = new Label("No appointments in this range.");
        placeholder.getStyleClass().add("list-placeholder");
        appointmentListView.setPlaceholder(placeholder);
        appointmentListView.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        appointmentListView.setOnMouseClicked(event -> chooseSelected());
        allAppointments.addListener((ListChangeListener<PatientAppointment>) change -> refresh());
        settings.addListener((unused, oldSettings, newSettings) -> refresh());
        week = AppointmentWeek.of(LocalDate.now());
        showPage();
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
     * Shows only the appointments of the patients in {@code scope}, on the page that is shown and on the pages that
     * follow, until another {@code viewappt} or Esc. Selects nothing.
     */
    public void setScope(PatientScope scope) {
        this.scope = requireNonNull(scope);
        showPage();
    }

    /**
     * Shows the appointments in {@code range} on one page, in place of the weekly view, and selects the first
     * one. The weekly view keeps its week, which Esc goes back to.
     */
    public void showRange(AppointmentRange range) {
        this.range = requireNonNull(range);
        showPage();
        page.firstEntryIndex().ifPresentOrElse(this::select, this::clearSelection);
    }

    /**
     * Shows the weekly view of this week, in place of any range, and selects the next appointment from now.
     */
    public void showThisWeek() {
        week = AppointmentWeek.of(LocalDate.now());
        range = null;
        showPage();
        selectNextFromNow();
    }

    /**
     * Selects the next appointment from now on the page that is shown: the first that has not passed, or the first
     * if all have passed. Selects nothing on a page with no appointments.
     */
    public void selectNextFromNow() {
        page.defaultEntryIndex().ifPresentOrElse(this::select, this::clearSelection);
    }

    /**
     * Selects {@code appointment} on the page that is shown if it is there, so that a command run on a range stays
     * on that range. Otherwise shows the weekly view of its week, in place of any range, and selects it there. If
     * its patient is archived and archived patients' appointments are hidden, they show from then on.
     */
    public void showAppointment(PatientAppointment appointment) {
        requireNonNull(appointment);
        if (!scope.includes(appointment.getPatient())) {
            scope = PatientScope.ALL;
            showPage();
        }
        if (page.indexOf(appointment).isEmpty()) {
            week = AppointmentWeek.of(appointment.getAppointment().getDate());
            range = null;
            showPage();
        }
        selectAppointment(appointment);
    }

    /**
     * Selects {@code appointment} on the page that is shown, and scrolls to it.
     *
     * @throws IllegalArgumentException if {@code appointment} is not on the page.
     */
    public void selectAppointment(PatientAppointment appointment) {
        requireNonNull(appointment);
        OptionalInt index = page.indexOf(appointment);
        if (index.isEmpty()) {
            throw new IllegalArgumentException("Appointment " + appointment + " is not on the page shown.");
        }
        select(index.getAsInt());
    }

    /**
     * Resets the list, as Esc does, and keeps the selected appointment, or else selects the next appointment from
     * now. From a range, removes the filter of {@code viewappt}: shows the weekly view of the selected appointment's
     * week, or of this week if none is selected, with the appointments of the default scope, and of all patients if
     * the selected appointment's patient is not in it. In the weekly view, goes back to the default scope. With
     * nothing to remove, nothing changes.
     */
    public void reset() {
        if (range == null && scope == PatientScope.DEFAULT) {
            return;
        }
        Optional<PatientAppointment> selected = page.getEntryAt(
                appointmentListView.getSelectionModel().getSelectedIndex());
        if (range == null) {
            scope = PatientScope.DEFAULT;
        } else {
            week = AppointmentWeek.of(selected.map(entry -> entry.getAppointment().getDate())
                    .orElse(LocalDate.now()));
            range = null;
            boolean isSelectedHidden = selected.filter(entry -> !PatientScope.DEFAULT.includes(entry.getPatient()))
                    .isPresent();
            scope = isSelectedHidden ? PatientScope.ALL : PatientScope.DEFAULT;
        }
        showPage();
        OptionalInt index = selected.map(page::indexOf).orElse(OptionalInt.empty());
        index.ifPresentOrElse(this::select, this::selectNextFromNow);
    }

    /**
     * Selects nothing, and so shows no appointment's details, as after a command that removes patients.
     */
    public void selectNothing() {
        clearSelection();
    }

    /**
     * Moves the keyboard focus to the list. The list takes the focus even on a page with no appointments, so
     * that Left and Right can still go to another week.
     */
    public void requestFocus() {
        appointmentListView.requestFocus();
    }

    /**
     * Returns true if the list has the keyboard focus.
     */
    public boolean isFocused() {
        return appointmentListView.isFocused();
    }

    /**
     * Returns the property that is true while the list has the keyboard focus.
     */
    public ReadOnlyBooleanProperty focusedProperty() {
        return appointmentListView.focusedProperty();
    }

    /**
     * Selects the appointment to start from, if no appointment is selected: on a page that spans today, the
     * first one that has not passed, and on another page, its first one.
     */
    public void selectDefaultIfNoneSelected() {
        if (appointmentListView.getSelectionModel().isEmpty()) {
            page.defaultEntryIndex().ifPresent(this::select);
        }
    }

    @FXML
    private void handlePreviousWeek() {
        showWeekAndSelectFirst(week.previous());
    }

    @FXML
    private void handleNextWeek() {
        showWeekAndSelectFirst(week.next());
    }

    /**
     * Handles the keys that move between appointments and weeks, so that a heading is never selected.
     * Ctrl+Up and Ctrl+Down (Cmd on macOS) go to the first and last appointment of the page. Page Up and Page Down
     * move about one screen, as they do in the patient list, to the nearest appointment. Left and Right change the
     * week only in the weekly view. Space, which would select whichever row the list last focused,
     * does nothing. Other keys keep their usual effect.
     */
    private void handleKeyPressed(KeyEvent event) {
        if (event.isAltDown() || (event.isControlDown() || event.isMetaDown()) && !event.isShortcutDown()) {
            return;
        }
        int selected = appointmentListView.getSelectionModel().getSelectedIndex();
        if (event.isShortcutDown()) {
            switch (event.getCode()) {
                case UP, KP_UP -> page.firstEntryIndex().ifPresent(this::select);
                case DOWN, KP_DOWN -> page.lastEntryIndex().ifPresent(this::select);
                default -> {
                    return;
                }
            }
            event.consume();
            return;
        }
        switch (event.getCode()) {
            case UP, KP_UP -> page.entryIndexBefore(selected).ifPresentOrElse(this::select, this::leaveTop);
            case DOWN, KP_DOWN -> (selected < 0 ? page.firstEntryIndex() : page.entryIndexAfter(selected))
                    .ifPresent(this::select);
            case HOME -> page.firstEntryIndex().ifPresent(this::select);
            case END -> page.lastEntryIndex().ifPresent(this::select);
            case PAGE_UP -> page.entryIndexRowsBefore(selected, getRowsPerScreen()).ifPresent(this::select);
            case PAGE_DOWN -> (selected < 0 ? page.firstEntryIndex()
                    : page.entryIndexRowsAfter(selected, getRowsPerScreen())).ifPresent(this::select);
            case LEFT, KP_LEFT -> {
                if (range == null) {
                    handlePreviousWeek();
                }
            }
            case RIGHT, KP_RIGHT -> {
                if (range == null) {
                    handleNextWeek();
                }
            }
            case SPACE -> {
                // Consumed below, so that the list does not select a heading
            }
            default -> {
                return;
            }
        }
        event.consume();
    }

    /**
     * Returns how many rows Page Up and Page Down move: all the rows shown but one, so that one row stays in view.
     */
    private int getRowsPerScreen() {
        if (!(appointmentListView.lookup(".virtual-flow") instanceof VirtualFlow<?> flow)) {
            return 1;
        }
        IndexedCell<?> first = flow.getFirstVisibleCell();
        IndexedCell<?> last = flow.getLastVisibleCell();
        return first == null || last == null ? 1 : Math.max(last.getIndex() - first.getIndex(), 1);
    }

    /**
     * Shows the top of the page, such as the days before its first appointment, and lets the focus move on,
     * as Up does at the first appointment.
     */
    private void leaveTop() {
        appointmentListView.scrollTo(0);
        onUpAtTop.run();
    }

    private void showWeekAndSelectFirst(AppointmentWeek newWeek) {
        if (newWeek.equals(week)) {
            // The first or last week has no week beyond it, so the selection stays
            return;
        }
        week = newWeek;
        showPage();
        page.firstEntryIndex().ifPresentOrElse(this::select, this::clearSelection);
    }

    /**
     * Builds the page that is shown, the range or the week, and its header. Selects nothing.
     */
    private void showPage() {
        pageTime = LocalDateTime.now();
        LocalDate today = pageTime.toLocalDate();
        // A range narrows the page to the appointments it contains, of its patient, if it has one
        List<PatientAppointment> pageAppointments = allAppointments.stream()
                .filter(entry -> scope.includes(entry.getPatient()))
                .filter(entry -> range == null || range.contains(entry))
                .toList();
        page = range == null
                ? AppointmentPage.ofWeek(week, pageAppointments, pageTime, settings.get())
                : AppointmentPage.ofRange(range.start(), range.end(), pageAppointments, pageTime, settings.get());
        appointmentListView.getSelectionModel().clearSelection();
        rows.setAll(page.getRows());
        appointmentListView.scrollTo(0);

        // The arrows change the week, so they show only in the weekly view, and only if there is a week to go to
        setShown(previousWeekButton, range == null && !week.previous().equals(week));
        setShown(nextWeekButton, range == null && !week.next().equals(week));
        weekRelative.getStyleClass().remove(THIS_WEEK_STYLE_CLASS);
        if (range == null) {
            weekDates.setText(week.describeDates());
            weekRelative.setText(week.describeRelativeTo(today).orElse(""));
            if (week.contains(today)) {
                weekRelative.getStyleClass().add(THIS_WEEK_STYLE_CLASS);
            }
        } else {
            // A range of one patient's appointments starts with their ID, so that it is not mistaken for everyone's
            weekDates.setText((range.patientId() == null ? "" : range.patientId().value + ", ")
                    + AppointmentFormatter.formatRange(range.start(), range.end()));
            weekRelative.setText("");
        }
        int archivedCount = (int) page.getRows().stream()
                .filter(AppointmentPage.Entry.class::isInstance)
                .map(AppointmentPage.Entry.class::cast)
                .filter(entry -> entry.appointment().getPatient().isArchived())
                .count();
        weekCount.setText(describeCount(page.getEntryCount() - archivedCount, archivedCount, scope));
    }

    /**
     * Rebuilds the page as of now, after appointments change, an appointment passes or a new day begins. Keeps the
     * selected appointment if it is still there, even if its patient's other details changed.
     */
    public void refresh() {
        Optional<PatientAppointment> selected = page.getEntryAt(
                appointmentListView.getSelectionModel().getSelectedIndex());
        showPage();
        OptionalInt index = selected.map(page::indexOf).orElse(OptionalInt.empty());
        if (index.isPresent()) {
            select(index.getAsInt());
        } else if (selected.isPresent()) {
            clearSelection();
        }
    }

    private void select(int index) {
        appointmentListView.getSelectionModel().select(index);
        appointmentListView.getFocusModel().focus(index);
        scrollIntoView(index);
        chooseSelected();
    }

    private void clearSelection() {
        appointmentListView.getSelectionModel().clearSelection();
        onAppointmentChosen.accept(Optional.empty());
    }

    private void chooseSelected() {
        page.getEntryAt(appointmentListView.getSelectionModel().getSelectedIndex())
                .ifPresent(entry -> onAppointmentChosen.accept(Optional.of(entry)));
    }

    /**
     * Scrolls the list, if the row at {@code index} is not fully shown, so that its day's heading shows above
     * it. If the day has too many appointments for both to fit, scrolls only as far as the row needs.
     */
    private void scrollIntoView(int index) {
        if (!(appointmentListView.lookup(".virtual-flow") instanceof VirtualFlow<?> flow)) {
            // The list has not been shown yet, so it has no rows to scroll
            appointmentListView.scrollTo(page.headingIndexOf(index));
            return;
        }
        // A command can show this view and its page at once, so lay the window out first, or the list would
        // scroll by the size that it had while hidden
        layOutWindow();
        IndexedCell<?> first = flow.getFirstVisibleCell();
        IndexedCell<?> last = flow.getLastVisibleCell();
        if (first == null || last == null) {
            appointmentListView.scrollTo(page.headingIndexOf(index));
            return;
        }
        if (isFullyShown(flow, index)) {
            return;
        }
        int heading = page.headingIndexOf(index);
        int shownRows = last.getIndex() - first.getIndex();
        if (index - heading < shownRows) {
            appointmentListView.scrollTo(heading);
            layOutWindow();
        }
        // From just out of view, VirtualFlow scrolls a row in by its own height, which leaves it partly hidden
        // when the row before it was too, so a second call scrolls the rest of the way
        flow.scrollTo(index);
        layOutWindow();
        flow.scrollTo(index);
    }

    /**
     * Returns true if the row at {@code index} is fully inside the visible part of the list.
     */
    private static boolean isFullyShown(VirtualFlow<?> flow, int index) {
        IndexedCell<?> cell = flow.getVisibleCell(index);
        return cell != null && cell.getLayoutY() >= 0 && cell.getLayoutY() + cell.getHeight() <= flow.getHeight();
    }

    /**
     * Lays the window out now, so that the list's rows are where they will be drawn.
     */
    private void layOutWindow() {
        Parent windowRoot = appointmentListView.getScene() == null ? null : appointmentListView.getScene().getRoot();
        if (windowRoot != null) {
            windowRoot.applyCss();
            windowRoot.layout();
        }
    }

    /**
     * Returns the count at the end of the header, in the same form as the patient list's, with no words, so that it
     * fits beside a long range: the appointments of patients who are not archived, such as "49", and in brackets
     * those of archived patients, such as "(2)". The default scope shows only the first, and
     * {@link PatientScope#ARCHIVED} only the second.
     */
    static String describeCount(int unarchivedCount, int archivedCount, PatientScope scope) {
        String unarchived = String.valueOf(unarchivedCount);
        String archived = "(" + archivedCount + ")";
        return switch (scope) {
            case UNARCHIVED -> unarchived;
            case ALL -> unarchived + " " + archived;
            case ARCHIVED -> archived;
        };
    }

    /**
     * A {@code ListCell} that shows a day's heading or one appointment. Headings ignore the mouse, so that only
     * appointments can be selected.
     */
    private class RowCell extends ListCell<AppointmentPage.Row> {
        // One heading and one appointment row for the cell's life, since building them for each row scrolled into
        // view, and loading the appointment row's FXML, makes scrolling stutter
        private final Label day = new Label();
        private final Label count = new Label();
        private final HBox headingBox;
        private final AppointmentRow entryRow = new AppointmentRow();

        RowCell() {
            // Sizes the cell to the list instead of its content, so the list never scrolls sideways.
            setPrefWidth(0);
            paddingProperty().bind(ListHeaders.cellPadding(rowGutter));
            count.getStyleClass().add("appointment-day-count");
            count.setMinWidth(Region.USE_PREF_SIZE);
            Region filler = new Region();
            HBox.setHgrow(filler, Priority.ALWAYS);
            headingBox = new HBox(day, filler, count);
            headingBox.getStyleClass().add("appointment-day-box");
        }

        @Override
        protected void updateItem(AppointmentPage.Row row, boolean empty) {
            super.updateItem(row, empty);
            getStyleClass().removeAll(DAY_CELL_STYLE_CLASS, ENTRY_CELL_STYLE_CLASS);
            setMouseTransparent(false);
            setText(null);

            if (empty || row == null) {
                setGraphic(null);
            } else if (row instanceof AppointmentPage.DayHeading heading) {
                getStyleClass().add(DAY_CELL_STYLE_CLASS);
                setMouseTransparent(true);
                setGraphic(showHeading(heading));
            } else if (row instanceof AppointmentPage.Entry entry) {
                getStyleClass().add(ENTRY_CELL_STYLE_CLASS);
                setGraphic(showEntry(entry));
            }
        }

        private Node showHeading(AppointmentPage.DayHeading heading) {
            LocalDate today = pageTime.toLocalDate();
            day.setText(AppointmentFormatter.formatDayHeading(heading.date(), today));
            day.getStyleClass().setAll("label",
                    heading.date().equals(today) ? "appointment-day-today" : "appointment-day");
            count.setText(heading.appointmentCount() == 0 ? "None" : String.valueOf(heading.appointmentCount()));
            return headingBox;
        }

        private Node showEntry(AppointmentPage.Entry entry) {
            boolean isClashing = !AppointmentClashes.find(allAppointments, entry.appointment(), settings.get(),
                    pageTime).isEmpty();
            entryRow.show(entry, pageTime, isClashing, settings.get());
            return entryRow.getRoot();
        }
    }
}
