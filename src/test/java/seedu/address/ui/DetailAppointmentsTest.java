package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import seedu.address.model.Settings;
import seedu.address.model.patient.Appointment;
import seedu.address.model.patient.AppointmentSchedule;

public class DetailAppointmentsTest {

    /** Midnight today, so that every appointment today is still to come. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 16, 0, 0);

    private static final Appointment FIRST_VISIT = new Appointment("2026-10-05 09:30").withNumber(1);
    private static final Appointment SECOND_VISIT = new Appointment("2026-10-07").withNumber(2);
    private static final Appointment THIRD_VISIT = new Appointment("2026-10-09 09:30").withNumber(3);
    private static final Appointment YESTERDAY = new Appointment("2026-10-15 16:00").withNumber(4);
    private static final Appointment TODAY_EARLY = new Appointment("2026-10-16 00:00").withNumber(5);
    private static final Appointment NEXT_WEEK = new Appointment("2026-10-23 09:30").withNumber(6);
    private static final Appointment NEXT_MONTH = new Appointment("2026-11-02").withNumber(7);

    @Test
    public void of_null_throwsNullPointerException() {
        AppointmentSchedule schedule = AppointmentSchedule.empty();
        assertThrows(NullPointerException.class, () -> DetailAppointments.of(null, NOW));
        assertThrows(NullPointerException.class, () -> DetailAppointments.of(schedule, null));
        assertThrows(NullPointerException.class, () -> DetailAppointments.of(schedule, NOW, null));
    }

    @Test
    public void of_noAppointments_nothingListed() {
        DetailAppointments appointments = DetailAppointments.of(AppointmentSchedule.empty(), NOW);
        assertEquals(List.of(), appointments.getUpcoming());
        assertEquals(List.of(), appointments.getPast());
        assertEquals("None upcoming", appointments.describeUpcoming());
    }

    @Test
    public void of_mixedAppointments_upcomingInTimeOrderAndPastLatestFirst() {
        DetailAppointments appointments = DetailAppointments.of(schedule(NEXT_MONTH, FIRST_VISIT, TODAY_EARLY,
                THIRD_VISIT, NEXT_WEEK, SECOND_VISIT, YESTERDAY), NOW);
        // an appointment earlier today still counts as upcoming, as only dates are compared
        assertEquals(List.of(TODAY_EARLY, NEXT_WEEK, NEXT_MONTH), appointments.getUpcoming());
        assertEquals(List.of(YESTERDAY, THIRD_VISIT, SECOND_VISIT, FIRST_VISIT), appointments.getPast());
        assertEquals("3 upcoming", appointments.describeUpcoming());
        assertEquals("4, latest first", appointments.describePast());
    }

    @Test
    public void of_markCountedAsDone_markedListedAsPast() {
        Appointment markedNextWeek = NEXT_WEEK.withDone(true);
        AppointmentSchedule schedule = schedule(markedNextWeek, NEXT_MONTH, YESTERDAY);
        // a mark counted as done (#216) makes an appointment past, wherever it is in time
        DetailAppointments appointments = DetailAppointments.of(schedule, NOW,
                Settings.DEFAULT.with(Settings.Key.MARK_COUNTS_AS_DONE, true));
        assertEquals(List.of(NEXT_MONTH.toString()), names(appointments.getUpcoming()));
        assertEquals(List.of(markedNextWeek.toString(), YESTERDAY.toString()), names(appointments.getPast()));
        // the default settings keep it upcoming
        assertEquals(List.of(markedNextWeek.toString(), NEXT_MONTH.toString()),
                names(DetailAppointments.of(schedule, NOW).getUpcoming()));
    }

    @Test
    public void of_manyPastAppointments_allListed() {
        List<Appointment> past = pastVisits(12);
        DetailAppointments appointments = DetailAppointments.of(new AppointmentSchedule(past), NOW);
        assertEquals(past.reversed(), appointments.getPast());
        assertEquals("12, latest first", appointments.describePast());
    }

    @Test
    public void of_fewPastAppointments_allListed() {
        DetailAppointments appointments = DetailAppointments.of(schedule(YESTERDAY, NEXT_WEEK), NOW);
        // the schedule renumbers its appointments, so compare their dates and times
        assertEquals(List.of(YESTERDAY.value), values(appointments.getPast()));
        assertEquals("1, latest first", appointments.describePast());
        assertEquals("1 upcoming", appointments.describeUpcoming());
    }

    @Test
    public void of_earlierToday_listedAsPast() {
        Appointment todayNoTime = new Appointment("2026-10-16").withNumber(8);
        Appointment todayLater = new Appointment("2026-10-16 14:00").withNumber(9);
        DetailAppointments appointments = DetailAppointments.of(schedule(YESTERDAY, TODAY_EARLY, todayNoTime,
                todayLater), LocalDateTime.of(2026, 10, 16, 10, 0));

        // an appointment today with no time has not passed
        assertEquals(List.of(todayNoTime.value, todayLater.value), values(appointments.getUpcoming()));
        assertEquals(List.of(TODAY_EARLY.value, YESTERDAY.value), values(appointments.getPast()));
    }

    private static List<String> values(List<Appointment> appointments) {
        return appointments.stream().map(appointment -> appointment.value).toList();
    }

    /** Returns the dates and times of {@code appointments}, as a schedule numbers its appointments anew. */
    private static List<String> names(List<Appointment> appointments) {
        return appointments.stream().map(Appointment::toString).toList();
    }

    private static AppointmentSchedule schedule(Appointment... appointments) {
        return new AppointmentSchedule(List.of(appointments));
    }

    /**
     * Returns {@code count} appointments at 09:30 on the days from 1 Sep 2026, oldest first, all before {@link #NOW}.
     */
    private static List<Appointment> pastVisits(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new Appointment(LocalDate.of(2026, 9, 1).plusDays(i) + " 09:30").withNumber(i + 1))
                .toList();
    }
}
