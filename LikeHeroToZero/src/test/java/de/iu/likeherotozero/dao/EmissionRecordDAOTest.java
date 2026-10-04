package de.iu.likeherotozero.dao;

import de.iu.likeherotozero.model.Country;
import de.iu.likeherotozero.model.EmissionRecord;
import de.iu.likeherotozero.model.RecordSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmissionRecordDAOTest {

    private EntityManagerProvider entityManagerProvider;
    private CountryDAO countryDAO;
    private EmissionRecordDAO emissionRecordDAO;

    @BeforeEach
    void setUp() {
        entityManagerProvider = new EntityManagerProvider(Map.of(
                "jakarta.persistence.jdbc.driver", "org.h2.Driver",
                "jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:likeherotozero;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR,VALUE",
                "jakarta.persistence.jdbc.user", "sa",
                "jakarta.persistence.jdbc.password", "",
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.show_sql", "false"));
        countryDAO = new CountryDAO(entityManagerProvider);
        emissionRecordDAO = new EmissionRecordDAO(entityManagerProvider);
        countryDAO.saveAll(List.of(
                new Country("DEU", "Deutschland"),
                new Country("FRA", "Frankreich"),
                new Country("AUT", "Österreich")));
    }

    @AfterEach
    void tearDown() {
        entityManagerProvider.close();
    }

    private static EmissionRecord createRecord(String countryIsoCode, int year, String megatonnes,
                                               RecordSource source) {
        return new EmissionRecord(new Country(countryIsoCode, null), year, new BigDecimal(megatonnes), source);
    }

    private static void assertSameValue(String expectedMegatonnes, BigDecimal actualMegatonnes) {
        assertEquals(0, new BigDecimal(expectedMegatonnes).compareTo(actualMegatonnes));
    }

    @Test
    void latestValueIsTheHighestYearOfTheCountry() {
        emissionRecordDAO.create(createRecord("DEU", 2022, "650.0", RecordSource.WORLD_BANK));
        emissionRecordDAO.create(createRecord("DEU", 2024, "579.9", RecordSource.WORLD_BANK));
        emissionRecordDAO.create(createRecord("DEU", 2023, "600.0", RecordSource.WORLD_BANK));
        emissionRecordDAO.create(createRecord("FRA", 2025, "280.0", RecordSource.WORLD_BANK));

        Optional<EmissionRecord> latestRecord = emissionRecordDAO.findLatestByCountry("DEU");

        assertTrue(latestRecord.isPresent());
        assertEquals(2024, latestRecord.get().getYear());
        assertEquals("Deutschland", latestRecord.get().getCountry().getName());
    }

    @Test
    void newlyEnteredResearchValueBecomesLatest() {
        emissionRecordDAO.create(createRecord("DEU", 2024, "579.9", RecordSource.WORLD_BANK));
        emissionRecordDAO.create(createRecord("DEU", 2025, "560.0", RecordSource.RESEARCH));

        assertEquals(2025, emissionRecordDAO.findLatestByCountry("DEU").orElseThrow().getYear());
    }

    @Test
    void countryWithoutValuesHasNoLatestValue() {
        assertFalse(emissionRecordDAO.findLatestByCountry("AUT").isPresent());
    }

    @Test
    void historyIsSortedNewestFirst() {
        emissionRecordDAO.create(createRecord("FRA", 2000, "1", RecordSource.WORLD_BANK));
        emissionRecordDAO.create(createRecord("FRA", 2010, "2", RecordSource.WORLD_BANK));
        emissionRecordDAO.create(createRecord("FRA", 2005, "3", RecordSource.WORLD_BANK));

        List<EmissionRecord> emissionHistory = emissionRecordDAO.findByCountry("FRA");

        List<Integer> years = new ArrayList<>();
        for (EmissionRecord emissionRecord : emissionHistory) {
            years.add(emissionRecord.getYear());
        }
        assertEquals(List.of(2010, 2005, 2000), years);
    }

    @Test
    void duplicateCountryAndYearIsFoundAndRejectedByDatabase() {
        emissionRecordDAO.create(createRecord("DEU", 2024, "579.9", RecordSource.WORLD_BANK));

        assertTrue(emissionRecordDAO.findIdByCountryAndYear("DEU", 2024).isPresent());
        assertFalse(emissionRecordDAO.findIdByCountryAndYear("DEU", 2023).isPresent());
        assertThrows(RuntimeException.class,
                () -> emissionRecordDAO.create(createRecord("DEU", 2024, "1.0", RecordSource.RESEARCH)));
    }

    @Test
    void updateChangesValueAndSource() {
        emissionRecordDAO.create(createRecord("DEU", 2024, "579.9", RecordSource.WORLD_BANK));
        EmissionRecord emissionRecord = emissionRecordDAO.findLatestByCountry("DEU").orElseThrow();

        emissionRecord.setCo2Megatonnes(new BigDecimal("580.5"));
        emissionRecord.setSource(RecordSource.RESEARCH);
        emissionRecordDAO.update(emissionRecord);

        EmissionRecord reloadedRecord = emissionRecordDAO.findById(emissionRecord.getId()).orElseThrow();
        assertSameValue("580.5", reloadedRecord.getCo2Megatonnes());
        assertEquals(RecordSource.RESEARCH, reloadedRecord.getSource());
    }

    @Test
    void importInsertsUpdatesAndNeverOverwritesResearchValues() {
        emissionRecordDAO.create(createRecord("DEU", 2023, "600.0", RecordSource.WORLD_BANK));
        emissionRecordDAO.create(createRecord("DEU", 2024, "999.0", RecordSource.RESEARCH));
        emissionRecordDAO.create(createRecord("FRA", 2024, "280.0", RecordSource.WORLD_BANK));

        EmissionRecordDAO.ImportCounts importCounts = emissionRecordDAO.importValues(List.of(
                createRecord("DEU", 2023, "610.0", RecordSource.WORLD_BANK),
                createRecord("DEU", 2024, "579.9", RecordSource.WORLD_BANK),
                createRecord("FRA", 2024, "280.0", RecordSource.WORLD_BANK),
                createRecord("AUT", 2024, "60.0", RecordSource.WORLD_BANK)));

        assertEquals(new EmissionRecordDAO.ImportCounts(1, 1, 1, 1), importCounts);

        List<EmissionRecord> germanHistory = emissionRecordDAO.findByCountry("DEU");
        assertSameValue("610.0", germanHistory.get(1).getCo2Megatonnes());

        EmissionRecord researchRecord = emissionRecordDAO.findLatestByCountry("DEU").orElseThrow();
        assertSameValue("999.0", researchRecord.getCo2Megatonnes());
        assertEquals(RecordSource.RESEARCH, researchRecord.getSource());
        assertEquals(4, emissionRecordDAO.count());
    }

    @Test
    void countriesAreSortedByGermanName() {
        List<String> countryNames = new ArrayList<>();
        for (Country country : countryDAO.findAll()) {
            countryNames.add(country.getName());
        }

        assertEquals(List.of("Deutschland", "Frankreich", "Österreich"), countryNames);
    }
}
