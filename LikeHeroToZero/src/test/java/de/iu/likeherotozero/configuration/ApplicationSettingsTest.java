package de.iu.likeherotozero.configuration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ApplicationSettingsTest {

    @Test
    void parsesNamesAndValues() {
        Map<String, String> values = ApplicationSettings.parseEnvironmentLines(List.of(
                "DB_USER=likeherotozero",
                "DEMO_PASSWORD = geheim=123 ",
                "DB_PORT=3306"));

        assertEquals("likeherotozero", values.get("DB_USER"));
        assertEquals("geheim=123", values.get("DEMO_PASSWORD"));
        assertEquals("3306", values.get("DB_PORT"));
    }

    @Test
    void ignoresCommentsEmptyAndInvalidLines() {
        Map<String, String> values = ApplicationSettings.parseEnvironmentLines(List.of(
                "# Kommentar",
                "",
                "OHNE_GLEICHHEITSZEICHEN",
                "=OHNE_NAME"));

        assertEquals(0, values.size());
    }

    @Test
    void removesSurroundingQuotes() {
        Map<String, String> values = ApplicationSettings.parseEnvironmentLines(List.of(
                "DOUBLE_QUOTED=\"mit Leerzeichen\"",
                "SINGLE_QUOTED='einfach'"));

        assertEquals("mit Leerzeichen", values.get("DOUBLE_QUOTED"));
        assertEquals("einfach", values.get("SINGLE_QUOTED"));
        assertFalse(values.containsKey("UNBEKANNT"));
    }
}
