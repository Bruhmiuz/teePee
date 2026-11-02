package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.logic.Messages.MESSAGE_PATIENT_NOT_FOUND;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ALL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ARCHIVED;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FROM;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ID;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TO;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult.Navigation;
import seedu.address.logic.commands.CommandResult.View;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.PatientScope;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentBetweenDatesPredicate;
import seedu.address.model.patient.Patient;
import seedu.address.model.patient.PatientId;

/**
 * Views the appointments on a date, or between two dates (inclusive), in the appointment view, and counts the
 * patients who have one. With {@code --id}, only that patient's appointments are shown. Without a date, it shows
 * the weekly view of this week, or with {@code --id}, every appointment of that patient. In each case, the next
 * appointment from now is selected. The patient list is left as it is.
 * <p>
 * Whether archived patients' appointments show is part of the same filter, as the dates and {@code --id} are: the
 * patients in the scope that the command gives, or the default scope. With {@code --id}, that patient's appointments
 * show even if they are archived: the scope is the one that has the patient, so that the header counts the patient's
 * appointments as it would count them among everyone's, such as "3", or "(3)" for an archived patient.
 */
public class ViewAppointmentCommand extends Command {

    public static final String COMMAND_WORD = "viewappt";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Shows the appointments on a date, or within a date "
            + "range, in the appointment view. The patient list stays as it is. Without a date "
            + "or range, shows this week, or with " + PREFIX_ID + "alone, all of that patient's appointments. "
            + "A DATE must come before other options.\n"
            + "Shows the appointments of patients who are not archived, or, with " + PREFIX_ALL.toString().trim()
            + ", of all patients, or, with " + PREFIX_ARCHIVED.toString().trim()
            + ", only of archived ones. An ID shows that patient's, archived or not.\n"
            + "Parameters: [DATE] or [" + PREFIX_FROM + "START_DATE] [" + PREFIX_TO + "END_DATE], with an optional ["
            + PREFIX_ID + "ID] or an optional [" + PREFIX_ALL.toString().trim() + " or "
            + PREFIX_ARCHIVED.toString().trim() + "] (dates in yyyy-MM-dd)\n"
            + "Example: " + COMMAND_WORD + " 2026-10-15";

    public static final String MESSAGE_VIEW_APPOINTMENT_LISTED_OVERVIEW = "%1$s with appointments on %2$s";
    public static final String MESSAGE_VIEW_APPOINTMENT_RANGE_LISTED_OVERVIEW =
            "%1$s with appointments from %2$s to %3$s";
    public static final String MESSAGE_VIEW_APPOINTMENT_FROM_LISTED_OVERVIEW =
            "%1$s with appointments on or after %2$s";
    public static final String MESSAGE_VIEW_APPOINTMENT_THIS_WEEK =
            "Showing this week's appointments of patients who are not archived.";
    public static final String MESSAGE_VIEW_APPOINTMENT_THIS_WEEK_WITH_ARCHIVED =
            "Showing this week's appointments, with archived patients' ones.";
    public static final String MESSAGE_VIEW_APPOINTMENT_THIS_WEEK_ARCHIVED =
            "Showing this week's appointments of archived patients.";
    public static final String MESSAGE_VIEW_APPOINTMENT_PATIENT_ALL_LISTED_OVERVIEW = "%1$s of %2$s";
    public static final String MESSAGE_VIEW_APPOINTMENT_PATIENT_LISTED_OVERVIEW =
            "%1$s of %2$s on %3$s";
    public static final String MESSAGE_VIEW_APPOINTMENT_PATIENT_RANGE_LISTED_OVERVIEW =
            "%1$s of %2$s from %3$s to %4$s";
    public static final String MESSAGE_VIEW_APPOINTMENT_PATIENT_FROM_LISTED_OVERVIEW =
            "%1$s of %2$s on or after %3$s";
    public static final String MESSAGE_VIEW_APPOINTMENT_TO_BEFORE_TODAY =
            "The " + PREFIX_TO + "date %1$s is before today (%2$s). "
                    + "To see past appointments, give " + PREFIX_FROM + "as well.";
    public static final String MESSAGE_VIEW_APPOINTMENT_FROM_AFTER_TO =
            "The " + PREFIX_FROM + "date must not be after the " + PREFIX_TO + "date.";
    public static final String MESSAGE_VIEW_APPOINTMENT_DATE_AFTER_OPTIONS =
            "A DATE must come before other options.\n"
                    + "Example: " + COMMAND_WORD + " 2026-10-15 " + PREFIX_ID + "S1234567A";
    public static final String MESSAGE_VIEW_APPOINTMENT_TWO_DATES_IN_ONE_OPTION =
            "Each date option takes only one date.\n"
                    + "Example: " + COMMAND_WORD + " " + PREFIX_FROM + "2026-10-01 " + PREFIX_TO + "2026-10-05";

    private static final Logger logger = LogsCenter.getLogger(ViewAppointmentCommand.class);

    private final Optional<LocalDate> startDate;
    private final Optional<LocalDate> endDate;
    private final Optional<PatientId> patientId; // narrows the appointments to one patient
    private final PatientScope scope; // whose appointments show, unless patientId names one patient
    private final Clock clock; // gives today's date when no start date is given

    /**
     * Creates a ViewAppointmentCommand to view appointments on a single date.
     */
    public ViewAppointmentCommand(LocalDate date) {
        this(Optional.of(date), Optional.of(date), Optional.empty());
    }

    /**
     * Creates a ViewAppointmentCommand to view appointments between two dates (inclusive).
     *
     * @param startDate First date of the range, or {@code Optional#empty()} to start on the day the command runs.
     * @param endDate Last date of the range, or {@code Optional#empty()} for a range with no end.
     * @throws IllegalArgumentException if both dates are given and {@code startDate} is after {@code endDate}.
     */
    public ViewAppointmentCommand(Optional<LocalDate> startDate, Optional<LocalDate> endDate) {
        this(startDate, endDate, Optional.empty());
    }

    /**
     * Creates a ViewAppointmentCommand to view one patient's appointments between two dates (inclusive).
     *
     * @param patientId The patient whose appointments are shown, or {@code Optional#empty()} for every patient's.
     */
    public ViewAppointmentCommand(Optional<LocalDate> startDate, Optional<LocalDate> endDate,
            Optional<PatientId> patientId) {
        this(startDate, endDate, patientId, PatientScope.DEFAULT);
    }

    /**
     * Creates a ViewAppointmentCommand to view the appointments of the patients in {@code scope} between two dates
     * (inclusive), or of one patient, archived or not.
     *
     * @param scope Whose appointments are shown if {@code patientId} is empty.
     */
    public ViewAppointmentCommand(Optional<LocalDate> startDate, Optional<LocalDate> endDate,
            Optional<PatientId> patientId, PatientScope scope) {
        this(startDate, endDate, patientId, scope, Clock.systemDefaultZone());
    }

    /**
     * Creates a ViewAppointmentCommand that reads today's date from {@code clock}, so that tests can fix the date.
     */
    ViewAppointmentCommand(Optional<LocalDate> startDate, Optional<LocalDate> endDate, Clock clock) {
        this(startDate, endDate, Optional.empty(), PatientScope.DEFAULT, clock);
    }

    /**
     * Creates a ViewAppointmentCommand that reads today's date from {@code clock}, so that tests can fix the date.
     */
    ViewAppointmentCommand(Optional<LocalDate> startDate, Optional<LocalDate> endDate,
            Optional<PatientId> patientId, Clock clock) {
        this(startDate, endDate, patientId, PatientScope.DEFAULT, clock);
    }

    /**
     * Creates a ViewAppointmentCommand that reads today's date from {@code clock}, so that tests can fix the date.
     */
    ViewAppointmentCommand(Optional<LocalDate> startDate, Optional<LocalDate> endDate,
            Optional<PatientId> patientId, PatientScope scope, Clock clock) {
        requireAllNonNull(startDate, endDate, patientId, scope, clock);
        checkArgument(startDate.isEmpty() || endDate.isEmpty() || !startDate.get().isAfter(endDate.get()),
                MESSAGE_VIEW_APPOINTMENT_FROM_AFTER_TO);
        this.startDate = startDate;
        this.endDate = endDate;
        this.patientId = patientId;
        this.scope = scope;
        this.clock = clock;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        LocalDate today = LocalDate.now(clock);
        boolean hasDates = startDate.isPresent() || endDate.isPresent();
        if (!hasDates) {
            return patientId.isEmpty() ? showThisWeek() : showAllOfPatient(model, patientId.get(), today);
        }
        LocalDate start = startDate.orElse(today);
        LocalDate end = endDate.orElse(LocalDate.MAX);
        if (start.isAfter(end)) {
            // Only an end date without a start date can come before the start
            logger.fine("Rejected viewappt: the " + PREFIX_TO + " date " + end
                    + " is before today " + today + ".");
            throw new CommandException(String.format(MESSAGE_VIEW_APPOINTMENT_TO_BEFORE_TODAY, end, today));
        }
        assert !start.isAfter(end) : "The range must be in order once the end-before-today case is rejected.";

        AppointmentBetweenDatesPredicate inRange = new AppointmentBetweenDatesPredicate(start, end);
        if (patientId.isEmpty()) {
            long count = countPatients(model, inRange);
            logger.fine("Viewed appointments of " + count + " patient(s) from " + start + " to " + end + ".");
            return new CommandResult(describeAll(count, start, end), new CommandResult.AppointmentRange(start, end))
                    .withAppointmentScope(scope)
                    .withNavigation(Navigation.toView(View.APPOINTMENTS));
        }

        PatientId id = patientId.get();
        Patient patient = findPatientOrThrow(model, id);
        long count = patient.getSchedule().asList().stream()
                .filter(appointment -> inRange.isWithinRange(appointment.getDate()))
                .count();
        logger.fine("Viewed " + count + " appointment(s) of patient " + id + " from " + start + " to " + end + ".");
        return new CommandResult(describeForPatient(count, patient, start, end),
                new CommandResult.AppointmentRange(start, end, id))
                .withAppointmentScope(scopeOf(patient))
                .withNavigation(Navigation.toView(View.APPOINTMENTS));
    }

    /**
     * Shows the weekly view, which lists every appointment of the patients in the scope, a week at a time, starting
     * from this week. The result has no range, so the appointment view shows the weekly view, not a page of a range.
     * The message says which appointments are listed, as {@code view} says which patients are, since the header of
     * the view counts them.
     */
    private CommandResult showThisWeek() {
        String message = switch (scope) {
            case UNARCHIVED -> MESSAGE_VIEW_APPOINTMENT_THIS_WEEK;
            case ALL -> MESSAGE_VIEW_APPOINTMENT_THIS_WEEK_WITH_ARCHIVED;
            case ARCHIVED -> MESSAGE_VIEW_APPOINTMENT_THIS_WEEK_ARCHIVED;
        };
        logger.fine("Viewed this week's appointments.");
        return new CommandResult(message)
                .withAppointmentScope(scope)
                .withNavigation(Navigation.toView(View.APPOINTMENTS));
    }

    /**
     * Shows every appointment of the patient with {@code id}, past and upcoming, on one page. A patient with none
     * gets an empty page of today.
     */
    private CommandResult showAllOfPatient(Model model, PatientId id, LocalDate today) throws CommandException {
        Patient patient = findPatientOrThrow(model, id);
        List<Appointment> appointments = patient.getSchedule().asList(); // in time order
        LocalDate first = appointments.isEmpty() ? today : appointments.get(0).getDate();
        LocalDate last = appointments.isEmpty() ? today : appointments.get(appointments.size() - 1).getDate();
        assert appointments.isEmpty() || !last.isBefore(first)
                : "The schedule lists appointments in time order, so the last date is not before the first.";
        logger.fine("Viewed all " + appointments.size() + " appointment(s) of patient " + id + ".");
        return new CommandResult(String.format(MESSAGE_VIEW_APPOINTMENT_PATIENT_ALL_LISTED_OVERVIEW,
                Messages.formatCount(appointments.size(), "appointment"), patient.getName().fullName),
                new CommandResult.AppointmentRange(first, last, id))
                .withAppointmentScope(scopeOf(patient))
                .withNavigation(Navigation.toView(View.APPOINTMENTS));
    }

    /**
     * Returns the patient with {@code id} in {@code model}, or reports that no patient has it.
     */
    private static Patient findPatientOrThrow(Model model, PatientId id) throws CommandException {
        return model.findPatientById(id)
                .orElseThrow(() -> {
                    logger.fine("Rejected viewappt: no patient has the given ID.");
                    return new CommandException(String.format(MESSAGE_PATIENT_NOT_FOUND, id));
                });
    }

    /**
     * Returns the scope that has {@code patient}: archived patients only if they are archived, and otherwise the
     * patients who are not archived. It is not the default scope, which a setting may change (#216).
     */
    private static PatientScope scopeOf(Patient patient) {
        return patient.isArchived() ? PatientScope.ARCHIVED : PatientScope.UNARCHIVED;
    }

    /**
     * Returns how many patients in the command's scope {@code hasAppointmentShown} keeps, out of all patients, without
     * changing the patient list.
     */
    private long countPatients(Model model, Predicate<Patient> hasAppointmentShown) {
        return model.getLaiNurse().getPatientList().stream()
                .filter(candidate -> scope.includes(candidate) && hasAppointmentShown.test(candidate))
                .count();
    }

    /** Returns the status message for the appointments of every patient in the range. */
    private String describeAll(long count, LocalDate start, LocalDate end) {
        String patientCount = Messages.formatCount(count, "patient");
        if (endDate.isEmpty()) {
            return String.format(MESSAGE_VIEW_APPOINTMENT_FROM_LISTED_OVERVIEW, patientCount, start);
        }
        if (start.equals(end)) {
            return String.format(MESSAGE_VIEW_APPOINTMENT_LISTED_OVERVIEW, patientCount, start);
        }
        return String.format(MESSAGE_VIEW_APPOINTMENT_RANGE_LISTED_OVERVIEW, patientCount, start, end);
    }

    /** Returns the status message for the appointments of one patient in the range. */
    private String describeForPatient(long count, Patient patient, LocalDate start, LocalDate end) {
        String appointmentCount = Messages.formatCount(count, "appointment");
        String name = patient.getName().fullName;
        if (endDate.isEmpty()) {
            return String.format(MESSAGE_VIEW_APPOINTMENT_PATIENT_FROM_LISTED_OVERVIEW, appointmentCount, name, start);
        }
        if (start.equals(end)) {
            return String.format(MESSAGE_VIEW_APPOINTMENT_PATIENT_LISTED_OVERVIEW, appointmentCount, name, start);
        }
        return String.format(MESSAGE_VIEW_APPOINTMENT_PATIENT_RANGE_LISTED_OVERVIEW, appointmentCount, name, start,
                end);
    }

    @Override
    public Optional<CommandResult.View> getView() {
        return Optional.of(CommandResult.View.APPOINTMENTS);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ViewAppointmentCommand otherViewAppointmentCommand)) {
            return false;
        }

        // The clock is not compared, as it is not part of what the user asked for
        return startDate.equals(otherViewAppointmentCommand.startDate)
                && endDate.equals(otherViewAppointmentCommand.endDate)
                && patientId.equals(otherViewAppointmentCommand.patientId)
                && scope == otherViewAppointmentCommand.scope;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("startDate", startDate)
                .add("endDate", endDate)
                .add("patientId", patientId)
                .add("scope", scope)
                .toString();
    }
}
