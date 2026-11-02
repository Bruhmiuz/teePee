package seedu.address.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.ReadOnlyLaiNurse;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;

/**
 * API of the Storage component
 */
public interface Storage {

    /**
     * Returns the file path of the UserPrefs data file.
     */
    Path getUserPrefsFilePath();

    /**
     * Returns UserPrefs data from storage.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if the loading of data from preference file failed.
     */
    Optional<UserPrefs> readUserPrefs() throws DataLoadingException;

    /**
     * Saves the given {@link seedu.address.model.ReadOnlyUserPrefs} to the storage.
     * @param userPrefs cannot be null.
     * @throws IOException if there was any problem writing to the file.
     */
    void saveUserPrefs(ReadOnlyUserPrefs userPrefs) throws IOException;

    /**
     * Returns the file path of the LaiNurse data file.
     */
    Path getLaiNurseFilePath();

    /**
     * Returns LaiNurse data as a {@link ReadOnlyLaiNurse}.
     * Returns {@code Optional.empty()} if storage file is not found.
     *
     * @throws DataLoadingException if loading the data from storage failed.
     */
    Optional<ReadOnlyLaiNurse> readLaiNurse() throws DataLoadingException;

    /**
     * Returns the file path of the backup of the LaiNurse data file.
     */
    Path getLaiNurseBackupFilePath();

    /**
     * Returns the file path that a LaiNurse data file that cannot be loaded is copied to.
     */
    Path getLaiNurseCorruptFilePath();

    /**
     * Returns the file path that the backup is copied to when the LaiNurse data file is missing.
     */
    Path getLaiNursePreviousFilePath();

    /**
     * Returns the LaiNurse data in the backup file as a {@link ReadOnlyLaiNurse}.
     * Returns {@code Optional.empty()} if the backup file is not found.
     *
     * @throws DataLoadingException if loading the data from the backup failed.
     */
    Optional<ReadOnlyLaiNurse> readLaiNurseBackup() throws DataLoadingException;

    /**
     * Saves the given {@link ReadOnlyLaiNurse} to the data file, and then to its backup.
     * @param laiNurse cannot be null.
     * @throws IOException if there was any problem writing to either file.
     */
    void saveLaiNurse(ReadOnlyLaiNurse laiNurse) throws IOException;

    /**
     * Copies the LaiNurse data file to {@link #getLaiNurseCorruptFilePath()}, replacing any earlier copy.
     * @throws IOException if there was any problem copying the file.
     */
    void keepCorruptLaiNurseFile() throws IOException;

    /**
     * Copies the backup of the LaiNurse data file to {@link #getLaiNursePreviousFilePath()}, replacing any earlier
     * copy.
     * @throws IOException if there was any problem copying the file.
     */
    void keepLaiNurseBackup() throws IOException;

}
