package de.iu.likeherotozero.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Data360ClientTest {

    @Test
    void parsesStringValuesAndYears() {
        String json = """
                {
                  "count": 55,
                  "value": [
                    {"DATABASE_ID": "WB_WDI", "INDICATOR": "WB_WDI_EN_GHG_CO2_MT_CE_AR5", "REF_AREA": "DEU",
                     "TIME_PERIOD": "2024", "OBS_VALUE": "579.9356", "UNIT_MEASURE": "MTCO2E", "LATEST_DATA": true},
                    {"REF_AREA": "DEU", "TIME_PERIOD": "1990", "OBS_VALUE": "1052.1", "UNIT_MEASURE": "MTCO2E"}
                  ]
                }
                """;

        Data360Client.Page page = Data360Client.parsePage(json);

        assertEquals(55, page.totalCount());
        assertEquals(2, page.entryCount());
        Data360Client.Observation firstObservation = page.observations().get(0);
        assertEquals("DEU", firstObservation.areaCode());
        assertEquals(2024, firstObservation.year());
        assertEquals(new BigDecimal("579.9356"), firstObservation.value());
        assertEquals("MTCO2E", firstObservation.unitOfMeasure());
    }

    @Test
    void emptyOrMissingValueBecomesNullAndInvalidEntriesAreSkipped() {
        String json = """
                {"count": 4, "value": [
                  {"REF_AREA": "ABW", "TIME_PERIOD": "2020", "OBS_VALUE": ""},
                  {"REF_AREA": "ABW", "TIME_PERIOD": "2021", "OBS_VALUE": null},
                  {"REF_AREA": "ABW", "TIME_PERIOD": "kein Jahr", "OBS_VALUE": "1.0"},
                  {"TIME_PERIOD": "2022", "OBS_VALUE": "1.0"}
                ]}
                """;

        Data360Client.Page page = Data360Client.parsePage(json);

        assertEquals(4, page.entryCount());
        assertEquals(2, page.observations().size());
        assertNull(page.observations().get(0).value());
        assertNull(page.observations().get(1).value());
    }

    @Test
    void acceptsNumericJsonValues() {
        String json = """
                {"count": 1, "value": [{"REF_AREA": "fra", "TIME_PERIOD": 2023, "OBS_VALUE": 290.5}]}
                """;

        Data360Client.Observation observation = Data360Client.parsePage(json).observations().get(0);

        assertEquals("FRA", observation.areaCode());
        assertEquals(2023, observation.year());
        assertEquals(0, new BigDecimal("290.5").compareTo(observation.value()));
    }

    @Test
    void emptyResponse() {
        Data360Client.Page page = Data360Client.parsePage("{\"count\": 0, \"value\": []}");

        assertEquals(0, page.totalCount());
        assertEquals(0, page.entryCount());
    }
}
