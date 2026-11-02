package seedu.address.ui;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Logic;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.CommandResult.AppointmentRange;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.CommandResult.View;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.model.patient.PatientId;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 * It shows one view at a time, patients or appointments, on two tabs.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    /** Marks the closed menu that the mouse is over, in place of {@code :hover}. */
    private static final PseudoClass HOVERED = PseudoClass.getPseudoClass("hovered");
    private static final String PATIENT_PLACEHOLDER =
            "To see a patient's details here, type view and their ID, for example: view S1234567A";
    private static final String APPOINTMENT_PLACEHOLDER =
            "To see a patient's details here, select one of their appointments.";
    private static final String PATIENT_PROMPT = "Type a command, or press Esc to browse patients";
    private static final String APPOINTMENT_PROMPT = "Type a command, or press Esc to browse appointments";
    /** The share of a view's width that its list takes in a small window. The details beside it take the rest. */
    private static final double SMALL_LIST_SHARE = 0.5;
    /** The share of a view's width that its list takes in a large window. */
    private static final double LARGE_LIST_SHARE = 0.28;
    /** The room in a view up to which its list takes {@link #SMALL_LIST_SHARE}: that in a window 1,000 pixels wide. */
    private static final double SMALL_VIEW_WIDTH = 950;
    /** The room in a view from which its list takes {@link #LARGE_LIST_SHARE}: that in a full HD window. */
    private static final double LARGE_VIEW_WIDTH = 1870;

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private Path dataFilePath;

    /** The view that is shown. */
    private View shownView = View.PATIENTS;

    // Independent Ui parts residing in this Ui container
    private PatientListPanel patientListPanel;
    private PatientDetailPanel patientDetailPanel;
    private AppointmentListPanel appointmentListPanel;
    private PatientDetailPanel appointmentDetailPanel;
    private CommandBox commandBox;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;
    private MinuteClock clock;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuBar menuBar;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private HBox viewTabs;

    @FXML
    private Button patientsTab;

    @FXML
    private Button appointmentsTab;

    @FXML
    private HBox patientView;

    @FXML
    private HBox appointmentView;

    @FXML
    private StackPane patientListPanelPlaceholder;

    @FXML
    private StackPane patientDetailPanelPlaceholder;

    @FXML
    private StackPane appointmentListPanelPlaceholder;

    @FXML
    private StackPane appointmentDetailPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.dataFilePath = dataFilePath;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();
        ignoreHoverOfClosedMenus(menuBar);

        helpWindow = new HelpWindow();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Keeps the mouse from selecting a menu of {@code menuBar} while none of its menus is open. JavaFX selects a menu
     * that the mouse is over, and while a menu is selected, the menu bar takes Left and Right to move between its
     * menus, also after the mouse has left, until a click elsewhere. A click still opens a menu, and an open menu
     * still follows the mouse to the next one. As the mouse-entered events stop here, the menu under the mouse gets
     * the {@code hovered} pseudo-class in place of {@code :hover}.
     */
    static void ignoreHoverOfClosedMenus(MenuBar menuBar) {
        menuBar.addEventFilter(MouseEvent.MOUSE_ENTERED_TARGET, event -> {
            boolean isAnyMenuShowing = menuBar.getMenus().stream().anyMatch(Menu::isShowing);
            if (event.getTarget() instanceof Node node && node != menuBar && !isAnyMenuShowing) {
                node.pseudoClassStateChanged(HOVERED, true);
                event.consume();
            }
        });
        menuBar.addEventFilter(MouseEvent.MOUSE_EXITED_TARGET, event -> {
            if (event.getTarget() instanceof Node node) {
                node.pseudoClassStateChanged(HOVERED, false);
            }
        });
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * the help window purposely so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        patientDetailPanel = new PatientDetailPanel(logic.getPatientList(), logic.getAppointmentList(),
                PATIENT_PLACEHOLDER);
        patientDetailPanelPlaceholder.getChildren().add(patientDetailPanel.getRoot());

        patientListPanel = new PatientListPanel(logic.getFilteredPatientList(), logic.getPatientList(),
                logic.getAppointmentList(), logic.getPatientListScope(), logic.isPatientListFiltered(),
                patientDetailPanel::show, this::focusViewTabs);
        patientListPanelPlaceholder.getChildren().add(patientListPanel.getRoot());
        patientListPanel.useSettings(logic.getSettings());
        patientDetailPanel.useSettings(logic.getSettings());

        appointmentDetailPanel = new PatientDetailPanel(logic.getPatientList(), logic.getAppointmentList(),
                APPOINTMENT_PLACEHOLDER);
        appointmentDetailPanelPlaceholder.getChildren().add(appointmentDetailPanel.getRoot());
        appointmentDetailPanel.useSettings(logic.getSettings());

        appointmentListPanel = new AppointmentListPanel(logic.getAppointmentList(), this::showAppointmentDetails,
                this::focusViewTabs);
        appointmentListPanelPlaceholder.getChildren().add(appointmentListPanel.getRoot());
        appointmentListPanel.useSettings(logic.getSettings());

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(dataFilePath);
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        commandBox = new CommandBox(this::executeCommand, logic::getCompletions, this::focusViewList,
                statusBarFooter::showCompletions);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());

        // The window sees these keys before the focused part does, so it can move the focus away from it.
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        getRoot().addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);

        viewTabs.addEventFilter(KeyEvent.KEY_PRESSED, this::handleTabKeyPressed);
        shareWidth(patientView, patientListPanelPlaceholder);
        shareWidth(appointmentView, appointmentListPanelPlaceholder);
        showView(View.PATIENTS);

        clock = new MinuteClock(this::handleMinuteChanged);
        clock.start();
    }

    /**
     * Shows the views again as of now if, since {@code previousMinute}, an appointment has passed or a new day has
     * begun. The lists and details decide what has passed, and what is today, only when they are drawn, so without
     * this they would go out of date while the window is left open.
     */
    private void handleMinuteChanged(LocalDateTime previousMinute, LocalDateTime minute) {
        if (!AppointmentFormatter.hasChangedBetween(logic.getAppointmentList(), previousMinute, minute)) {
            return;
        }
        logger.fine("Refreshing the views at " + minute + ", as an appointment has passed or a new day has begun");
        patientListPanel.refresh();
        patientDetailPanel.refresh();
        appointmentListPanel.refresh();
        appointmentDetailPanel.refresh();
    }

    /**
     * Shows {@code view} and hides the other view. Each view keeps its state while it is hidden.
     */
    private void showView(View view) {
        // A hidden part must not keep the focus, or the keys would go to it unseen
        boolean isFocusInHiddenView = view == View.PATIENTS
                ? appointmentListPanel.isFocused() || appointmentDetailPanel.isFocused()
                : patientListPanel.isFocused() || patientDetailPanel.isFocused();
        // A click on a tab takes no focus, so the focus moves with the view here
        boolean isFocusOnTabs = patientsTab.isFocused() || appointmentsTab.isFocused();
        shownView = view;
        setShown(patientView, view == View.PATIENTS);
        setShown(appointmentView, view == View.APPOINTMENTS);
        patientsTab.pseudoClassStateChanged(SELECTED, view == View.PATIENTS);
        appointmentsTab.pseudoClassStateChanged(SELECTED, view == View.APPOINTMENTS);
        commandBox.setPromptText(view == View.PATIENTS ? PATIENT_PROMPT : APPOINTMENT_PROMPT);
        if (isFocusInHiddenView || isFocusOnTabs) {
            getTab(view).requestFocus();
        }
    }

    @FXML
    private void handlePatientsTab() {
        showView(View.PATIENTS);
    }

    @FXML
    private void handleAppointmentsTab() {
        showView(View.APPOINTMENTS);
    }

    /**
     * Handles the arrow keys while the focus is on a tab. Left and Right switch the view and keep the focus on
     * the tab shown. Down moves the focus to the list below the tabs. Up does nothing, so that the focus stays
     * on the tabs.
     */
    private void handleTabKeyPressed(KeyEvent event) {
        if (event.isShiftDown() || event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
            return;
        }
        switch (event.getCode()) {
            case LEFT, KP_LEFT -> showView(View.PATIENTS);
            case RIGHT, KP_RIGHT -> showView(View.APPOINTMENTS);
            case DOWN, KP_DOWN -> requestFocus(FocusArea.LIST);
            case UP, KP_UP -> {
                // Consumed below, so that JavaFX does not move the focus to another part of the window
            }
            default -> {
                return;
            }
        }
        event.consume();
    }

    private Button getTab(View view) {
        return view == View.PATIENTS ? patientsTab : appointmentsTab;
    }

    /**
     * Selects the entry that {@code navigation} names in the list of its view, which {@link #showView} has shown,
     * or the entry to start from if it names none. {@code range} is the range of appointments that the command
     * showed, if any. {@code LogicManager} has checked that the entry is in the list.
     */
    private void navigate(Navigation navigation, Optional<AppointmentRange> range) {
        if (navigation.view() == View.PATIENTS) {
            Optional<PatientId> patient = navigation.patient().or(() -> logic.getFilteredPatientList().stream()
                    .findFirst().map(Patient::getId));
            // executeCommand then selects the shown patient in the list
            patient.ifPresentOrElse(patientDetailPanel::show, patientDetailPanel::clear);
            return;
        }
        if (range.isPresent()) {
            // showRange has shown the range and selected its first appointment, which a result with no navigation keeps
            navigation.appointment().ifPresentOrElse(appointmentListPanel::selectAppointment,
                    appointmentListPanel::selectNextFromNow);
            return;
        }
        navigation.appointment().ifPresentOrElse(appointmentListPanel::showAppointment,
                appointmentListPanel::showThisWeek);
    }

    private void showAppointmentDetails(Optional<PatientAppointment> appointment) {
        appointment.ifPresentOrElse(entry -> appointmentDetailPanel.show(entry.getPatient().getId(),
                Optional.of(entry.getAppointment())), appointmentDetailPanel::clear);
    }

    /**
     * Handles the keys that move the focus, reset the view or scroll the details. In the command box, only
     * Shift+Tab, and Shift+Up and Shift+Down, which scroll the result display there, are handled here, because the
     * command box uses the other keys itself, such as Tab to complete an ID.
     */
    private void handleKeyPressed(KeyEvent event) {
        boolean isShortcutDown = event.isControlDown() || event.isAltDown() || event.isMetaDown();
        Optional<BrowseKeys.Action> action = BrowseKeys.actionFor(event.getCode(), event.isShiftDown(),
                isShortcutDown);
        boolean isIgnored = action.equals(Optional.of(BrowseKeys.Action.IGNORE));
        boolean isScrollUp = action.equals(Optional.of(BrowseKeys.Action.SCROLL_DETAILS_UP));
        boolean isScrollDown = action.equals(Optional.of(BrowseKeys.Action.SCROLL_DETAILS_DOWN));
        if (commandBox.isFocused() && (isScrollUp || isScrollDown)) {
            resultDisplay.scroll(isScrollUp ? -1 : 1);
            event.consume();
            return;
        }
        if (action.isEmpty() || commandBox.isFocused() && !isIgnored) {
            return;
        }

        switch (action.get()) {
            case RETURN_TO_COMMAND_BOX -> commandBox.requestFocus();
            case RESET_VIEW -> resetView();
            case SWITCH_AREA -> switchFocusArea();
            case SCROLL_DETAILS_UP -> getShownDetailPanel().scroll(-1);
            case SCROLL_DETAILS_DOWN -> getShownDetailPanel().scroll(1);
            case IGNORE -> {
                // Consumed below, so that JavaFX does not move the focus on its own
            }
            default -> throw new AssertionError("Unknown action: " + action.get());
        }
        event.consume();
    }

    private PatientDetailPanel getShownDetailPanel() {
        return shownView == View.PATIENTS ? patientDetailPanel : appointmentDetailPanel;
    }

    /**
     * Moves the focus to the command box when the user types text outside it, and adds the text there.
     */
    private void handleKeyTyped(KeyEvent event) {
        boolean isShortcutDown = event.isControlDown() || event.isAltDown() || event.isMetaDown();
        if (commandBox.isFocused() || !BrowseKeys.isCommandText(event.getCharacter(), isShortcutDown)) {
            return;
        }
        commandBox.requestFocusAndType(event.getCharacter());
        event.consume();
    }

    /**
     * Resets the current view by one step, as Esc does, and keeps the selected entry so that it is seen among the
     * rest. In the patient view, removes the filter, such as that of {@code find}, or with no filter, goes back to
     * the default scope, as {@code view} does, but without running a command, so the details panel and the status
     * message stay as they are. In the appointment view, removes the filter of {@code viewappt}, as
     * {@link AppointmentListPanel#reset} says.
     */
    private void resetView() {
        if (shownView == View.APPOINTMENTS) {
            appointmentListPanel.reset();
            return;
        }
        logic.resetPatientList();
        // Showing more rows can move the list's selection, so match it to the patient in the details panel
        patientDetailPanel.getShownId().ifPresentOrElse(patientListPanel::select, patientListPanel::clearSelection);
    }

    /**
     * Moves the focus from the command box to the list of the view that is shown, as Esc does there, or to
     * the tabs if the list is empty.
     */
    private void focusViewList() {
        if (!requestFocus(FocusArea.LIST)) {
            requestFocus(FocusArea.VIEW_TABS);
        }
    }

    /**
     * Moves the focus to the tab of the view that is shown, as Up does at the top of a list.
     */
    private void focusViewTabs() {
        requestFocus(FocusArea.VIEW_TABS);
    }

    /**
     * Moves the focus between the tabs and the list of the view, as Tab does. If the list is empty, the focus
     * stays on the tabs.
     */
    private void switchFocusArea() {
        // The menu bar, the result display and the details are not areas, so Tab from them goes to the list
        FocusArea area = getFocusedArea().orElse(FocusArea.VIEW_TABS).afterTab();
        if (!requestFocus(area)) {
            requestFocus(FocusArea.VIEW_TABS);
        }
    }

    private Optional<FocusArea> getFocusedArea() {
        if (commandBox.isFocused()) {
            return Optional.of(FocusArea.COMMAND_BOX);
        } else if (patientsTab.isFocused() || appointmentsTab.isFocused()) {
            return Optional.of(FocusArea.VIEW_TABS);
        } else if (shownView == View.PATIENTS ? patientListPanel.isFocused() : appointmentListPanel.isFocused()) {
            return Optional.of(FocusArea.LIST);
        }
        return Optional.empty();
    }

    /**
     * Moves the focus to {@code area} of the view that is shown, and returns true if it took the focus.
     */
    private boolean requestFocus(FocusArea area) {
        return switch (area) {
            case COMMAND_BOX -> {
                commandBox.requestFocus();
                yield true;
            }
            case VIEW_TABS -> {
                getTab(shownView).requestFocus();
                yield true;
            }
            case LIST -> shownView == View.PATIENTS ? requestFocusOnPatientList() : requestFocusOnAppointmentList();
        };
    }

    /**
     * Moves the focus to the patient list. If no patient in the list is selected, selects the first one and
     * shows their details, so that the selected patient is always the one shown.
     */
    private boolean requestFocusOnPatientList() {
        if (!patientListPanel.requestFocus()) {
            return false;
        }
        patientListPanel.selectFirstIfNoneSelected().ifPresent(patientDetailPanel::show);
        return true;
    }

    /**
     * Moves the focus to the appointment list. If no appointment is selected, selects the one to start from,
     * which also shows its patient's details.
     */
    private boolean requestFocusOnAppointmentList() {
        appointmentListPanel.requestFocus();
        appointmentListPanel.selectDefaultIfNoneSelected();
        return true;
    }

    /**
     * Makes {@code list} take the width that {@link #listWidth} gives for the room in {@code view}, so that the list
     * and the details beside it grow and shrink together as the window is resized. The minimum widths of both still
     * hold.
     */
    private static void shareWidth(HBox view, Region list) {
        DoubleBinding room = view.widthProperty()
                .subtract(view.getPadding().getLeft() + view.getPadding().getRight() + view.getSpacing());
        list.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> listWidth(room.get()), room));
    }

    /**
     * Returns the width of a list in a view with {@code room}: {@link #SMALL_LIST_SHARE} of it in a small window, and
     * {@link #LARGE_LIST_SHARE} of it in a full HD or larger window. In between, the width grows steadily from the one
     * to the other, so that the list's share falls as the window grows, and a large window gives most of its extra
     * room to the details. The list never gets narrower as the window gets wider.
     */
    private static double listWidth(double room) {
        if (room <= SMALL_VIEW_WIDTH) {
            return room * SMALL_LIST_SHARE;
        }
        if (room >= LARGE_VIEW_WIDTH) {
            return room * LARGE_LIST_SHARE;
        }
        double smallWidth = SMALL_VIEW_WIDTH * SMALL_LIST_SHARE;
        double largeWidth = LARGE_VIEW_WIDTH * LARGE_LIST_SHARE;
        double progress = (room - SMALL_VIEW_WIDTH) / (LARGE_VIEW_WIDTH - SMALL_VIEW_WIDTH);
        return smallWidth + (largeWidth - smallWidth) * progress;
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(guiSettings.getWindowHeight());
        primaryStage.setWidth(guiSettings.getWindowWidth());
        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        clock.stop();
        helpWindow.hide();
        primaryStage.hide();
    }

    public PatientListPanel getPatientListPanel() {
        return patientListPanel;
    }

    /**
     * Shows {@code feedback} in the result display, as if a command had returned it.
     */
    void showFeedback(String feedback) {
        resultDisplay.setFeedbackToUser(feedback);
    }

    /**
     * Executes the command and returns the result.
     *
     * @see seedu.address.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());

            commandResult.getView().ifPresent(this::showView);
            commandResult.getAppointmentScope().ifPresent(appointmentListPanel::setScope);
            commandResult.getAppointmentRange().ifPresent(appointmentListPanel::showRange);

            commandResult.getNavigation().ifPresent(navigation ->
                    navigate(navigation, commandResult.getAppointmentRange()));
            if (commandResult.isSelectionCleared()) {
                patientDetailPanel.clear();
                appointmentListPanel.selectNothing();
            }
            // Removing or filtering rows can move the list's selection to another patient, so match it to the panel.
            patientDetailPanel.getShownId().ifPresentOrElse(patientListPanel::select, patientListPanel::clearSelection);

            if (commandResult.isShowHelp()) {
                handleHelp();
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        }
    }
}
