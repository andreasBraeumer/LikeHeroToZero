package de.iu.likeherotozero.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DataImportServiceTest {

    @Test
    void isoCountriesGetGermanNames() {
        assertEquals("Deutschland", DataImportService.countryName("DEU"));
        assertEquals("Frankreich", DataImportService.countryName("FRA"));
        assertEquals("Vereinigte Staaten", DataImportService.countryName("USA"));
    }

    @Test
    void regionsAndAggregatesAreNotCountries() {
        assertNull(DataImportService.countryName("AFE"));
        assertNull(DataImportService.countryName("WLD"));
        assertNull(DataImportService.countryName("EUU"));
        assertNull(DataImportService.countryName(null));
    }

    @Test
    void kosovoIsAddedManually() {
        assertEquals("Kosovo", DataImportService.countryName("XKX"));
    }
}
