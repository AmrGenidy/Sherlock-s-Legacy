package singleplayer.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import singleplayer.SinglePlayerMain;

public class CaseFileUtil {

    /**
     * Gets a list of available case files from the external cases directory.
     *
     * @return A list of File objects, or an empty list if the directory doesn't exist.
     */
    public static List<File> getAvailableCaseFiles() {
        Path externalCasesDir = Paths.get(SinglePlayerMain.CASES_DIRECTORY);
        if (!Files.isDirectory(externalCasesDir)) {
            return Collections.emptyList();
        }

        try (Stream<Path> stream = Files.list(externalCasesDir)) {
            return stream
                    .filter(path -> !Files.isDirectory(path) && path.toString().toLowerCase().endsWith(".json"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading case files: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Adds a new case file from a given path to the application's cases directory.
     *
     * @param sourcePathStr The full path to the source .json file.
     * @return A status message indicating success or failure.
     */
    public static String addCaseFile(String sourcePathStr) {
        Path sourcePath = Paths.get(sourcePathStr);

        // 1. Validate source file
        if (!Files.exists(sourcePath)) {
            return "Error: Source file does not exist at '" + sourcePathStr + "'.";
        }
        if (Files.isDirectory(sourcePath)) {
            return "Error: The provided path is a directory, not a file.";
        }
        if (!sourcePathStr.toLowerCase().endsWith(".json")) {
            return "Error: The file must be a .json file.";
        }

        // 2. Prepare destination directory
        Path destDir = Paths.get(SinglePlayerMain.CASES_DIRECTORY);
        try {
            Files.createDirectories(destDir);
        } catch (IOException e) {
            return "Error: Could not create cases directory: " + e.getMessage();
        }

        // 3. Copy the file
        Path destPath = destDir.resolve(sourcePath.getFileName());
        if (Files.exists(destPath)) {
            return "Warning: A case with the name '" + sourcePath.getFileName() + "' already exists. File not copied.";
        }

        try {
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
            return "Success: Case '" + sourcePath.getFileName() + "' added successfully.";
        } catch (IOException e) {
            return "Error: Failed to copy file: " + e.getMessage();
        }
    }
}
