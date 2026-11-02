package seedu.address.ui;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import seedu.address.model.Settings;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentSchedule;

/**
 * The appointments that the patient details list: every upcoming appointment, in time order, and every past
 * appointment, latest first. An appointment earlier today counts as past once it has passed, and one today with no
 * time counts as upcoming.
 * Guarantees: immutable.
 */
public final class DetailAppointments {

    private final List<Appointment> upcoming;
    private final List<Appointment> past;

    private DetailAppointments(List<Appointment> upcoming, List<Appointment> past) {
        this.upcoming = upcoming;
        this.past = past;
    }

    /**
     * Splits {@code schedule} into the appointments to list at {@code now}.
     */
    public static DetailAppointments of(AppointmentSchedule schedule, LocalDateTime now) {
        return of(schedule, now, Settings.DEFAULT);
    }

    /**
     * Splits {@code schedule}, as {@link #of(AppointmentSchedule, LocalDateTime)} does, with the appointments that
     * are over under {@code settings}, such as marked ones if they count as done (#216), listed as past.
     */
    public static DetailAppointments of(AppointmentSchedule schedule, LocalDateTime now, Settings settings) {
        requireAllNonNull(schedule, now, settings);
        List<Appointment> all = schedule.asList();
        List<Appointment> upcoming = all.stream()
                .filter(appointment -> !AppointmentFormatter.isOver(appointment, now, settings))
                .sorted()
                .toList();
        List<Appointment> past = all.stream()
                .filter(appointment -> AppointmentFormatter.isOver(appointment, now, settings))
                .sorted(Comparator.reverseOrder())
                .toList();
        return new DetailAppointments(upcoming, past);
    }

    /**
     * Returns the upcoming appointments, in time order.
     */
    public List<Appointment> getUpcoming() {
        return upcoming;
    }

    /**
     * Returns the past appointments, latest first.
     */
    public List<Appointment> getPast() {
        return past;
    }

    /**
     * Returns the note beside the "Appointments" heading, such as "3 upcoming" or "None upcoming".
     */
    public String describeUpcoming() {
        return upcoming.isEmpty() ? "None upcoming" : upcoming.size() + " upcoming";
    }

    /**
     * Returns the note beside the "Past" heading, such as "12, latest first".
     */
    public String describePast() {
        return past.size() + ", latest first";
    }
}
