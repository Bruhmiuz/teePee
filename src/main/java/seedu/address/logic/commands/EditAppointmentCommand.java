package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADD_DETAILS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_DATE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_DETAILS;

import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.CollectionUtil;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.AppointmentLookup.Target;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.CommandResult.View;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentDetails;
import seedu.address.model.patient.AppointmentSchedule;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientAppointment;
import seedu.address.model.patient.PatientId;

/**
 * Moves one of a patient's appointments to another date or time, or changes its details, or both.
 */
public class EditAppointmentCommand extends Command {

    public static final String COMMAND_WORD = "editappt";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Edits the appointment of the patient with the "
            + "given ID, using its number. Keeps its completion status. Moving it gives it the number of its new "
            + "place in date order. "
            + PREFIX_DETAILS.toString().trim() + " with no value clears the details, and "
            + PREFIX_ADD_DETAILS.toString().trim() + " adds text to the end of them, after \""
            + OptionalFieldEdit.APPEND_SEPARATOR + "\". The two cannot be given together.\n"
            + "Parameters: ID NUMBER "
            + "[" + PREFIX_DATE + "NEW_DATE_OR_DATETIME] "
            + "[" + PREFIX_DETAILS + "[DETAILS]] "
            + "[" + PREFIX_ADD_DETAILS + "MORE_DETAILS]\n"
            + "Example: " + COMMAND_WORD + " S1234567A 2 "
            + PREFIX_DATE + "2026-10-06 10:00 "
            + PREFIX_DETAILS + "Bring insulin pen";

    public static final String MESSAGE_EDIT_APPOINTMENT_SUCCESS = "Edited appointment %1$d of %2$s (%3$s): %4$s";
    public static final String MESSAGE_MOVED_FROM = " (moved from %1$s)";
    public static final String MESSAGE_NOT_EDITED = "At least one of " + PREFIX_DATE.toString().trim() + ", "
            + PREFIX_DETAILS.toString().trim() + " and " + PREFIX_ADD_DETAILS.toString().trim()
            + " must be provided.";

    private static final Logger logger = LogsCenter.getLogger(EditAppointmentCommand.class);

    private final PatientId targetId;
    private final Target targetAppointment;
    private final Optional<Appointment> newDateTime;
    private final Optional<OptionalFieldEdit<AppointmentDetails>> detailsEdit;

    /**
     * Creates a command selecting an appointment by its patient-local number.
     *
     * @param targetId The ID of the patient whose appointment to edit.
     * @param number The patient-local appointment number to edit.
     * @param newDateTime The date and time to move the appointment to, if it moves. Its details are ignored.
     * @param detailsEdit The change to the details of the appointment, if they change.
     */
    public EditAppointmentCommand(PatientId targetId, long number, Optional<Appointment> newDateTime,
            Optional<OptionalFieldEdit<AppointmentDetails>> detailsEdit) {
        CollectionUtil.requireAllNonNull(targetId, newDateTime, detailsEdit);
        if (newDateTime.isEmpty() && detailsEdit.isEmpty()) {
            throw new IllegalArgumentException(MESSAGE_NOT_EDITED);
        }
        this.targetId = targetId;
        this.targetAppointment = new Target(number);
        this.newDateTime = newDateTime;
        this.detailsEdit = detailsEdit;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Optional<Patient> foundPatient = model.findPatientById(targetId);
        if (foundPatient.isEmpty()) {
            logger.fine("Rejected editappt: no patient has the given ID.");
            throw new CommandException(String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, targetId));
        }
        Patient patient = foundPatient.get();
        AppointmentSchedule schedule = patient.getSchedule();
        Appointment oldAppointment =
                AppointmentLookup.find(targetId, schedule, targetAppointment, COMMAND_WORD);
        Appointment editedAppointment = createEditedAppointment(oldAppointment);

        boolean isMoved = !editedAppointment.hasSameDateTime(oldAppointment);

        // A moved appointment comes after the others at its new date and time, and the numbers change with it
        AppointmentSchedule editedSchedule = schedule.replace(oldAppointment, editedAppointment);
        editedAppointment = isMoved
                ? editedSchedule.getLastAt(editedAppointment)
                : editedSchedule.findByNumber(oldAppointment.getNumber()).orElseThrow();
        assert editedAppointment.isDone() == oldAppointment.isDone() : "An edit keeps the appointment's mark.";
        Patient editedPatient = patient.withSchedule(editedSchedule);
        model.setPatient(patient, editedPatient);

        String message = String.format(MESSAGE_EDIT_APPOINTMENT_SUCCESS, editedAppointment.getNumber(),
                editedPatient.getName(), targetId, Messages.format(editedAppointment));
        if (isMoved) {
            message += String.format(MESSAGE_MOVED_FROM, oldAppointment);
        }
        PatientAppointment editedEntry = new PatientAppointment(editedPatient, editedAppointment);
        String warning = Messages.formatClashWarning(model, editedEntry);
        message += warning + Messages.formatArchivedNote(editedPatient, model.getSettings().getValue());
        logger.fine("Edited appointment #" + editedAppointment.getNumber() + " of patient " + targetId
                + (warning.isEmpty() ? "." : " with a clash warning."));
        return new CommandResult(message).withNavigation(Navigation.toAppointment(editedEntry));
    }

    /**
     * Returns {@code oldAppointment} moved to the new date and time, if any, with its details edited, if they are.
     * What is not changed is kept from {@code oldAppointment}.
     */
    private Appointment createEditedAppointment(Appointment oldAppointment) {
        assert oldAppointment.isNumbered();
        String dateTime = newDateTime.map(Appointment::toString).orElse(oldAppointment.toString());
        Optional<AppointmentDetails> details = detailsEdit
                .map(edit -> edit.applyTo(oldAppointment.getDetails(), AppointmentDetails::new))
                .orElse(oldAppointment.getDetails());
        return new Appointment(oldAppointment.getNumber(), dateTime, details).withDone(oldAppointment.isDone());
    }

    @Override
    public Optional<View> getView() {
        return Optional.of(View.APPOINTMENTS);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof EditAppointmentCommand otherCommand)) {
            return false;
        }

        return targetId.equals(otherCommand.targetId)
                && targetAppointment.equals(otherCommand.targetAppointment)
                && newDateTime.equals(otherCommand.newDateTime)
                && detailsEdit.equals(otherCommand.detailsEdit);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetId", targetId)
                .add("targetAppointment", targetAppointment)
                .add("newDateTime", newDateTime)
                .add("detailsEdit", detailsEdit)
                .toString();
    }
}
