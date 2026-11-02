package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.util.StringUtil;
import seedu.address.model.LaiNurse;
import seedu.address.model.ReadOnlyLaiNurse;
import seedu.address.model.util.SampleDataUtil;

/**
 * Loads the LaiNurse data that the app starts with from a {@link Storage}, falling back to the backup when the data
 * file cannot be loaded.
 * <ul>
 * <li>If the data file loads, its data is used.</li>
 * <li>If the data file is missing, the sample data is used, even if there is a backup. A backup is first copied to
 * the previous backup, since the next save replaces it.</li>
 * <li>If the data file cannot be loaded, it is copied to the corrupt copy, and the backup's data is used, or no data
 * if the backup is missing or cannot be loaded either.</li>
 * </ul>
 */
public class LaiNurseLoader {

    /** Closing the window does not save, but every command that succeeds does, {@code exit} included. */
    public static final String MESSAGE_EMPTY_DATA_SAVED = "The next command that succeeds, even exit, saves this"
            + " empty data over both files. To leave them as they are, close the window with its close button.";
    public static final String MESSAGE_BACKUP_LOADED = "%1$s could not be loaded, so LaiNurse loaded its backup, %2$s,"
            + " which holds the data it last saved.";
    public static final String MESSAGE_BACKUP_NOT_LOADED = "%1$s could not be loaded, and neither could its backup,"
            + " %2$s, so LaiNurse started with no data. " + MESSAGE_EMPTY_DATA_SAVED;
    public static final String MESSAGE_BACKUP_MISSING = "%1$s could not be loaded, and there is no backup at %2$s,"
            + " so LaiNurse started with no data. " + MESSAGE_EMPTY_DATA_SAVED;
    public static final String MESSAGE_CORRUPT_FILE_KEPT = "A copy of the file that could not be loaded is kept at"
            + " %1$s. To restore it, close the window with its close button, fix the copy, and copy it over %2$s.";
    public static final String MESSAGE_CORRUPT_FILE_NOT_KEPT = "The file that could not be loaded could not be copied"
            + " to %1$s. To keep it, close the window with its close button now, without running any command, not"
            + " even exit, and copy the file somewhere else. The next command that succeeds overwrites it.";
    public static final String MESSAGE_SAMPLE_DATA_BACKUP_KEPT = "%1$s is missing, so LaiNurse loaded the sample data."
            + " Your last backup is kept at %2$s. To restore it, close LaiNurse, and copy it to %1$s.";
    public static final String MESSAGE_SAMPLE_DATA_BACKUP_NOT_KEPT = "%1$s is missing, so LaiNurse loaded the sample"
            + " data, and its backup could not be copied to %2$s. To keep the backup, close the window with its close"
            + " button now, without running any command, not even exit, and copy %3$s somewhere else. The next command"
            + " that succeeds overwrites it.";

    private static final Logger logger = LogsCenter.getLogger(LaiNurseLoader.class);

    /**
     * The data that the app starts with, and the message that tells the user where it came from, if it did not come
     * from the data file as usual.
     */
    public record Result(ReadOnlyLaiNurse data, Optional<String> message) {
        /**
         * Creates a {@code Result} with the given data and message.
         */
        public Result {
            requireNonNull(data);
            requireNonNull(message);
        }
    }

    private LaiNurseLoader() {}

    /**
     * Returns the data that the app starts with, as read from {@code storage}.
     */
    public static Result load(Storage storage) {
        requireNonNull(storage);
        logger.info("Using data file : " + storage.getLaiNurseFilePath());

        try {
            Optional<ReadOnlyLaiNurse> laiNurseOptional = storage.readLaiNurse();
            if (laiNurseOptional.isPresent()) {
                return new Result(laiNurseOptional.get(), Optional.empty());
            }
            logger.info("Creating a new data file " + storage.getLaiNurseFilePath() + " populated with sample data.");
            return new Result(SampleDataUtil.getSampleLaiNurse(), keepBackup(storage));
        } catch (DataLoadingException e) {
            logger.warning("Data file at " + storage.getLaiNurseFilePath() + " could not be loaded.");
        }

        // The data file is copied before the backup is read, so that the copy is kept whatever the backup holds
        String corruptFileMessage = keepCorruptFile(storage);
        return loadBackup(storage, corruptFileMessage);
    }

    /**
     * Returns the data in the backup, or no data if the backup is missing or cannot be loaded either, with a message
     * that says which, followed by {@code corruptFileMessage}.
     */
    private static Result loadBackup(Storage storage, String corruptFileMessage) {
        try {
            Optional<ReadOnlyLaiNurse> backupOptional = storage.readLaiNurseBackup();
            if (backupOptional.isPresent()) {
                logger.warning("Loaded the backup at " + storage.getLaiNurseBackupFilePath() + " instead.");
                return new Result(backupOptional.get(), message(MESSAGE_BACKUP_LOADED, storage, corruptFileMessage));
            }
            logger.warning("No backup at " + storage.getLaiNurseBackupFilePath()
                    + ". Will be starting with an empty patient list.");
            return new Result(new LaiNurse(), message(MESSAGE_BACKUP_MISSING, storage, corruptFileMessage));
        } catch (DataLoadingException e) {
            logger.warning("Backup at " + storage.getLaiNurseBackupFilePath() + " could not be loaded either."
                    + " Will be starting with an empty patient list.");
            return new Result(new LaiNurse(), message(MESSAGE_BACKUP_NOT_LOADED, storage, corruptFileMessage));
        }
    }

    /**
     * Returns {@code format}, which names the data file and then the backup of {@code storage}, followed on a line of
     * its own by {@code corruptFileMessage}.
     */
    private static Optional<String> message(String format, Storage storage, String corruptFileMessage) {
        assert format.contains("%1$s") && format.contains("%2$s") : "The message must name both files.";
        return Optional.of(String.format(format, storage.getLaiNurseFilePath(), storage.getLaiNurseBackupFilePath())
                + "\n" + corruptFileMessage);
    }

    /**
     * Copies any backup to the previous backup, since the next save replaces it with the sample data, and returns the
     * message that tells the user whether it was kept. Returns an empty {@code Optional} if there is no backup.
     */
    private static Optional<String> keepBackup(Storage storage) {
        if (!Files.exists(storage.getLaiNurseBackupFilePath())) {
            return Optional.empty();
        }
        try {
            storage.keepLaiNurseBackup();
            logger.warning("Data file is missing. Copied the backup to " + storage.getLaiNursePreviousFilePath());
            return Optional.of(String.format(MESSAGE_SAMPLE_DATA_BACKUP_KEPT,
                    storage.getLaiNurseFilePath(), storage.getLaiNursePreviousFilePath()));
        } catch (IOException e) {
            logger.warning("Failed to copy the backup to " + storage.getLaiNursePreviousFilePath() + " : "
                    + StringUtil.getDetails(e));
            return Optional.of(String.format(MESSAGE_SAMPLE_DATA_BACKUP_NOT_KEPT, storage.getLaiNurseFilePath(),
                    storage.getLaiNursePreviousFilePath(), storage.getLaiNurseBackupFilePath()));
        }
    }

    /**
     * Copies the data file that could not be loaded to the corrupt copy, and returns the message that tells the user
     * whether it was kept.
     */
    private static String keepCorruptFile(Storage storage) {
        try {
            storage.keepCorruptLaiNurseFile();
            logger.info("Copied the data file to " + storage.getLaiNurseCorruptFilePath());
            return String.format(MESSAGE_CORRUPT_FILE_KEPT,
                    storage.getLaiNurseCorruptFilePath(), storage.getLaiNurseFilePath());
        } catch (IOException e) {
            logger.warning("Failed to copy the data file to " + storage.getLaiNurseCorruptFilePath() + " : "
                    + StringUtil.getDetails(e));
            return String.format(MESSAGE_CORRUPT_FILE_NOT_KEPT, storage.getLaiNurseCorruptFilePath());
        }
    }

}
