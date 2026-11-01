package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ALL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ARCHIVED;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ID;
import static seedu.address.logic.parser.CliSyntax.PREFIX_MEDICAL_HISTORY;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NOTES;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;

import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.util.TextNormalizer;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.PatientScope;
import seedu.address.model.patient.PatientContainsSubstringsPredicate;

/**
 * Parses input arguments and creates a new FindCommand object
 */
public class FindCommandParser implements Parser<FindCommand> {

    private static final Logger logger = LogsCenter.getLogger(FindCommandParser.class);
    private static final Pattern OPTION_PATTERN = Pattern.compile("(?<!\\S)--\\S*");
    private static final Prefix[] SEARCH_PREFIXES = {
        PREFIX_ID, PREFIX_NAME, PREFIX_PHONE, PREFIX_ADDRESS, PREFIX_MEDICAL_HISTORY, PREFIX_NOTES
    };
    private static final Prefix[] SUPPORTED_PREFIXES = {
        PREFIX_ID, PREFIX_NAME, PREFIX_PHONE, PREFIX_ADDRESS, PREFIX_MEDICAL_HISTORY, PREFIX_NOTES, PREFIX_ALL,
        PREFIX_ARCHIVED
    };

    /**
     * Parses the given {@code String} of arguments in the context of the FindCommand
     * and returns a FindCommand object for execution.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public FindCommand parse(String args) throws ParseException {
        requireNonNull(args);
        String invalidFormatMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        String normalizedArgs = ArgumentTokenizer.normalizeOptionSeparators(args, invalidFormatMessage,
                SUPPORTED_PREFIXES);
        ArgumentMultimap argMultimap = ArgumentTokenizer.tokenize(normalizedArgs, SUPPORTED_PREFIXES);

        if (!argMultimap.getPreamble().isEmpty()
                || Stream.of(SEARCH_PREFIXES).noneMatch(prefix -> argMultimap.getValue(prefix).isPresent())) {
            throw new ParseException(invalidFormatMessage);
        }

        argMultimap.verifyNoDuplicatePrefixesFor(SUPPORTED_PREFIXES);
        Optional<PatientScope> scope = ParserUtil.parseScope(argMultimap, invalidFormatMessage);
        return new FindCommand(new PatientContainsSubstringsPredicate(
                parseSearchValue(argMultimap, PREFIX_ID), parseSearchValue(argMultimap, PREFIX_NAME),
                parseSearchValue(argMultimap, PREFIX_PHONE), parseSearchValue(argMultimap, PREFIX_ADDRESS),
                parseSearchValue(argMultimap, PREFIX_MEDICAL_HISTORY), parseSearchValue(argMultimap, PREFIX_NOTES)),
                scope);
    }

    /**
     * Returns an optional non-blank substring, removing a matching pair of surrounding double quotes.
     * Phone substrings must contain at least one digit.
     */
    private static Optional<String> parseSearchValue(ArgumentMultimap argMultimap, Prefix prefix)
            throws ParseException {
        Optional<String> argument = argMultimap.getValue(prefix);
        if (argument.isEmpty()) {
            return Optional.empty();
        }

        String value = argument.get();
        if (value.startsWith("\"") || value.startsWith("“")) {
            char closingQuote = value.startsWith("\"") ? '"' : '”';
            if (value.length() < 2 || value.indexOf(closingQuote, 1) != value.length() - 1) {
                throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
            }
            value = value.substring(1, value.length() - 1);
        } else if (value.startsWith("”") || value.endsWith("\"") || value.endsWith("“") || value.endsWith("”")) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
        }

        if (TextNormalizer.normalizeText(value).isEmpty() || OPTION_PATTERN.matcher(value).find()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
        }
        if (prefix.equals(PREFIX_PHONE)) {
            if (value.replaceAll("[^0-9]", "").isEmpty()) {
                logger.fine("Rejected phone search without digits.");
                throw new ParseException(PatientContainsSubstringsPredicate.MESSAGE_PHONE_SEARCH_CONSTRAINTS);
            }
            logger.fine("Parsed phone search.");
        }
        return Optional.of(value);
    }
}
