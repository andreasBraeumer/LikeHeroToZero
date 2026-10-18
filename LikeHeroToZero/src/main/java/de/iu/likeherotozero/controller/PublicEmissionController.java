package de.iu.likeherotozero.controller;

import de.iu.likeherotozero.dao.CountryDAO;
import de.iu.likeherotozero.dao.EmissionRecordDAO;
import de.iu.likeherotozero.model.Country;
import de.iu.likeherotozero.model.EmissionRecord;
import de.iu.likeherotozero.service.ApplicationInitializer;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

@Named
@ViewScoped
public class PublicEmissionController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private CountryDAO countryDAO;

    @Inject
    private EmissionRecordDAO emissionRecordDAO;

    @Inject
    private ApplicationInitializer applicationInitializer;

    private List<Country> countries = Collections.emptyList();
    private String selectedCountryCode;
    private Country selectedCountry;
    private EmissionRecord latestRecord;
    private List<EmissionRecord> emissionHistory = Collections.emptyList();

    @PostConstruct
    void initialize() {
        applicationInitializer.ensureInitialized();
        countries = countryDAO.findAll();
    }

    public void onPageLoad() {
        if (selectedCountryCode == null) {
            selectedCountryCode = findCountryCodeFromBrowserLanguage();
        }
        loadSelectedCountry();
    }

    public void loadSelectedCountry() {
        selectedCountry = null;
        latestRecord = null;
        emissionHistory = Collections.emptyList();

        if (selectedCountryCode == null) {
            return;
        }

        selectedCountry = findCountryByIsoCode(selectedCountryCode);
        if (selectedCountry == null) {
            selectedCountryCode = null;
            return;
        }

        emissionHistory = emissionRecordDAO.findByCountry(selectedCountryCode);
        if (!emissionHistory.isEmpty()) {
            latestRecord = emissionHistory.get(0);
        }
    }

    public boolean isNoDataAvailable() {
        return selectedCountry != null && latestRecord == null;
    }

    public boolean isHistoryAvailable() {
        return emissionHistory.size() > 1;
    }

    public EmissionRecord getPreviousRecord() {
        if (emissionHistory.size() > 1) {
            return emissionHistory.get(1);
        }
        return null;
    }

    public BigDecimal getChangePercent() {
        EmissionRecord previousRecord = getPreviousRecord();
        if (latestRecord == null || previousRecord == null) {
            return null;
        }
        BigDecimal previousValue = previousRecord.getCo2Megatonnes();
        if (previousValue.signum() == 0) {
            return null;
        }
        BigDecimal difference = latestRecord.getCo2Megatonnes().subtract(previousValue);
        return difference.multiply(BigDecimal.valueOf(100)).divide(previousValue, 1, RoundingMode.HALF_UP);
    }

    public String getChangeStyleClass() {
        BigDecimal changePercent = getChangePercent();
        if (changePercent != null && changePercent.signum() > 0) {
            return "trend-up";
        }
        return "trend-down";
    }

    private Country findCountryByIsoCode(String isoCode) {
        for (Country country : countries) {
            if (country.getIsoCode().equals(isoCode)) {
                return country;
            }
        }
        return null;
    }

    private String findCountryCodeFromBrowserLanguage() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            return null;
        }

        Iterator<Locale> requestLocales = facesContext.getExternalContext().getRequestLocales();
        while (requestLocales.hasNext()) {
            Locale requestLocale = requestLocales.next();
            if (requestLocale.getCountry().isEmpty()) {
                continue;
            }
            try {
                String isoCode = requestLocale.getISO3Country();
                if (findCountryByIsoCode(isoCode) != null) {
                    return isoCode;
                }
            } catch (MissingResourceException unknownCountryCode) {
                continue;
            }
        }
        return null;
    }

    public List<Country> getCountries() {
        return countries;
    }

    public String getSelectedCountryCode() {
        return selectedCountryCode;
    }

    public void setSelectedCountryCode(String selectedCountryCode) {
        if (selectedCountryCode == null || selectedCountryCode.isBlank()) {
            this.selectedCountryCode = null;
        } else {
            this.selectedCountryCode = selectedCountryCode.trim().toUpperCase();
        }
    }

    public Country getSelectedCountry() {
        return selectedCountry;
    }

    public EmissionRecord getLatestRecord() {
        return latestRecord;
    }

    public List<EmissionRecord> getEmissionHistory() {
        return emissionHistory;
    }

    public String getUnitLabel() {
        return EmissionRecord.UNIT_LABEL;
    }
}
