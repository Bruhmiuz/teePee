package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class AppointmentWeekTest {

    /** A Friday. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 18);

    @Test
    public void of_eachDayOfWeek_startsOnMondayAndEndsOnSunday() {
        for (LocalDate day = MONDAY; !day.isAfter(SUNDAY); day = day.plusDays(1)) {
            AppointmentWeek week = AppointmentWeek.of(day);
            assertEquals(MONDAY, week.getStart(), day.toString());
            assertEquals(SUNDAY, week.getEnd(), day.toString());
        }
        // a Sunday belongs to the week before it, and a Monday starts a new one
        assertEquals(MONDAY.plusWeeks(1), AppointmentWeek.of(SUNDAY.plusDays(1)).getStart());
    }

    @Test
    public void of_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> AppointmentWeek.of(null));
    }

    @Test
    public void getDays_returnsMondayToSunday() {
        List<LocalDate> days = AppointmentWeek.of(TODAY).getDays();
        assertEquals(7, days.size());
        assertEquals(MONDAY, days.get(0));
        assertEquals(TODAY, days.get(4));
        assertEquals(SUNDAY, days.get(6));
    }

    @Test
    public void contains_boundaries() {
        AppointmentWeek week = AppointmentWeek.of(TODAY);
        assertFalse(week.contains(MONDAY.minusDays(1)));
        assertTrue(week.contains(MONDAY));
        assertTrue(week.contains(SUNDAY));
        assertFalse(week.contains(SUNDAY.plusDays(1)));
    }

    @Test
    public void previousAndNext_moveByOneWeek() {
        AppointmentWeek week = AppointmentWeek.of(TODAY);
        assertEquals(MONDAY.minusWeeks(1), week.previous().getStart());
        assertEquals(MONDAY.plusWeeks(1), week.next().getStart());
        assertEquals(week, week.next().previous());
    }

    @Test
    public void firstWeek_hasOnlyDaysFromEarliestDate() {
        LocalDate saturday = LocalDate.of(0, 1, 1);
        LocalDate sunday = LocalDate.of(0, 1, 2);
        AppointmentWeek week = AppointmentWeek.of(sunday);

        assertEquals(saturday, week.getStart());
        assertEquals(sunday, week.getEnd());
        assertEquals(List.of(saturday, sunday), week.getDays());
        assertFalse(week.contains(saturday.minusDays(1)));
        assertEquals("Sat 1 – Sun 2 Jan 0000", week.describeDates());
        assertEquals(week, week.previous());
        assertEquals(LocalDate.of(0, 1, 3), week.next().getStart());
        assertEquals(week, week.next().previous());
    }

    @Test
    public void lastWeek_hasOnlyDaysUpToLatestDate() {
        LocalDate monday = LocalDate.of(9999, 12, 27);
        LocalDate friday = LocalDate.of(9999, 12, 31);
        AppointmentWeek week = AppointmentWeek.of(monday);

        assertEquals(monday, week.getStart());
        assertEquals(friday, week.getEnd());
        assertEquals(5, week.getDays().size());
        assertEquals(friday, week.getDays().get(4));
        assertFalse(week.contains(friday.plusDays(1)));
        assertEquals("Mon 27 – Fri 31 Dec 9999", week.describeDates());
        assertEquals(week, week.next());
        assertEquals(LocalDate.of(9999, 12, 20), week.previous().getStart());
        assertEquals(week, week.previous().next());
    }

    @Test
    public void describeDates_sameMonth_namesMonthOnce() {
        assertEquals("Mon 12 – Sun 18 Oct 2026", AppointmentWeek.of(TODAY).describeDates());
    }

    @Test
    public void describeDates_twoMonths_namesBothMonths() {
        assertEquals("Mon 28 Sep – Sun 4 Oct 2026",
                AppointmentWeek.of(LocalDate.of(2026, 10, 1)).describeDates());
    }

    @Test
    public void describeDates_twoYears_namesBothYears() {
        assertEquals("Mon 28 Dec 2026 – Sun 3 Jan 2027",
                AppointmentWeek.of(LocalDate.of(2027, 1, 1)).describeDates());
    }

    @Test
    public void describeRelativeTo_nearWeeks_namesThem() {
        AppointmentWeek thisWeek = AppointmentWeek.of(TODAY);
        assertEquals(Optional.of("This week"), thisWeek.describeRelativeTo(TODAY));
        assertEquals(Optional.of("Next week"), thisWeek.next().describeRelativeTo(TODAY));
        assertEquals(Optional.of("Last week"), thisWeek.previous().describeRelativeTo(TODAY));
        assertEquals(Optional.empty(), thisWeek.next().next().describeRelativeTo(TODAY));
        assertEquals(Optional.empty(), thisWeek.previous().previous().describeRelativeTo(TODAY));
        // on a Sunday, the next day is in next week
        assertEquals(Optional.of("Next week"), thisWeek.next().describeRelativeTo(SUNDAY));
    }

    @Test
    public void equals() {
        AppointmentWeek week = AppointmentWeek.of(TODAY);

        // same week from another day -> returns true
        assertTrue(week.equals(AppointmentWeek.of(MONDAY)));
        assertEquals(week.hashCode(), AppointmentWeek.of(SUNDAY).hashCode());

        // same object -> returns true
        assertTrue(week.equals(week));

        // null -> returns false
        assertFalse(week.equals(null));

        // different types -> returns false
        assertFalse(week.equals(MONDAY));

        // different week -> returns false
        assertNotEquals(week, week.next());
    }

    @Test
    public void toStringMethod() {
        assertEquals("Mon 12 – Sun 18 Oct 2026", AppointmentWeek.of(TODAY).toString());
    }
}
