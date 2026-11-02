package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javafx.scene.input.KeyCode;

/**
 * Decides what a key does while the keyboard focus is outside the command box, for example in the patient list.
 * Inside the command box, {@link CommandBox} handles the keys itself, except Shift+Tab. The arrow keys keep their
 * usual effect here; the view tabs and the lists give them their own meaning.
 */
public final class BrowseKeys {

    /**
     * What a key does outside the command box.
     */
    public enum Action {
        /** Moves the focus back to the command box, keeping the text in it. */
        RETURN_TO_COMMAND_BOX,
        /**
         * Resets the current view: shows all patients again, as {@code view} without an ID does, or goes back to the
         * weekly view of the appointments, or to this week.
         */
        RESET_VIEW,
        /** Moves the focus between the view tabs and the list, as given by {@link FocusArea#afterTab}. */
        SWITCH_AREA,
        /** Scrolls the details beside the list up. */
        SCROLL_DETAILS_UP,
        /** Scrolls the details beside the list down. */
        SCROLL_DETAILS_DOWN,
        /**
         * Does nothing, also in the command box, so that JavaFX does not move the focus on its own, as it would
         * with Shift+Tab.
         */
        IGNORE
    }

    private BrowseKeys() {}

    /**
     * Returns what pressing {@code key} does outside the command box, or {@code Optional#empty()} if the key keeps
     * its usual effect, such as Up and Down moving the selection in the patient list.
     *
     * @param isShiftDown Whether Shift is held.
     * @param isShortcutDown Whether Ctrl, Alt or Cmd is held. Such a key keeps its usual effect, such as copying.
     */
    public static Optional<Action> actionFor(KeyCode key, boolean isShiftDown, boolean isShortcutDown) {
        requireNonNull(key);
        if (isShortcutDown) {
            return Optional.empty();
        }
        return switch (key) {
            case TAB -> Optional.of(isShiftDown ? Action.IGNORE : Action.SWITCH_AREA);
            case ENTER -> isShiftDown ? Optional.empty() : Optional.of(Action.RETURN_TO_COMMAND_BOX);
            case ESCAPE -> isShiftDown ? Optional.empty() : Optional.of(Action.RESET_VIEW);
            case UP, KP_UP -> isShiftDown ? Optional.of(Action.SCROLL_DETAILS_UP) : Optional.empty();
            case DOWN, KP_DOWN -> isShiftDown ? Optional.of(Action.SCROLL_DETAILS_DOWN) : Optional.empty();
            default -> Optional.empty();
        };
    }

    /**
     * Returns true if typing {@code character} outside the command box should start a command there:
     * it is printable text, such as a letter, a digit or a space, typed without Ctrl, Alt or Cmd.
     *
     * @param isShortcutDown Whether Ctrl, Alt or Cmd is held.
     */
    public static boolean isCommandText(String character, boolean isShortcutDown) {
        requireNonNull(character);
        return !isShortcutDown && !character.isEmpty() && character.codePoints().noneMatch(Character::isISOControl);
    }
}
