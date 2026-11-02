package seedu.address.model;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import seedu.address.commons.util.ToStringBuilder;

/**
 * The settings that decide how marks, past clashes and archived patients behave (#216), saved with the other user
 * preferences. The defaults are how LaiNurse behaves without settings.
 * Guarantees: immutable.
 */
public class Settings {

    /** The settings that a user can change, each with the word that the {@code set} command takes for it. */
    public enum Key {
        /** On: a marked appointment never clashes. Off: it clashes like any other. */
        MARK_STOPS_CLASHES("mark-noclash"),
        /** On: a marked appointment counts as over, as one that has passed. Off: a mark only shows a pin. */
        MARK_COUNTS_AS_DONE("mark-past"),
        /** On: appointments that have passed can clash. Off: they never do, and neither do marked ones. */
        PAST_CLASHES("past-clash"),
        /** On: archived patients' appointments clash like any other. Off: they never do. */
        ARCHIVED_CLASHES("archived-clash");

        private final String word;

        Key(String word) {
            this.word = word;
        }

        public String getWord() {
            return word;
        }

        /**
         * Returns the setting that {@code word} names, or {@code Optional#empty()} if it names none.
         */
        public static Optional<Key> of(String word) {
            requireNonNull(word);
            return Arrays.stream(values()).filter(key -> key.word.equals(word)).findFirst();
        }
    }

    /** How LaiNurse behaves without settings. */
    public static final Settings DEFAULT = new Settings();

    private final boolean isMarkStoppingClashes;
    private final boolean isMarkCountedAsDone;
    private final boolean isPastClashing;
    private final boolean isArchivedClashing;

    /**
     * Constructs the default settings.
     */
    public Settings() {
        this(true, false, true, false);
    }

    /**
     * Constructs settings with the given values.
     */
    public Settings(boolean isMarkStoppingClashes, boolean isMarkCountedAsDone, boolean isPastClashing,
            boolean isArchivedClashing) {
        this.isMarkStoppingClashes = isMarkStoppingClashes;
        this.isMarkCountedAsDone = isMarkCountedAsDone;
        this.isPastClashing = isPastClashing;
        this.isArchivedClashing = isArchivedClashing;
    }

    public boolean isMarkStoppingClashes() {
        return isMarkStoppingClashes;
    }

    public boolean isMarkCountedAsDone() {
        return isMarkCountedAsDone;
    }

    public boolean isPastClashing() {
        return isPastClashing;
    }

    public boolean isArchivedClashing() {
        return isArchivedClashing;
    }

    /**
     * Returns whether the setting {@code key} is on.
     */
    public boolean isOn(Key key) {
        requireNonNull(key);
        return switch (key) {
            case MARK_STOPS_CLASHES -> isMarkStoppingClashes;
            case MARK_COUNTS_AS_DONE -> isMarkCountedAsDone;
            case PAST_CLASHES -> isPastClashing;
            case ARCHIVED_CLASHES -> isArchivedClashing;
        };
    }

    /**
     * Returns a copy of these settings with the setting {@code key} turned on if {@code isOn}, and off otherwise.
     */
    public Settings with(Key key, boolean isOn) {
        requireNonNull(key);
        return new Settings(key == Key.MARK_STOPS_CLASHES ? isOn : isMarkStoppingClashes,
                key == Key.MARK_COUNTS_AS_DONE ? isOn : isMarkCountedAsDone,
                key == Key.PAST_CLASHES ? isOn : isPastClashing,
                key == Key.ARCHIVED_CLASHES ? isOn : isArchivedClashing);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Settings otherSettings)) {
            return false;
        }

        return isMarkStoppingClashes == otherSettings.isMarkStoppingClashes
                && isMarkCountedAsDone == otherSettings.isMarkCountedAsDone
                && isPastClashing == otherSettings.isPastClashing
                && isArchivedClashing == otherSettings.isArchivedClashing;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isMarkStoppingClashes, isMarkCountedAsDone, isPastClashing, isArchivedClashing);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("isMarkStoppingClashes", isMarkStoppingClashes)
                .add("isMarkCountedAsDone", isMarkCountedAsDone)
                .add("isPastClashing", isPastClashing)
                .add("isArchivedClashing", isArchivedClashing)
                .toString();
    }
}
