package extractors;

import JsonDTO.CaseFile;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseLoader {

  private static final Logger logger = LoggerFactory.getLogger(CaseLoader.class);

  private static final ObjectMapper mapper =
          new ObjectMapper()
                  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private CaseLoader() {
  }

  public static List<CaseFile> loadCases(String directoryPath) {
    List<CaseFile> cases = new ArrayList<>();

    // --- 1. Load built-in cases from inside the JAR/resources ---
    try {
      URI uri = CaseLoader.class.getClassLoader().getResource(directoryPath).toURI();
      if (uri.getScheme().equals("jar")) {
        try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
          Path casesPath = fileSystem.getPath(directoryPath);
          try (Stream<Path> paths = Files.walk(casesPath, 1)) {
            paths.filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".json"))
                    .forEach(path -> {
                      try (InputStream is = Files.newInputStream(path)) {
                        CaseFile caseFile = mapper.readValue(is, CaseFile.class);
                        // MODIFIED: Use getUniversalTitle() for validation
                        if (caseFile.getUniversalTitle() != null && !caseFile.getUniversalTitle().isBlank()) {
                          cases.add(caseFile);
                        }
                      } catch (IOException e) {
                        logger.error("Error reading or parsing case file from JAR: {}", path, e);
                      }
                    });
          }
        }
      } else { // Running from IDE, resources are plain files
        Path casesPath = Paths.get(uri);
        loadCasesFromDirectory(casesPath, cases);
      }
    } catch (IOException | URISyntaxException | NullPointerException e) {
      logger.warn("Could not load built-in cases from resource directory: '{}'. This is normal if the directory is empty or missing.", directoryPath);
    }

    // --- 2. Load external cases from a folder next to the JAR ---
    Path externalCasesDir = Paths.get(directoryPath);
    if (Files.exists(externalCasesDir) && Files.isDirectory(externalCasesDir)) {
      logger.info("Found external cases directory. Loading additional cases...");
      loadCasesFromDirectory(externalCasesDir, cases);
    }

    logger.info("Finished loading cases. Found {} valid case(s).", cases.size());
    return cases;
  }

  private static void loadCasesFromDirectory(Path dir, List<CaseFile> cases) {
    try (Stream<Path> stream = Files.list(dir)) {
      stream
              .filter(path -> !Files.isDirectory(path))
              .filter(path -> path.toString().toLowerCase().endsWith(".json"))
              .forEach(filePath -> {
                File file = filePath.toFile();
                try {
                  CaseFile caseFile = mapper.readValue(file, CaseFile.class);
                  // MODIFIED: Use getUniversalTitle() for validation and duplicate checking
                  if (caseFile.getUniversalTitle() != null && !caseFile.getUniversalTitle().isBlank() &&
                          cases.stream().noneMatch(c -> c.getUniversalTitle().equalsIgnoreCase(caseFile.getUniversalTitle()))) {
                      boolean allLocalizationsValid = caseFile.getLocalizations().values().stream()
                              .allMatch(loc -> loc.getFinalExam() != null && loc.getFinalExam().getQuestions() != null && !loc.getFinalExam().getQuestions().isEmpty());
                      if (allLocalizationsValid) {
                          cases.add(caseFile);
                      } else {
                          logger.warn("Skipping case file '{}': Invalid structure (missing or empty final_exam).", file.getName());
                      }
                  } else if (caseFile.getUniversalTitle() == null || caseFile.getUniversalTitle().isBlank()) {
                    logger.warn("Skipping case file '{}': Invalid structure (missing universal_title).", file.getName());
                  }
                } catch (IOException e) {
                  logger.error("Error reading or parsing external case file '{}'", file.getName(), e);
                }
              });
    } catch (IOException e) {
      logger.error("Error listing files in external case directory '{}'", dir, e);
    }
  }
}