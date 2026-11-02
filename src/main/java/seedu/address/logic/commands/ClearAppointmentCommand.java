package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FROM;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TO;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentSchedule;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientId;

/**
 * Deletes many appointments at once (#217): every appointment, all of one patient's, or every one from one date, or
 * date and time, to another, of every patient or of one. An appointment with no time counts as at the start of its
 * day, a date without a time given to {@code --from} as the start of that day, and one given to {@code --to} as its
 * end.
 */
public class ClearAppointmentCommand extends Command implements Confirmable {

    public static final String COMMAND_WORD = "clearappt";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Deletes every appointment, or with an ID, all "
            + "appointments of that patient, or every appointment from one date, or date and time, to another, of "
            + "every patient, or with an ID, of one, after you confirm it.\n"
            + "Parameters: [ID] [" + PREFIX_FROM + "DATE_OR_DATETIME] [" + PREFIX_TO + "DATE_OR_DATETIME]\n"
            + "Example: " + COMMAND_WORD + " S1234567A, or " + COMMAND_WORD + " " + PREFIX_FROM + "2026-10-01 "
            + PREFIX_TO + "2026-10-31 12:00";

    public static final String MESSAGE_SUCCESS = "Deleted %1$s.";
    public static final String MESSAGE_DELETES = "This deletes %1$s of %2$s.";
    public static final String MESSAGE_DELETES_OF_PATIENT = "This deletes %1$s of %2$s (%3$s).";
    public static final String MESSAGE_NO_APPOINTMENTS = "No appointments match, so there is nothing to delete.";
    public static final String MESSAGE_FROM_AFTER_TO = "The " + PREFIX_FROM.toString().trim()
            + " date and time must not be after the " + PREFIX_TO.toString().trim() + " date and time.";

    private final Optional<PatientId> patientId; // empty for the appointments of every patient
    private final Optional<Appointment> from; // empty for no start
    private final Optional<Appointment> to; // empty for no end

    /**
     * Creates a ClearAppointmentCommand that deletes the appointments of the patient with {@code patientId}, or of
     * every patient if it is empty, from {@code from} to {@code to}, either of which may be empty for no limit. With
     * all three empty, it deletes every appointment.
     *
     * @throws IllegalArgumentException if {@code from} is after {@code to}.
     */
    public ClearAppointmentCommand(Optional<PatientId> patientId, Optional<Appointment> from,
            Optional<Appointment> to) {
        requireAllNonNull(patientId, from, to);
        if (from.isPresent() && to.isPresent() && startOf(from.get()).isAfter(endOf(to.get()))) {
            throw new IllegalArgumentException(MESSAGE_FROM_AFTER_TO);
        }
        this.patientId = patientId;
        this.from = from;
        this.to = to;
    }

    /**
     * Returns the start of the range that {@code from} gives: its time, or the start of its day.
     */
    public static LocalDateTime startOf(Appointment from) {
        return from.getDate().atTime(from.getTime().orElse(LocalTime.MIN));
    }

    /**
     * Returns the end of the range that {@code to} gives: its time, or the end of its day.
     */
    public static LocalDateTime endOf(Appointment to) {
        return to.getDate().atTime(to.getTime().orElse(LocalTime.MAX));
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        int count = 0;
        for (Patient patient : patientsToChange(model)) {
            List<Appointment> kept = patient.getSchedule().asList().stream()
                    .filter(appointment -> !isInRange(appointment)).toList();
            count += patient.getSchedule().asList().size() - kept.size();
            model.setPatient(patient, patient.withSchedule(new AppointmentSchedule(kept)));
        }
        return new CommandResult(String.format(MESSAGE_SUCCESS, Messages.formatCount(count, "appointment")))
                .withSelectionCleared();
    }

    @Override
    public String describeDeletion(Model model) throws CommandException {
        requireNonNull(model);
        List<Patient> patients = patientsToChange(model);
        long count = patients.stream()
                .mapToLong(patient -> patient.getSchedule().asList().stream().filter(this::isInRange).count())
                .sum();
        if (count == 0) {
            throw new CommandException(MESSAGE_NO_APPOINTMENTS);
        }
        if (patientId.isPresent()) {
            Patient patient = patients.get(0);
            return String.format(MESSAGE_DELETES_OF_PATIENT, Messages.formatCount(count, "appointment"),
                    patient.getName(), patient.getId());
        }
        return String.format(MESSAGE_DELETES, Messages.formatCount(count, "appointment"),
                Messages.formatCount(patients.size(), "patient"));
    }

    /**
     * Returns the patients who have an appointment to delete.
     *
     * @throws CommandException if no patient has the given ID.
     */
    private List<Patient> patientsToChange(Model model) throws CommandException {
        if (patientId.isPresent() && model.findPatientById(patientId.get()).isEmpty()) {
            throw new CommandException(String.format(Messages.MESSAGE_PATIENT_NOT_FOUND, patientId.get()));
        }
        return model.getLaiNurse().getPatientList().stream()
                .filter(patient -> patientId.map(patient.getId()::equals).orElse(true))
                .filter(patient -> patient.getSchedule().asList().stream().anyMatch(this::isInRange))
                .toList();
    }

    /**
     * Returns true if {@code appointment} is in the range, where one with no time counts as at the start of its day.
     */
    private boolean isInRange(Appointment appointment) {
        LocalDateTime at = startOf(appointment);
        return from.map(start -> !at.isBefore(startOf(start))).orElse(true)
                && to.map(end -> !at.isAfter(endOf(end))).orElse(true);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ClearAppointmentCommand otherCommand)) {
            return false;
        }

        return patientId.equals(otherCommand.patientId) && from.equals(otherCommand.from)
                && to.equals(otherCommand.to);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("patientId", patientId)
                .add("from", from)
                .add("to", to)
                .toString();
    }
}
