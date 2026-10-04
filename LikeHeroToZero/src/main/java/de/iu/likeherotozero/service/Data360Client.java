package de.iu.likeherotozero.service;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Data360Client {

    public static final String BASE_URL = "https://data360api.worldbank.org";
    public static final String DATABASE_ID = "WB_WDI";
    public static final String INDICATOR_CO2_MEGATONNES = "WB_WDI_EN_GHG_CO2_MT_CE_AR5";
    public static final String UNIT_MEGATONNES_CO2_EQUIVALENT = "MTCO2E";

    private static final int MAXIMUM_PAGE_COUNT = 100;

    public record Observation(String areaCode, int year, BigDecimal value, String unitOfMeasure) {
    }

    record Page(int totalCount, int entryCount, List<Observation> observations) {
    }

    private final HttpClient httpClient;
    private final String baseUrl;

    public Data360Client() {
        this(BASE_URL);
    }

    Data360Client(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<Observation> fetchAll(String indicator) throws IOException, InterruptedException {
        List<Observation> allObservations = new ArrayList<>();
        int skippedEntries = 0;
        for (int pageNumber = 0; pageNumber < MAXIMUM_PAGE_COUNT; pageNumber++) {
            String responseBody = requestPage(indicator, skippedEntries);
            Page page = parsePage(responseBody);
            allObservations.addAll(page.observations());
            skippedEntries = skippedEntries + page.entryCount();

            boolean pageWasEmpty = page.entryCount() == 0;
            boolean allEntriesLoaded = skippedEntries >= page.totalCount();
            if (pageWasEmpty || allEntriesLoaded) {
                break;
            }
        }
        return allObservations;
    }

    private String requestPage(String indicator, int skippedEntries) throws IOException, InterruptedException {
        URI requestUri = URI.create(baseUrl + "/data360/data?DATABASE_ID=" + DATABASE_ID
                                    + "&INDICATOR=" + indicator + "&skip=" + skippedEntries);
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Data360 API antwortet mit HTTP " + response.statusCode() + " für " + requestUri);
        }
        return response.body();
    }

    static Page parsePage(String json) {
        try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
            JsonObject rootObject = jsonReader.readObject();
            JsonArray entries = rootObject.getJsonArray("value");

            List<Observation> observations = new ArrayList<>();
            int entryCount = 0;
            if (entries != null) {
                entryCount = entries.size();
                for (JsonValue entryValue : entries) {
                    if (entryValue.getValueType() != JsonValue.ValueType.OBJECT) {
                        continue;
                    }
                    JsonObject entry = entryValue.asJsonObject();
                    String areaCode = readText(entry, "REF_AREA");
                    Integer year = parseYear(readText(entry, "TIME_PERIOD"));
                    if (areaCode == null || year == null) {
                        continue;
                    }
                    BigDecimal value = parseDecimal(readText(entry, "OBS_VALUE"));
                    String unitOfMeasure = readText(entry, "UNIT_MEASURE");
                    observations.add(new Observation(areaCode.trim().toUpperCase(), year, value, unitOfMeasure));
                }
            }

            int totalCount = entryCount;
            boolean hasNumericCount = rootObject.containsKey("count")
                                      && rootObject.get("count").getValueType() == JsonValue.ValueType.NUMBER;
            if (hasNumericCount) {
                totalCount = rootObject.getInt("count");
            }
            return new Page(totalCount, entryCount, observations);
        }
    }

    private static String readText(JsonObject entry, String fieldName) {
        JsonValue fieldValue = entry.get(fieldName);
        if (fieldValue == null || fieldValue.getValueType() == JsonValue.ValueType.NULL) {
            return null;
        }
        if (fieldValue instanceof JsonString) {
            String text = ((JsonString) fieldValue).getString().trim();
            if (text.isEmpty()) {
                return null;
            }
            return text;
        }
        if (fieldValue instanceof JsonNumber) {
            return ((JsonNumber) fieldValue).bigDecimalValue().toPlainString();
        }
        return null;
    }

    private static Integer parseYear(String text) {
        if (text == null) {
            return null;
        }
        String yearText = text;
        if (yearText.length() > 4) {
            yearText = yearText.substring(0, 4);
        }
        try {
            return Integer.valueOf(yearText);
        } catch (NumberFormatException invalidYear) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String text) {
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException invalidNumber) {
            return null;
        }
    }
}
