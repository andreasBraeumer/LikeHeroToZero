package de.iu.likeherotozero.service;

import de.iu.likeherotozero.dao.CountryDAO;
import de.iu.likeherotozero.dao.EmissionRecordDAO;
import de.iu.likeherotozero.dao.EmissionRecordDAO.ImportCounts;
import de.iu.likeherotozero.model.Country;
import de.iu.likeherotozero.model.EmissionRecord;
import de.iu.likeherotozero.model.RecordSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class DataImportService {

    public record ImportResult(int countryCount, int insertedRecords, int updatedRecords, int unchangedRecords,
                               int keptResearchRecords, int skippedAreaCount) {

        public String summary() {
            return String.format(Locale.GERMAN,
                    "%d Länder, %d Werte neu, %d aktualisiert, %d unverändert, %d Forschungswerte beibehalten, "
                    + "%d Regionen/Aggregate übersprungen",
                    countryCount, insertedRecords, updatedRecords, unchangedRecords, keptResearchRecords,
                    skippedAreaCount);
        }
    }

    private static final Map<String, String> ADDITIONAL_COUNTRIES = Map.of("XKX", "Kosovo");

    private static final Logger LOGGER = Logger.getLogger(DataImportService.class.getName());

    private static final Map<String, String> COUNTRY_NAMES_BY_ISO_CODE = buildCountryNames();

    @Inject
    private CountryDAO countryDAO;

    @Inject
    private EmissionRecordDAO emissionRecordDAO;

    private final Data360Client data360Client = new Data360Client();

    public void importIfEmpty() {
        try {
            long existingRecordCount = emissionRecordDAO.count();
            if (existingRecordCount > 0) {
                LOGGER.info("Import übersprungen, es sind bereits " + existingRecordCount + " Emissionswerte vorhanden.");
                return;
            }
            LOGGER.info("Keine Emissionsdaten vorhanden, starte Erstimport aus der World Bank Data360 API ...");
            ImportResult importResult = importFromApi();
            LOGGER.info("Erstimport abgeschlossen: " + importResult.summary());
        } catch (IOException connectionException) {
            LOGGER.log(Level.SEVERE, "Data360 API nicht erreichbar, Anwendung startet ohne Importdaten: "
                                     + connectionException.getMessage(), connectionException);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            LOGGER.warning("Import wurde unterbrochen.");
        } catch (RuntimeException unexpectedException) {
            LOGGER.log(Level.SEVERE, "Import fehlgeschlagen", unexpectedException);
        }
    }

    public synchronized ImportResult importFromApi() throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        List<Data360Client.Observation> observations = data360Client.fetchAll(Data360Client.INDICATOR_CO2_MEGATONNES);
        long durationInMilliseconds = System.currentTimeMillis() - startTime;
        LOGGER.info(observations.size() + " Datenpunkte von der Data360 API empfangen (" + durationInMilliseconds + " ms).");

        Map<String, Country> countriesByIsoCode = new LinkedHashMap<>();
        Map<String, EmissionRecord> recordsByKey = new HashMap<>();
        Set<String> skippedAreaCodes = new HashSet<>();

        for (Data360Client.Observation observation : observations) {
            String areaCode = observation.areaCode();
            String countryName = countryName(areaCode);
            if (countryName == null) {
                skippedAreaCodes.add(areaCode);
                continue;
            }

            if (!countriesByIsoCode.containsKey(areaCode)) {
                countriesByIsoCode.put(areaCode, new Country(areaCode, countryName));
            }

            if (!isUsableValue(observation)) {
                continue;
            }

            Country country = countriesByIsoCode.get(areaCode);
            EmissionRecord emissionRecord = new EmissionRecord(country, observation.year(), observation.value(),
                    RecordSource.WORLD_BANK);
            recordsByKey.put(areaCode + ":" + observation.year(), emissionRecord);
        }

        countryDAO.saveAll(countriesByIsoCode.values());
        ImportCounts importCounts = emissionRecordDAO.importValues(recordsByKey.values());
        return new ImportResult(countriesByIsoCode.size(), importCounts.insertedRecords(),
                importCounts.updatedRecords(), importCounts.unchangedRecords(), importCounts.keptResearchRecords(),
                skippedAreaCodes.size());
    }

    private static boolean isUsableValue(Data360Client.Observation observation) {
        if (observation.value() == null) {
            return false;
        }
        if (observation.value().signum() < 0) {
            return false;
        }
        if (observation.unitOfMeasure() == null) {
            return true;
        }
        return Data360Client.UNIT_MEGATONNES_CO2_EQUIVALENT.equalsIgnoreCase(observation.unitOfMeasure());
    }

    static String countryName(String isoCode) {
        if (isoCode == null) {
            return null;
        }
        return COUNTRY_NAMES_BY_ISO_CODE.get(isoCode.toUpperCase(Locale.ROOT));
    }

    private static Map<String, String> buildCountryNames() {
        Map<String, String> countryNames = new HashMap<>(ADDITIONAL_COUNTRIES);
        for (String twoLetterCode : Locale.getISOCountries()) {
            Locale countryLocale = new Locale.Builder().setRegion(twoLetterCode).build();
            try {
                String threeLetterCode = countryLocale.getISO3Country();
                if (!threeLetterCode.isEmpty()) {
                    countryNames.put(threeLetterCode, countryLocale.getDisplayCountry(Locale.GERMAN));
                }
            } catch (MissingResourceException unknownThreeLetterCode) {
                LOGGER.fine("Kein dreistelliger Ländercode für " + twoLetterCode);
            }
        }
        return Collections.unmodifiableMap(countryNames);
    }
}
