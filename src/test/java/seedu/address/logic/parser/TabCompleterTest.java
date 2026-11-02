package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.FeatureFlags;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.model.patient.PatientId;

public class TabCompleterTest {

    private static final List<PatientId> IDS = List.of(new PatientId("S1234567A"), new PatientId("T7654321B"),
            new PatientId("S1999999C"));

    @Test
    public void complete_partialCommandWord_givesLongerCommandWordsInAlphabeticalOrder() {
        assertEquals(List.of("delete", "deleteappt"), words("dele"));
        assertEquals(List.of("delete", "deleteappt"), texts("dele"));
        assertEquals(List.of("markappt"), words("m"));
        // spaces before the command word are dropped
        assertEquals(List.of("find"), texts("  fi"));
    }

    @Test
    public void complete_commandWordThatLongerOnesStartWith_givesOnlyTheLongerOnes() {
        assertEquals(List.of("viewappt"), words("view"));
        assertEquals(List.of("editappt"), words("edit"));
        assertEquals(List.of("addappt"), words("add"));
    }

    @Test
    public void complete_wholeCommandWordThatTakesId_givesIds() {
        assertEquals(List.of("addappt S1234567A", "addappt T7654321B", "addappt S1999999C"), texts("addappt"));
        assertEquals(List.of("S1234567A", "T7654321B", "S1999999C"), words("markappt"));
        // a space after a command word that longer ones start with
        assertEquals(List.of("view S1234567A", "view S1999999C"), texts("view s1"));
    }

    @Test
    public void complete_wholeCommandWordWithNothingToComplete_returnsEmptyList() {
        assertEquals(List.of(), words("exit"));
        assertEquals(List.of(), words("viewappt"));
        assertEquals(List.of(), words("viewappt "));
        assertEquals(List.of(), words("xyz"));
    }

    @Test
    public void complete_option_givesOptionsOfTheCommandInUsageOrder() {
        assertEquals(List.of("--date", "--details", "--add-details"), words("editappt S1234567A 2 --"));
        assertEquals(List.of("editappt S1234567A 2 --date", "editappt S1234567A 2 --details",
                "editappt S1234567A 2 --add-details"), texts("editappt S1234567A 2 --"));
        assertEquals(List.of("--medical-history"), words("edit S1234567A --m"));
        assertEquals(List.of("--add-medical-history", "--add-notes"), words("edit S1234567A --add-"));
        assertEquals(List.of("--id", "--from", "--to", "--all", "--archived"), words("viewappt -"));
        assertEquals(List.of("--date", "--details"), words("addappt S1234567A --d"));
        assertEquals(List.of("--details"), words("addappt S1234567A --date 2026-12-01 --d"));
    }

    @Test
    public void complete_uppercase_givesLowercaseWords() {
        assertEquals(List.of("delete", "deleteappt"), texts("DELE"));
        assertEquals(List.of("edit S1234567A --name"), texts("edit S1234567A --NA"));
        assertEquals(List.of("help add", "help addappt"), texts("help ADD"));
        // the whole word in the wrong case gives the lowercase word first
        assertEquals(List.of("view", "viewappt"), texts("VIEW"));
        assertEquals(List.of("edit S1234567A --notes"), texts("edit S1234567A --Notes"));
    }

    @Test
    public void complete_earlierWordNotInLowercase_returnsEmptyList() {
        // the parser accepts only lowercase command words and options, so they are not completed after
        assertEquals(List.of(), words("EDIT S1234567A --PH"));
        assertEquals(List.of(), words("HELP W"));
        assertEquals(List.of(), words("VIEWAPPT --id S1"));
        assertEquals(List.of(), words("viewappt --ID S1"));
        assertEquals(List.of(), words("View s1"));
        assertEquals(List.of(), words("EDIT s1 --phone 91234567"));
    }

    @Test
    public void complete_option_skipsOptionsAlreadyTyped() {
        assertEquals(List.of("--phone", "--address", "--medical-history", "--add-medical-history", "--notes",
                "--add-notes"), words("edit S1234567A --name Alex Tan --"));
        assertEquals(List.of("--name", "--phone", "--address", "--medical-history", "--notes"),
                words("add --id S1234567A --"));
    }

    @Test
    public void complete_option_keepsSpacingBeforeIt() {
        assertEquals(List.of("find\t--name"), texts("find\t--na"));
        assertEquals(List.of("viewappt 2026-10-01   --id"), texts("  viewappt 2026-10-01   --i"));
    }

    @Test
    public void complete_scopeOptions_offersOnlyThoseThatCanBeGiven() {
        assertEquals(List.of("--all", "--archived"), words("view --"));
        assertEquals(List.of("--archived"), words("view --ar"));
        assertEquals(List.of("--address", "--all", "--archived"), words("find --name Alex --a"));
        // at most one scope option
        assertEquals(List.of(), words("view --all --"));
        assertEquals(List.of("--id", "--name", "--phone", "--address", "--medical-history", "--notes"),
                words("find --archived --"));
        // an ID shows that patient, archived or not
        assertEquals(List.of(), words("view S1234567A --"));
        assertEquals(List.of("--from", "--to"), words("viewappt --id S1234567A --"));
        assertEquals(List.of("--from", "--to"), words("viewappt --all --"));
    }

    @Test
    public void complete_noOptionToComplete_returnsEmptyList() {
        // the whole option, which no other option starts with
        assertEquals(List.of(), words("edit S1234567A --notes"));
        // commands without options
        assertEquals(List.of(), words("deleteappt S1234567A --"));
        assertEquals(List.of(), words("xyz --"));
    }

    @Test
    public void complete_afterSet_givesSettingsThenOnOrOff() {
        boolean wasEnabled = FeatureFlags.isSettingsEnabled();
        FeatureFlags.setSettingsEnabled(true);
        try {
            assertEquals(List.of("mark-noclash", "mark-past", "past-clash", "archived-clash"), words("set "));
            assertEquals(List.of("set mark-noclash", "set mark-past"), texts("set mark"));
            assertEquals(List.of("set past-clash"), texts("set PAST"));
            assertEquals(List.of("set past-clash on", "set past-clash off"), texts("set past-clash "));
            assertEquals(List.of("off"), words("set past-clash of"));
            // a whole word, or a command with both words, has nothing left to complete
            assertEquals(List.of(), words("set past-clash"));
            assertEquals(List.of(), words("set past-clash on "));
        } finally {
            FeatureFlags.setSettingsEnabled(wasEnabled);
        }
    }

    @Test
    public void complete_helpTarget_givesCommandWordsAndWindow() {
        assertEquals(List.of("add", "addappt"), words("help ad"));
        assertEquals(List.of("help window"), texts("help w"));
        assertEquals(List.of("help add", "help addappt"), texts("help AD"));
        assertEquals(List.of("addappt"), words("help add"));
        // every command word, then window
        List<String> targets = words("help ");
        assertEquals(HelpCommand.COMMAND_USAGE_MAP.size() + 1, targets.size());
        assertEquals("window", targets.get(targets.size() - 1));
    }

    @Test
    public void complete_helpWithTwoWords_returnsEmptyList() {
        assertEquals(List.of(), words("help add a"));
    }

    @Test
    public void complete_idAfterIdOption_givesExistingIds() {
        assertEquals(List.of("viewappt 2026-10-15 --id S1234567A", "viewappt 2026-10-15 --id S1999999C"),
                texts("viewappt 2026-10-15 --id s1"));
        assertEquals(List.of("S1234567A", "T7654321B", "S1999999C"), words("viewappt --from 2026-10-01 --id "));
        assertEquals(List.of(), words("viewappt --id X"));
    }

    @Test
    public void complete_idAfterIdOptionOfAddOrFind_returnsEmptyList() {
        // add takes the ID of a new patient
        assertEquals(List.of(), words("add --id S1"));
        // find takes any part of an ID
        assertEquals(List.of(), words("find --id S1"));
        assertEquals(List.of(), words("find --name Alex --id "));
    }

    @Test
    public void complete_idThatCommandTakesFirst_completesWithIdCompleter() {
        assertEquals(List.of("edit T7654321B --phone 91234567"), texts("edit t7 --phone 91234567"));
        assertEquals(List.of("T7654321B"), words("edit t7 --phone 91234567"));
    }

    @Test
    public void complete_blank_returnsEmptyList() {
        assertEquals(List.of(), words(""));
        assertEquals(List.of(), words("   "));
    }

    @Test
    public void complete_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> TabCompleter.complete(null, IDS));
        assertThrows(NullPointerException.class, () -> TabCompleter.complete("view S1", null));
    }

    private static List<String> texts(String commandText) {
        return TabCompleter.complete(commandText, IDS).stream().map(Completion::text).toList();
    }

    private static List<String> words(String commandText) {
        return TabCompleter.complete(commandText, IDS).stream().map(Completion::word).toList();
    }

    @Test
    public void complete_selectiveClearReleased_completesItsOptionsAndIds() {
        FeatureFlags.setSelectiveClearEnabled(true);
        try {
            assertEquals(List.of("--from", "--to"), words("clearappt --"));
            assertEquals(List.of("--archived"), words("clear --a"));
            assertEquals(List.of("S1234567A", "S1999999C"), words("clearappt S1"));
            // the ID has no option of its own
            assertEquals(List.of(), words("clearappt --id S1"));
        } finally {
            FeatureFlags.setSelectiveClearEnabled(false);
        }
        // before the release, neither has options
        assertEquals(List.of(), words("clearappt --"));
        assertEquals(List.of(), words("clear --a"));
    }
}
