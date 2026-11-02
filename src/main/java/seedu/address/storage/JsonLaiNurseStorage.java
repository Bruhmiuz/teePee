package seedu.address.storage;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Logger;

import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.FileUtil;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.ReadOnlyLaiNurse;

/**
 * A class to access LaiNurse data stored as a JSON file on the hard disk.
 * Every save also writes a backup next to the data file, such as {@code lainurse.backup.json} for
 * {@code lainurse.json}, and a data file that cannot be loaded can be kept as a copy, such as
 * {@code lainurse.corrupt.json}.
 */
public class JsonLaiNurseStorage {

    private static final Logger logger = LogsCenter.getLogger(JsonLaiNurseStorage.class);

    private final Path filePath;
    private final Path backupFilePath;
    private final Path corruptFilePath;
    private final Path previousFilePath;

    /**
     * Creates a {@code JsonLaiNurseStorage} for the data file at {@code filePath}, with its backup, corrupt copy and
     * previous backup in the same folder.
     */
    public JsonLaiNurseStorage(Path filePath) {
        requireNonNull(filePath);
        this.filePath = filePath;
        this.backupFilePath = withSuffix(filePath, "backup");
        this.corruptFilePath = withSuffix(filePath, "corrupt");
        this.previousFilePath = withSuffix(filePath, "previous");
        // A save writes the data file and then the backup, so the backup must never be the data file itself
        assert !backupFilePath.equals(filePath) && !corruptFilePath.equals(filePath);
    }

    /**
     * Returns the path of a file next to {@code filePath} with {@code suffix} added before its extension,
     * such as {@code data/lainurse.backup.json} for {@code data/lainurse.json} and {@code backup}.
     */
    static Path withSuffix(Path filePath, String suffix) {
        String fileName = filePath.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');
        String suffixedName = extensionStart > 0
                ? fileName.substring(0, extensionStart) + "." + suffix + fileName.substring(extensionStart)
                : fileName + "." + suffix;
        assert !suffixedName.equals(fileName) : "A suffixed file must not be the file itself.";
        return filePath.resolveSibling(suffixedName);
    }

    public Path getLaiNurseFilePath() {
        return filePath;
    }

    public Path getLaiNurseBackupFilePath() {
        return backupFilePath;
    }

    public Path getLaiNurseCorruptFilePath() {
        return corruptFilePath;
    }

    /**
     * Returns LaiNurse data as a {@link ReadOnlyLaiNurse}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyLaiNurse> readLaiNurse() throws DataLoadingException {
        return readLaiNurse(filePath);
    }

    /**
     * Similar to {@link #readLaiNurse()}.
     *
     * @param filePath location of the data. Cannot be null.
     * @throws DataLoadingException if loading the data from storage failed.
     */
    public Optional<ReadOnlyLaiNurse> readLaiNurse(Path filePath) throws DataLoadingException {
        requireNonNull(filePath);

        Optional<JsonSerializableLaiNurse> jsonLaiNurse = JsonUtil.readJsonFile(
                filePath, JsonSerializableLaiNurse.class);
        if (!jsonLaiNurse.isPresent()) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonLaiNurse.get().toModelType());
        } catch (IllegalValueException ive) {
            logger.info("Illegal values found in " + filePath + ": " + ive.getMessage());
            throw new DataLoadingException(ive);
        }
    }

    /**
     * Returns the LaiNurse data in the backup file, as {@link #readLaiNurse()} does for the data file.
     *
     * @throws DataLoadingException if loading the data from the backup failed.
     */
    public Optional<ReadOnlyLaiNurse> readLaiNurseBackup() throws DataLoadingException {
        return readLaiNurse(backupFilePath);
    }

    /**
     * Saves the given {@link ReadOnlyLaiNurse} to the data file, and then to the backup.
     * The backup is written only once the data file has been, so it always holds data that was saved.
     * @param laiNurse cannot be null.
     * @throws BackupFailedException if the data file was saved, but the backup could not be.
     * @throws IOException if there was any problem writing to the data file.
     */
    public void saveLaiNurse(ReadOnlyLaiNurse laiNurse) throws IOException {
        saveLaiNurse(laiNurse, filePath);
        try {
            saveLaiNurse(laiNurse, backupFilePath);
        } catch (IOException e) {
            logger.warning("Saved data to " + filePath + " but could not back it up to " + backupFilePath + ": " + e);
            throw new BackupFailedException(backupFilePath, e);
        }
        logger.fine("Saved data to " + filePath + " and backed it up to " + backupFilePath);
    }

    /**
     * Similar to {@link #saveLaiNurse(ReadOnlyLaiNurse)}.
     *
     * @param filePath location of the data. Cannot be null.
     */
    public void saveLaiNurse(ReadOnlyLaiNurse laiNurse, Path filePath) throws IOException {
        requireNonNull(laiNurse);
        requireNonNull(filePath);

        FileUtil.createIfMissing(filePath);
        JsonUtil.saveJsonFile(new JsonSerializableLaiNurse(laiNurse), filePath);
    }

    /**
     * Copies the data file to the corrupt copy, replacing any earlier copy, so that the next save does not destroy
     * what was in it.
     * @throws IOException if there was any problem copying the file.
     */
    public void keepCorruptLaiNurseFile() throws IOException {
        Files.copy(filePath, corruptFilePath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Returns the file path that the backup is copied to when the data file is missing.
     */
    public Path getLaiNursePreviousFilePath() {
        return previousFilePath;
    }

    /**
     * Copies the backup to the previous backup, replacing any earlier copy, so that the next save does not destroy
     * what was in it.
     * @throws IOException if there was any problem copying the file.
     */
    public void keepLaiNurseBackup() throws IOException {
        Files.copy(backupFilePath, previousFilePath, StandardCopyOption.REPLACE_EXISTING);
    }

}
