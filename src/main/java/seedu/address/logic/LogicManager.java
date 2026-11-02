package seedu.address.logic;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import seedu.address.commons.core.FeatureFlags;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.Confirmable;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.Completion;
import seedu.address.logic.parser.LaiNurseParser;
import seedu.address.logic.parser.TabCompleter;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.LaiNurse;
import seedu.address.model.Model;
import seedu.address.model.PatientScope;
import seedu.address.model.ReadOnlyLaiNurse;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.Settings;
import seedu.address.model.UserPrefs;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.model.patient.PatientId;
import seedu.address.storage.BackupFailedException;
import seedu.address.storage.Storage;

/**
 * The main LogicManager of the app.
 */
public class LogicManager implements Logic {
    public static final String FILE_OPS_ERROR_FORMAT = "Could not save data due to the following error: %s\n"
            + "Nothing was changed, so you can run the command again once LaiNurse can save.";

    public static final String FILE_OPS_PERMISSION_ERROR_FORMAT =
            "Could not save data to file %s due to insufficient permissions to write to the file or the folder.\n"
            + "Nothing was changed, so you can run the command again once LaiNurse can save.";

    public static final String PREFS_WARNING_FORMAT =
            "Warning: the settings are changed, but could not be saved: %s";

    public static final String BACKUP_WARNING_FORMAT =
            "Warning: the data is saved, but it could not be backed up to %s.";

    private final Logger logger = LogsCenter.getLogger(LogicManager.class);

    private final Model model;
    private final Storage storage;
    private final LaiNurseParser laiNurseParser;
    /** A command that deletes much at once, waiting for the user to confirm it, or null (#217). */
    private Command pendingCommand;

    /**
     * Constructs a {@code LogicManager} with the given {@code Model} and {@code Storage}.
     */
    public LogicManager(Model model, Storage storage) {
        this.model = model;
        this.storage = storage;
        laiNurseParser = new LaiNurseParser();
    }

    @Override
    public CommandResult execute(String commandText) throws CommandException, ParseException {
        logger.info("----------------[USER COMMAND][" + commandText + "]");

        CommandResult commandResult;
        Command command;
        if (pendingCommand != null) {
            // Only the phrase confirms the command; any other input cancels it, and runs no command
            command = pendingCommand;
            pendingCommand = null;
            if (!commandText.trim().equals(Messages.CONFIRMATION_PHRASE)) {
                return new CommandResult(Messages.MESSAGE_CANCELLED);
            }
        } else {
            command = laiNurseParser.parseCommand(commandText);
            if (FeatureFlags.isSelectiveClearEnabled() && command instanceof Confirmable confirmable) {
                String deletion = confirmable.describeDeletion(model);
                pendingCommand = command;
                return new CommandResult(String.format(Messages.MESSAGE_CONFIRM, deletion));
            }
        }
        // The data as it was, to put back if it cannot be saved, so that the command can be run again without its
        // change applying twice, such as text added twice to a patient's notes
        ReadOnlyLaiNurse dataBefore = new LaiNurse(model.getLaiNurse());
        UserPrefs userPrefsBefore = new UserPrefs(model.getUserPrefs());
        commandResult = command.execute(model);

        try {
            storage.saveLaiNurse(model.getLaiNurse());
        } catch (BackupFailedException e) {
            // The data file holds the change, so the change stays, and only the backup is out of date
            commandResult = commandResult.withNoteAppended(
                    String.format(BACKUP_WARNING_FORMAT, e.getBackupFilePath()));
        } catch (AccessDeniedException e) {
            restore(dataBefore, userPrefsBefore);
            throw new CommandException(String.format(FILE_OPS_PERMISSION_ERROR_FORMAT, e.getMessage()), e);
        } catch (IOException ioe) {
            restore(dataBefore, userPrefsBefore);
            throw new CommandException(String.format(FILE_OPS_ERROR_FORMAT, ioe.getMessage()), ioe);
        }

        // A command that changes the settings, such as set, saves them at once, not only when the app closes
        if (!userPrefsBefore.equals(model.getUserPrefs())) {
            try {
                storage.saveUserPrefs(model.getUserPrefs());
            } catch (IOException e) {
                commandResult = commandResult.withNoteAppended(String.format(PREFS_WARNING_FORMAT, e.getMessage()));
            }
        }

        // The view and the navigation are added and checked after saving, so that the data file still matches the
        // model if they show a bug in the command.
        // Each command declares the view it shows, so a command's own result never has to name it
        commandResult = command.getView().map(commandResult::withView).orElse(commandResult);
        checkNavigation(commandResult, model);
        return commandResult;
    }

    /**
     * Puts back {@code dataBefore} and the settings in {@code userPrefsBefore}, from before a command whose change
     * could not be saved. What the command did not change is left as it is, so that nothing refreshes for nothing.
     */
    private void restore(ReadOnlyLaiNurse dataBefore, ReadOnlyUserPrefs userPrefsBefore) {
        if (!dataBefore.equals(model.getLaiNurse())) {
            model.setLaiNurse(dataBefore);
            logger.info("Put back the data as it was before the command, as it could not be saved.");
        }
        if (!userPrefsBefore.getSettings().equals(model.getUserPrefs().getSettings())) {
            model.setSettings(userPrefsBefore.getSettings());
            logger.info("Put back the settings as they were before the command, as the data could not be saved.");
        }
    }

    /**
     * Checks that the entry that {@code result} navigates to, if any, is in the list that its view shows after the
     * command: a patient in the filtered patient list of {@code model}, or an appointment in its appointment list
     * and in the result's range, if it has one, which includes the range's patient.
     *
     * @throws IllegalStateException if the entry is not in that list, which is a bug in the command.
     */
    static void checkNavigation(CommandResult result, Model model) {
        Optional<Navigation> navigation = result.getNavigation();
        navigation.flatMap(Navigation::patient).ifPresent(id -> {
            if (model.getFilteredPatientList().stream().noneMatch(patient -> patient.getId().equals(id))) {
                throw new IllegalStateException("Patient " + id + " is not in the filtered patient list.");
            }
        });
        navigation.flatMap(Navigation::appointment).ifPresent(entry -> {
            if (model.getAppointmentList().stream().noneMatch(entry::isSameEntry)) {
                throw new IllegalStateException("Appointment " + entry + " is not in the appointment list.");
            }
            boolean isInRange = result.getAppointmentRange().map(range -> range.contains(entry)).orElse(true);
            if (!isInRange) {
                throw new IllegalStateException("Appointment " + entry + " is not in the range "
                        + result.getAppointmentRange().get() + ".");
            }
        });
    }

    @Override
    public ObservableList<Patient> getFilteredPatientList() {
        return model.getFilteredPatientList();
    }

    @Override
    public void resetPatientList() {
        if (model.hasPatientFilter()) {
            model.updateFilteredPatientList(Model.PREDICATE_SHOW_ALL_PATIENTS);
        } else {
            model.setPatientListScope(PatientScope.DEFAULT);
        }
    }

    @Override
    public ObservableValue<Settings> getSettings() {
        return model.getSettings();
    }

    @Override
    public ObservableValue<PatientScope> getPatientListScope() {
        return model.getPatientListScope();
    }

    @Override
    public ObservableValue<Boolean> isPatientListFiltered() {
        return model.isPatientListFiltered();
    }

    @Override
    public ObservableList<Patient> getPatientList() {
        return model.getLaiNurse().getPatientList();
    }

    @Override
    public ObservableList<PatientAppointment> getAppointmentList() {
        return model.getAppointmentList();
    }

    @Override
    public List<Completion> getCompletions(String commandText) {
        // The sort is stable, so archived patients come last, and in each group, the shown patients come first
        List<PatientId> ids = Stream.concat(getFilteredPatientList().stream(), getPatientList().stream())
                .distinct()
                .sorted(Comparator.comparing(Patient::isArchived))
                .map(Patient::getId)
                .toList();
        return TabCompleter.complete(commandText, ids);
    }

    @Override
    public GuiSettings getGuiSettings() {
        return model.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        model.setGuiSettings(guiSettings);
    }
}
