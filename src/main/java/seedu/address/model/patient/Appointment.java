package seedu.address.model.patient;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import seedu.address.commons.util.DateUtil;
import seedu.address.commons.util.TextNormalizer;

/**
 * Represents a patient's appointment, identified within that patient by its number, which is its place in date order.
 * Date/time constructors create unnumbered inputs; a schedule numbers its appointments before exposing them, and
 * numbers them again whenever one is added, moved or removed.
 * Ordered by date, optional time, then number. Full equality also compares the date/time, details and completion.
 * Guarantees: immutable; is valid as declared in {@link #isValidAppointment(String)}
 */
public class Appointment implements Comparable<Appointment> {

    public static final String MESSAGE_CONSTRAINTS = "Appointments must be a real date in the format "
            + DateUtil.DATE_FORMAT + ", optionally followed by a 24-hour time in the format " + DateUtil.TIME_FORMAT
            + ", such as 2026-09-20 or 2026-09-20 14:00.";
    public static final String MESSAGE_NUMBER_CONSTRAINTS =
            "Appointment numbers must be positive whole numbers, such as 1 or 2.";
    public static final String MESSAGE_NUMBER_TOO_LARGE = "Appointment number %1$s is too large.";

    /** The order that {@link #compareTo} gives, built once rather than on every comparison. */
    private static final Comparator<Appointment> NATURAL_ORDER = Comparator.comparing(Appointment::getDate)
            .thenComparing(appointment -> appointment.time, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(appointment -> appointment.number, Comparator.nullsFirst(Comparator.naturalOrder()));

    /** The date, and the time if there is one, as in "2026-09-20" or "2026-09-20 14:00". */
    public final String value;
    private final LocalDate date;
    private final LocalTime time;
    private final AppointmentDetails details; // null if none
    private final Integer number; // null for an unnumbered input
    private final boolean done;

    /**
     * Constructs an {@code Appointment} with no details.
     *
     * @param appointment A valid date, or date and time.
     */
    public Appointment(String appointment) {
        this(appointment, Optional.empty());
    }

    /**
     * Constructs an {@code Appointment} with {@code details}, or with no details if it is empty.
     *
     * @param appointment A valid date, or date and time.
     * @param details The details of the appointment, if it has any.
     */
    public Appointment(String appointment, Optional<AppointmentDetails> details) {
        this(null, appointment, details);
    }

    /**
     * Constructs a numbered appointment. Numbers are unique within a patient's schedule.
     */
    public Appointment(int number, String appointment, Optional<AppointmentDetails> details) {
        this(Integer.valueOf(number), appointment, details);
    }

    private Appointment(Integer number, String appointment, Optional<AppointmentDetails> details) {
        this(number, appointment, details, false);
    }

    private Appointment(Integer number, String appointment, Optional<AppointmentDetails> details, boolean done) {
        requireNonNull(appointment);
        requireNonNull(details);
        checkArgument(number == null || number > 0, MESSAGE_NUMBER_CONSTRAINTS);
        checkArgument(isValidAppointment(appointment), MESSAGE_CONSTRAINTS);
        value = TextNormalizer.normalize(appointment);
        String[] parts = value.split(" ");
        date = DateUtil.parseDate(parts[0]);
        time = parts.length == 2 ? DateUtil.parseTime(parts[1]) : null;
        this.details = details.orElse(null);
        this.number = number;
        this.done = done;
    }

    /**
     * Returns whether this appointment has been assigned a number.
     */
    public boolean isNumbered() {
        return number != null;
    }

    /**
     * Returns this appointment's number.
     *
     * @throws IllegalStateException if this is an unnumbered input.
     */
    public int getNumber() {
        if (!isNumbered()) {
            throw new IllegalStateException("The appointment has not been assigned a number.");
        }
        return number;
    }

    /**
     * Assigns a number to an unnumbered input, or returns this appointment if it already has that number.
     */
    public Appointment withNumber(int number) {
        checkArgument(!isNumbered() || this.number == number, "An appointment's number cannot change.");
        return isNumbered() ? this : new Appointment(Integer.valueOf(number), value, getDetails(), done);
    }

    /**
     * Returns this appointment without its number, as an input that a schedule numbers.
     */
    public Appointment withoutNumber() {
        return isNumbered() ? new Appointment(null, value, getDetails(), done) : this;
    }

    /**
     * Returns true if this appointment has passed at {@code now}: it is on an earlier day, or earlier today at a time
     * before the minute of {@code now}. An appointment today with no time has not passed, as it can still be at any
     * time of the day.
     */
    public boolean hasPassed(LocalDateTime now) {
        requireNonNull(now);
        LocalDate today = now.toLocalDate();
        if (!date.equals(today)) {
            return date.isBefore(today);
        }
        LocalTime thisMinute = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        return time != null && time.isBefore(thisMinute);
    }

    /**
     * Returns whether this appointment has been marked as done, independently of its date and time.
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Returns a copy with the given completion status, preserving its number, date, time and details.
     */
    public Appointment withDone(boolean done) {
        return this.done == done ? this : new Appointment(number, value, getDetails(), done);
    }

    /**
     * Returns true if a given string is a valid date, or a valid date followed by a time.
     * Leading, trailing and repeated whitespace is ignored.
     */
    public static boolean isValidAppointment(String test) {
        String[] parts = TextNormalizer.normalize(test).split(" ");
        if (parts.length > 2) {
            return false;
        }
        try {
            DateUtil.parseDate(parts[0]);
            if (parts.length == 2) {
                DateUtil.parseTime(parts[1]);
            }
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Returns the date of the appointment.
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Returns the time of the appointment, or {@code Optional#empty()} if only a date was given.
     */
    public Optional<LocalTime> getTime() {
        return Optional.ofNullable(time);
    }

    /**
     * Returns the details of the appointment, or {@code Optional#empty()} if it has none.
     */
    public Optional<AppointmentDetails> getDetails() {
        return Optional.ofNullable(details);
    }

    /**
     * Returns true if both appointments have the same assigned number within their patient's schedule.
     */
    public boolean isSameAppointment(Appointment other) {
        return other != null && isNumbered() && Objects.equals(number, other.number);
    }

    /**
     * Matches date and optional time, independently of identity and details.
     */
    public boolean hasSameDateTime(Appointment other) {
        return other != null && date.equals(other.date) && Objects.equals(time, other.time);
    }

    /**
     * Compares appointments by date, then by time, then by number (unnumbered inputs first).
     * An appointment with only a date comes before any appointment with a time on the same day.
     * Details and completion are not compared, so this order is inconsistent with {@link #equals(Object)}.
     */
    @Override
    public int compareTo(Appointment other) {
        return NATURAL_ORDER.compare(this, other);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Appointment otherAppointment)) {
            return false;
        }

        return Objects.equals(number, otherAppointment.number) && value.equals(otherAppointment.value)
                && Objects.equals(details, otherAppointment.details) && done == otherAppointment.done;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, value, details, done);
    }

}
