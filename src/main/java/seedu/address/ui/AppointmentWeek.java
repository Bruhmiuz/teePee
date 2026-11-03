package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import seedu.address.commons.util.DateUtil;

/**
 * One week of the appointment view, from Monday to Sunday, as {@link DateUtil#getStartOfWeek} gives. The weeks
 * stop at {@link DateUtil#EARLIEST_DATE} and {@link DateUtil#LATEST_DATE}, so the first and last weeks have only
 * the days from those dates on, or up to them.
 * Guarantees: immutable.
 */
public final class AppointmentWeek {

    private static final DateTimeFormatter WEEKDAY_DAY = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);
    private static final DateTimeFormatter WEEKDAY_DAY_MONTH =
            DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter WEEKDAY_DAY_MONTH_YEAR =
            DateTimeFormatter.ofPattern("EEE d MMM uuuu", Locale.ENGLISH);

    /** The Monday of this week, which can be before {@link DateUtil#EARLIEST_DATE}. */
    private final LocalDate monday;

    private AppointmentWeek(LocalDate monday) {
        this.monday = monday;
    }

    /**
     * Returns the week that contains {@code date}.
     */
    public static AppointmentWeek of(LocalDate date) {
        requireNonNull(date);
        return new AppointmentWeek(DateUtil.getStartOfWeek(date));
    }

    /**
     * Returns the first day of this week: its Monday, or {@link DateUtil#EARLIEST_DATE} in the first week.
     */
    public LocalDate getStart() {
        return monday.isBefore(DateUtil.EARLIEST_DATE) ? DateUtil.EARLIEST_DATE : monday;
    }

    /**
     * Returns the last day of this week: its Sunday, or {@link DateUtil#LATEST_DATE} in the last week.
     */
    public LocalDate getEnd() {
        LocalDate sunday = DateUtil.getEndOfWeek(monday);
        return sunday.isAfter(DateUtil.LATEST_DATE) ? DateUtil.LATEST_DATE : sunday;
    }

    /**
     * Returns the days of this week, in order: from Monday to Sunday, except in the first and last weeks.
     *
     * @throws IllegalArgumentException if this week is wholly before {@link DateUtil#EARLIEST_DATE} or after
     *     {@link DateUtil#LATEST_DATE}, which only a computer's clock outside those dates can cause.
     */
    public List<LocalDate> getDays() {
        return getStart().datesUntil(getEnd().plusDays(1)).toList();
    }

    /**
     * Returns true if {@code date} is in this week.
     */
    public boolean contains(LocalDate date) {
        requireNonNull(date);
        return !date.isBefore(getStart()) && !date.isAfter(getEnd());
    }

    /**
     * Returns the week before this one, or this week if it is the first.
     */
    public AppointmentWeek previous() {
        return getStart().equals(DateUtil.EARLIEST_DATE) ? this : new AppointmentWeek(monday.minusWeeks(1));
    }

    /**
     * Returns the week after this one, or this week if it is the last.
     */
    public AppointmentWeek next() {
        return getEnd().equals(DateUtil.LATEST_DATE) ? this : new AppointmentWeek(monday.plusWeeks(1));
    }

    /**
     * Returns the dates of this week, such as "Mon 12 – Sun 18 Oct 2026", "Mon 28 Sep – Sun 4 Oct 2026",
     * or "Mon 28 Dec 2026 – Sun 3 Jan 2027" for a week that spans two years.
     */
    public String describeDates() {
        LocalDate start = getStart();
        LocalDate end = getEnd();
        String endText = end.format(WEEKDAY_DAY_MONTH_YEAR);
        if (start.getYear() != end.getYear()) {
            return start.format(WEEKDAY_DAY_MONTH_YEAR) + " – " + endText;
        }
        if (start.getMonth() != end.getMonth()) {
            return start.format(WEEKDAY_DAY_MONTH) + " – " + endText;
        }
        return start.format(WEEKDAY_DAY) + " – " + endText;
    }

    /**
     * Returns where this week is relative to the week of {@code today}: "This week", "Next week" or "Last week".
     * Returns {@code Optional#empty()} for any other week.
     */
    public Optional<String> describeRelativeTo(LocalDate today) {
        AppointmentWeek thisWeek = of(today);
        if (equals(thisWeek)) {
            return Optional.of("This week");
        }
        if (equals(thisWeek.next())) {
            return Optional.of("Next week");
        }
        if (equals(thisWeek.previous())) {
            return Optional.of("Last week");
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AppointmentWeek otherWeek)) {
            return false;
        }

        return monday.equals(otherWeek.monday);
    }

    @Override
    public int hashCode() {
        return monday.hashCode();
    }

    @Override
    public String toString() {
        return describeDates();
    }
}
