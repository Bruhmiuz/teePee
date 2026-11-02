package seedu.address.commons.util;

import static java.util.Objects.requireNonNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Pattern;

/**
 * Contains date parsing and formatting utilities, and the week that a date is in.
 */
public final class DateUtil {

    public static final int DAYS_IN_WEEK = 7;

    /** The earliest date that the four-digit, unsigned year of a date allows. */
    public static final LocalDate EARLIEST_DATE = LocalDate.of(0, 1, 1);
    /** The latest date that the four-digit, unsigned year of a date allows. */
    public static final LocalDate LATEST_DATE = LocalDate.of(9999, 12, 31);

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String MESSAGE_CONSTRAINTS = "Dates must be a real date in the format " + DATE_FORMAT + ".";

    // Strict resolving rejects dates that do not exist, such as 2026-02-30.
    // "uuuu" is the proleptic year, which strict resolving requires in place of "yyyy".
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    // The "uuuu" pattern also accepts a sign and digits beyond four, such as "+12345-01-01",
    // which is not a shape a nurse would type, so only four unsigned digits are accepted.
    private static final Pattern VALID_DATE_SHAPE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    // Used to name the part of a rejected date that does not have the shape it needs.
    private static final Pattern FOUR_DIGIT_SHAPE = Pattern.compile("\\d{4}");
    private static final Pattern TWO_DIGIT_SHAPE = Pattern.compile("\\d{2}");

    public static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern(TIME_FORMAT).withResolverStyle(ResolverStyle.STRICT);

    private DateUtil() {}

    /**
     * Parses {@code String date} into a {@code LocalDate}. The shape must be {@code yyyy-MM-dd}: the year
     * exactly four digits without a sign, and the month and day each exactly two digits, so
     * {@code 0000-01-01} is valid while {@code +12345-01-01} and {@code 2026-1-5} are not.
     *
     * @throws DateTimeParseException if the given {@code date} is invalid.
     * @throws NullPointerException if {@code date} is null.
     */
    public static LocalDate parseDate(String date) {
        requireNonNull(date);
        if (!VALID_DATE_SHAPE.matcher(date).matches()) {
            throw new DateTimeParseException(
                    "Text '" + date + "' is not in the form " + DATE_FORMAT + ": " + describeDateShapeError(date),
                    date, 0);
        }
        // The shape and the formatter must agree, so that the date that parses is the date that was typed.
        LocalDate parsed = LocalDate.parse(date, DATE_FORMATTER);
        assert parsed.format(DATE_FORMATTER).equals(date)
                : "A date of the right shape must round-trip through the formatter.";
        return parsed;
    }

    /**
     * Returns what is wrong with the shape of {@code date}, naming the first part that breaks it,
     * so that the exception text does not blame the year for a wrong month or day.
     */
    private static String describeDateShapeError(String date) {
        String[] parts = date.split("-", -1);
        if (parts.length >= 3) {
            if (!FOUR_DIGIT_SHAPE.matcher(parts[0]).matches()) {
                return "the year must have exactly four digits without a sign";
            }
            if (!TWO_DIGIT_SHAPE.matcher(parts[1]).matches()) {
                return "the month must have two digits";
            }
            if (!TWO_DIGIT_SHAPE.matcher(parts[2]).matches()) {
                return "the day must have two digits";
            }
        }
        return "the text must have exactly a four-digit year, a two-digit month and a two-digit day";
    }

    /**
     * Parses {@code String time} into a {@code LocalTime}.
     *
     * @throws DateTimeParseException if the given {@code time} is invalid.
     * @throws NullPointerException if {@code time} is null.
     */
    public static LocalTime parseTime(String time) {
        requireNonNull(time);
        return LocalTime.parse(time, TIME_FORMATTER);
    }

    /**
     * Returns the Monday of the week, from Monday to Sunday, that contains {@code date}.
     * The logic and the UI both take their weeks from here, so that they agree on which days a week has.
     */
    public static LocalDate getStartOfWeek(LocalDate date) {
        requireNonNull(date);
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Returns the Sunday of the week, from Monday to Sunday, that contains {@code date}.
     */
    public static LocalDate getEndOfWeek(LocalDate date) {
        LocalDate start = getStartOfWeek(date);
        LocalDate end = start.plusDays(DAYS_IN_WEEK - 1);
        assert getStartOfWeek(end).equals(start)
                : "The end of the week must be in the same week as the start.";
        return end;
    }
}
