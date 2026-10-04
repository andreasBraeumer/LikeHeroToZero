package de.iu.likeherotozero.configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ApplicationSettings {

    static final String ENVIRONMENT_FILE_NAME = ".env";

    private static final int MAXIMUM_PARENT_DIRECTORY_DEPTH = 6;
    private static final Logger LOGGER = Logger.getLogger(ApplicationSettings.class.getName());

    private static Map<String, String> environmentFileValues;

    private ApplicationSettings() {
    }

    public static String read(String settingName, String defaultValue) {
        String value = System.getenv(settingName);
        if (isBlank(value)) {
            value = System.getProperty(settingName);
        }
        if (isBlank(value)) {
            value = getEnvironmentFileValues().get(settingName);
        }
        if (isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

    private static synchronized Map<String, String> getEnvironmentFileValues() {
        if (environmentFileValues == null) {
            environmentFileValues = loadEnvironmentFile();
        }
        return environmentFileValues;
    }

    private static Map<String, String> loadEnvironmentFile() {
        Path environmentFile = findEnvironmentFile();
        if (environmentFile == null) {
            LOGGER.info("Keine " + ENVIRONMENT_FILE_NAME + "-Datei gefunden, es gelten Umgebungsvariablen und System-Properties.");
            return Collections.emptyMap();
        }
        try {
            List<String> lines = Files.readAllLines(environmentFile, StandardCharsets.UTF_8);
            LOGGER.info("Einstellungen aus " + environmentFile + " geladen.");
            return parseEnvironmentLines(lines);
        } catch (IOException readException) {
            LOGGER.log(Level.WARNING, "Datei " + environmentFile + " konnte nicht gelesen werden.", readException);
            return Collections.emptyMap();
        }
    }

    private static Path findEnvironmentFile() {
        Path directory = findClassLocation();
        int depth = 0;
        while (directory != null && depth <= MAXIMUM_PARENT_DIRECTORY_DEPTH) {
            Path candidate = directory.resolve(ENVIRONMENT_FILE_NAME);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
            depth++;
        }
        return null;
    }

    private static Path findClassLocation() {
        CodeSource codeSource = ApplicationSettings.class.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return null;
        }
        try {
            return Path.of(codeSource.getLocation().toURI());
        } catch (URISyntaxException | IllegalArgumentException invalidLocation) {
            return null;
        }
    }

    static Map<String, String> parseEnvironmentLines(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }
            int separatorPosition = trimmedLine.indexOf('=');
            if (separatorPosition <= 0) {
                continue;
            }
            String name = trimmedLine.substring(0, separatorPosition).trim();
            String value = trimmedLine.substring(separatorPosition + 1).trim();
            values.put(name, removeSurroundingQuotes(value));
        }
        return values;
    }

    private static String removeSurroundingQuotes(String value) {
        boolean hasDoubleQuotes = value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"");
        boolean hasSingleQuotes = value.length() >= 2 && value.startsWith("'") && value.endsWith("'");
        if (hasDoubleQuotes || hasSingleQuotes) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
