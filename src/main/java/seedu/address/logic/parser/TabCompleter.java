package seedu.address.logic.parser;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADD_DETAILS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADD_MEDICAL_HISTORY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADD_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ALL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ARCHIVED;
import static seedu.address.logic.parser.CliSyntax.PREFIX_DATE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_DETAILS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_FROM;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ID;
import static seedu.address.logic.parser.CliSyntax.PREFIX_MEDICAL_HISTORY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TO;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import seedu.address.commons.core.FeatureFlags;
import seedu.address.logic.commands.AddAppointmentCommand;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearAppointmentCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.EditAppointmentCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.SetCommand;
import seedu.address.logic.commands.ViewAppointmentCommand;
import seedu.address.logic.commands.ViewCommand;
import seedu.address.model.Settings;
import seedu.address.model.patient.PatientId;

/**
 * Completes the word at the end of a partly typed command: a command word, an option such as {@code --name},
 * the command that {@code help} explains, or a patient ID. {@link IdCompleter} completes the patient ID that a
 * command takes first.
 */
public class TabCompleter {

    /** Every command word, in alphabetical order. */
    private static final List<String> COMMAND_WORDS = List.copyOf(HelpCommand.COMMAND_USAGE_MAP.keySet());

    /** What {@code help} can explain, in alphabetical order with the help window last. */
    private static final List<String> HELP_TARGETS = Stream.concat(COMMAND_WORDS.stream(),
            Stream.of(HelpCommand.TARGET_WINDOW)).toList();

    /** The settings that {@code set} can change, in the order of its usage message. */
    private static final List<String> SETTING_WORDS = Arrays.stream(Settings.Key.values())
            .map(Settings.Key::getWord).toList();

    /** What {@code set} can turn a setting to. */
    private static final List<String> SETTING_VALUES = List.of(SetCommand.WORD_ON, SetCommand.WORD_OFF);

    /** The options of each command that takes options, in the order of its usage message. */
    private static final Map<String, List<String>> OPTIONS = Map.of(
            AddCommand.COMMAND_WORD, options(PREFIX_ID, PREFIX_NAME, PREFIX_PHONE, PREFIX_ADDRESS,
                    PREFIX_MEDICAL_HISTORY, PREFIX_NOTES),
            EditCommand.COMMAND_WORD, options(PREFIX_NAME, PREFIX_PHONE, PREFIX_ADDRESS, PREFIX_MEDICAL_HISTORY,
                    PREFIX_ADD_MEDICAL_HISTORY, PREFIX_NOTES, PREFIX_ADD_NOTES),
            FindCommand.COMMAND_WORD, options(PREFIX_ID, PREFIX_NAME, PREFIX_PHONE, PREFIX_ADDRESS,
                    PREFIX_MEDICAL_HISTORY, PREFIX_NOTES, PREFIX_ALL, PREFIX_ARCHIVED),
            AddAppointmentCommand.COMMAND_WORD, options(PREFIX_DATE, PREFIX_DETAILS),
            EditAppointmentCommand.COMMAND_WORD, options(PREFIX_DATE, PREFIX_DETAILS, PREFIX_ADD_DETAILS),
            ViewAppointmentCommand.COMMAND_WORD, options(PREFIX_ID, PREFIX_FROM, PREFIX_TO, PREFIX_ALL,
                    PREFIX_ARCHIVED),
            ViewCommand.COMMAND_WORD, options(PREFIX_ALL, PREFIX_ARCHIVED));

    /** The options that choose the scope, of which a command takes at most one. */
    private static final List<String> SCOPE_OPTIONS = options(PREFIX_ALL, PREFIX_ARCHIVED);

    /**
     * Commands whose {@code --id} option takes the whole ID of an existing patient. {@code add} takes a new one, and
     * {@code find} takes any part of one.
     */
    private static final Set<String> COMMANDS_WITH_ID_OPTION = Set.of(ViewAppointmentCommand.COMMAND_WORD);

    private static final String OPTION_START = "-";

    private TabCompleter() {}

    /**
     * Returns each way to complete {@code commandText}, in the order that Tab goes through them, or an empty list if
     * there is nothing to complete, as in blank text. Spaces before the command word are dropped, and the rest of the
     * text is kept.
     * <ul>
     * <li>The first word is completed to the command words that start with it, so {@code dele} gives
     * {@code delete} and {@code deleteappt}, and {@code view} gives {@code viewappt}. A command word that no other
     * command word starts with, such as {@code addappt}, is completed with a patient ID.</li>
     * <li>A word that starts with {@code -} is completed to the options of the command that start with it, other
     * than itself, the options already typed, and the options that cannot be given with them: a second scope
     * option, or a scope option with a patient ID in {@code view} or {@code viewappt}.</li>
     * <li>The word after {@code help} is completed to a command word, or to {@code window}.</li>
     * <li>The word after {@code --id} in {@code viewappt} is completed to a patient ID.</li>
     * <li>Otherwise, {@link IdCompleter} completes the patient ID that the command takes first.</li>
     * </ul>
     * The word being completed matches ignoring case. Command words, options and what {@code help} explains are
     * completed in lowercase, the only case that {@link LaiNurseParser} accepts, so {@code DELE} gives
     * {@code delete}. The command word and options before it must be in lowercase already, as the parser expects
     * them, so {@code EDIT S1234567A --PH} has nothing to complete. IDs are given in the order of {@code ids}.
     */
    public static List<Completion> complete(String commandText, List<PatientId> ids) {
        requireAllNonNull(commandText, ids);
        String text = commandText.stripLeading();
        if (text.isEmpty()) {
            return List.of();
        }
        int wordStart = text.length();
        while (wordStart > 0 && !Character.isWhitespace(text.charAt(wordStart - 1))) {
            wordStart--;
        }
        String before = text.substring(0, wordStart);
        String word = text.substring(wordStart);

        if (before.isEmpty()) {
            List<Completion> commandWords = completeWord("", word, COMMAND_WORDS);
            return commandWords.isEmpty() ? IdCompleter.complete(commandText, ids) : commandWords;
        }

        List<String> earlierWords = Arrays.asList(before.strip().split("\\s+"));
        String commandWord = earlierWords.get(0);
        String previousWord = earlierWords.get(earlierWords.size() - 1);
        if (word.startsWith(OPTION_START)) {
            List<String> unusedOptions = optionsOf(commandWord).stream()
                    .filter(option -> !earlierWords.contains(option))
                    .filter(option -> !isExcluded(option, earlierWords))
                    .toList();
            return completeWord(before, word, unusedOptions);
        }
        if (commandWord.equals(HelpCommand.COMMAND_WORD) && earlierWords.size() == 1) {
            return completeWord(before, word, HELP_TARGETS);
        }
        if (FeatureFlags.isSettingsEnabled() && commandWord.equals(SetCommand.COMMAND_WORD)) {
            return switch (earlierWords.size()) {
                case 1 -> completeWord(before, word, SETTING_WORDS);
                case 2 -> completeWord(before, word, SETTING_VALUES);
                default -> List.of();
            };
        }
        if (COMMANDS_WITH_ID_OPTION.contains(commandWord) && previousWord.equals(PREFIX_ID.getPrefix().strip())) {
            String partialId = word.toUpperCase(Locale.ROOT);
            return ids.stream()
                    .map(id -> id.value)
                    .filter(id -> id.startsWith(partialId))
                    .map(id -> new Completion(before + id, id))
                    .toList();
        }
        return IdCompleter.complete(commandText, ids);
    }

    /**
     * Returns a completion of {@code partialWord} after {@code before} for each of the lowercase {@code words} that
     * starts with {@code partialWord}, ignoring case, other than {@code partialWord} itself, in the order of
     * {@code words}. So {@code VIEW} gives {@code view} and {@code viewappt}, but {@code view} gives only
     * {@code viewappt}.
     */
    private static List<Completion> completeWord(String before, String partialWord, List<String> words) {
        String lowercasePartialWord = partialWord.toLowerCase(Locale.ROOT);
        return words.stream()
                .filter(word -> word.startsWith(lowercasePartialWord) && !word.equals(partialWord))
                .map(word -> new Completion(before + word, word))
                .toList();
    }

    /**
     * Returns true if {@code option} cannot be given with {@code earlierWords}, the command word and what follows
     * it: a scope option after another, or a scope option and a patient ID together, in {@code view} or
     * {@code viewappt}, where the ID shows that patient, archived or not.
     */
    private static boolean isExcluded(String option, List<String> earlierWords) {
        String commandWord = earlierWords.get(0);
        boolean hasScope = earlierWords.stream().anyMatch(SCOPE_OPTIONS::contains);
        boolean hasId = switch (commandWord) {
            case ViewCommand.COMMAND_WORD -> earlierWords.size() > 1 && !earlierWords.get(1).startsWith(OPTION_START);
            case ViewAppointmentCommand.COMMAND_WORD -> earlierWords.contains(PREFIX_ID.getPrefix().strip());
            default -> false;
        };
        if (SCOPE_OPTIONS.contains(option)) {
            return hasScope || hasId;
        }
        return commandWord.equals(ViewAppointmentCommand.COMMAND_WORD)
                && option.equals(PREFIX_ID.getPrefix().strip()) && hasScope;
    }

    /**
     * Returns the options of {@code commandWord}, in the order of its usage message, with those of the commands that
     * are released only once their feature is (#217).
     */
    private static List<String> optionsOf(String commandWord) {
        if (FeatureFlags.isSelectiveClearEnabled()) {
            if (commandWord.equals(ClearAppointmentCommand.COMMAND_WORD)) {
                return options(PREFIX_FROM, PREFIX_TO);
            }
            if (commandWord.equals(ClearCommand.COMMAND_WORD)) {
                return options(PREFIX_ARCHIVED);
            }
        }
        return OPTIONS.getOrDefault(commandWord, List.of());
    }

    private static List<String> options(Prefix... prefixes) {
        return Stream.of(prefixes).map(prefix -> prefix.getPrefix().strip()).toList();
    }
}
