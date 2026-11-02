package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.patient.PatientId;

public class IdCompleterTest {

    private static final List<PatientId> IDS = List.of(new PatientId("S1234567A"), new PatientId("T7654321B"),
            new PatientId("S1999999C"));

    @Test
    public void complete_partialId_givesIdAsCompletedWord() {
        assertEquals(List.of(new Completion("edit S1234567A --phone 9", "S1234567A")),
                IdCompleter.complete("edit S12 --phone 9", IDS));
    }

    @Test
    public void complete_partialId_returnsMatchesInGivenOrder() {
        assertEquals(List.of("view S1234567A", "view S1999999C"), completeTexts("view S1", IDS));
        assertEquals(List.of("delete T7654321B"), completeTexts("delete T7", IDS));
    }

    @Test
    public void complete_archiveCommands_completeId() {
        for (String command : List.of("archive", "unarchive")) {
            assertEquals(List.of(command + " S1234567A", command + " S1999999C"), completeTexts(command + " S1", IDS));
        }
    }

    @Test
    public void complete_statusCommands_keepsAppointmentNumber() {
        for (String command : List.of("markappt", "unmarkappt")) {
            assertEquals(List.of(command + " S1234567A 2", command + " S1999999C 2"),
                    completeTexts(command + " s1 2", IDS));
            assertEquals(List.of(command + " S1234567A", command + " T7654321B", command + " S1999999C"),
                    completeTexts(command, IDS));
        }
    }

    @Test
    public void complete_lowercasePartialId_matchesIgnoringCase() {
        assertEquals(List.of("view T7654321B"), completeTexts("view t76", IDS));
    }

    @Test
    public void complete_noIdTyped_returnsEveryId() {
        List<String> expected = List.of("view S1234567A", "view T7654321B", "view S1999999C");
        assertEquals(expected, completeTexts("view ", IDS));
        // the command word alone gets a space before the ID
        assertEquals(expected, completeTexts("view", IDS));
    }

    @Test
    public void complete_fullId_returnsItself() {
        assertEquals(List.of("view S1234567A"), completeTexts("view S1234567A", IDS));
    }

    @Test
    public void complete_spacing_keepsSpacesBetweenWordsAndDropsLeadingSpaces() {
        assertEquals(List.of("view   T7654321B"), completeTexts("   view   T7", IDS));
    }

    @Test
    public void complete_noMatchingId_returnsEmptyList() {
        assertEquals(List.of(), completeTexts("view X", IDS));
        assertEquals(List.of(), completeTexts("view S1", List.of()));
    }

    @Test
    public void complete_editWithIdLast_completesId() {
        assertEquals(List.of("edit S1234567A", "edit S1999999C"), completeTexts("edit s1", IDS));
        assertEquals(List.of("edit S1234567A", "edit T7654321B", "edit S1999999C"),
                completeTexts("edit", IDS));
    }

    @Test
    public void complete_editApptWithOptionsAfterId_completesIdAndKeepsOptions() {
        assertEquals(List.of("editappt T7654321B 2 --date 2026-10-06"),
                completeTexts("editappt t7 2 --date 2026-10-06", IDS));
        assertEquals(List.of("editappt S1234567A", "editappt S1999999C"), completeTexts("editappt S1", IDS));
    }

    @Test
    public void complete_addApptWithOptionsAfterId_completesIdAndKeepsOptions() {
        assertEquals(List.of("addappt T7654321B --date 2026-10-05 09:30 --details Bring pen"),
                completeTexts("addappt t7 --date 2026-10-05 09:30 --details Bring pen", IDS));
        assertEquals(List.of("addappt S1234567A", "addappt S1999999C"), completeTexts("addappt s1", IDS));
    }

    @Test
    public void complete_deleteApptWithNumberAfterId_completesIdAndKeepsNumber() {
        assertEquals(List.of("deleteappt T7654321B 2"),
                completeTexts("deleteappt t7 2", IDS));
        assertEquals(List.of("deleteappt S1234567A", "deleteappt S1999999C"),
                completeTexts("deleteappt s1", IDS));
    }

    @Test
    public void complete_editWithOptionsAfterId_completesIdAndKeepsOptions() {
        assertEquals(List.of("edit T7654321B --phone 91234567 --notes Enjoys gardening"),
                completeTexts("edit t7 --phone 91234567 --notes Enjoys gardening", IDS));
        // the spacing around the ID and in the options is kept
        assertEquals(List.of("edit\tT7654321B\t--name\tAlex  Tan"),
                completeTexts("edit\tT7\t--name\tAlex  Tan", IDS));
        // a space after the ID, before any option is typed
        assertEquals(List.of("edit T7654321B "), completeTexts("edit T7 ", IDS));
    }

    @Test
    public void complete_editWithoutId_returnsEmptyList() {
        // the options come first, so there is no ID to complete
        assertEquals(List.of(), completeTexts("edit --phone 91234567", IDS));
        assertEquals(List.of(), completeTexts("edit X --phone 91234567", IDS));
    }

    @Test
    public void complete_nothingToComplete_returnsEmptyList() {
        assertEquals(List.of(), completeTexts("", IDS));
        assertEquals(List.of(), completeTexts("   ", IDS));
        // view and delete take only an ID, so it must end the command
        assertEquals(List.of(), completeTexts("view S1 ", IDS));
        assertEquals(List.of(), completeTexts("view S1 extra", IDS));
        assertEquals(List.of(), completeTexts("delete S1 extra", IDS));
        // commands that do not take the ID of an existing patient
        assertEquals(List.of(), completeTexts("add --id S1", IDS));
        assertEquals(List.of(), completeTexts("find S1", IDS));
        // command words are case-sensitive, as in LaiNurseParser
        assertEquals(List.of(), completeTexts("VIEW S1", IDS));
    }

    @Test
    public void complete_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> completeTexts(null, IDS));
        assertThrows(NullPointerException.class, () -> completeTexts("view S1", null));
    }

    /**
     * Returns the whole command of each completion that {@code IdCompleter} gives.
     */
    private static List<String> completeTexts(String commandText, List<PatientId> ids) {
        return IdCompleter.complete(commandText, ids).stream().map(Completion::text).toList();
    }
}
