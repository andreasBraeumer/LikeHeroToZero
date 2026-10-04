package de.iu.likeherotozero.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "emission_record",
       uniqueConstraints = @UniqueConstraint(name = "uk_emission_country_year",
                                             columnNames = {"country_iso3", "year"}))
public class EmissionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String UNIT_LABEL = "Mt CO₂e";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "country_iso3", nullable = false)
    private Country country;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "co2_mt", nullable = false, precision = 14, scale = 4)
    private BigDecimal co2Megatonnes;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20, nullable = false, columnDefinition = "varchar(20)")
    private RecordSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    public EmissionRecord() {
    }

    public EmissionRecord(Country country, Integer year, BigDecimal co2Megatonnes, RecordSource source) {
        this.country = country;
        this.year = year;
        this.co2Megatonnes = co2Megatonnes;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getCo2Megatonnes() {
        return co2Megatonnes;
    }

    public void setCo2Megatonnes(BigDecimal co2Megatonnes) {
        this.co2Megatonnes = co2Megatonnes;
    }

    public RecordSource getSource() {
        return source;
    }

    public void setSource(RecordSource source) {
        this.source = source;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(User changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getUnitLabel() {
        return UNIT_LABEL;
    }

    @Override
    public String toString() {
        String countryIsoCode = null;
        if (country != null) {
            countryIsoCode = country.getIsoCode();
        }
        return "EmissionRecord[" + id + ", " + countryIsoCode + ", " + year + ", " + co2Megatonnes + "]";
    }
}
