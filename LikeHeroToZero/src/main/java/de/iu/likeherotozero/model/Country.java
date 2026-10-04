package de.iu.likeherotozero.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "country")
public class Country implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "iso3", length = 3, nullable = false)
    private String isoCode;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    public Country() {
    }

    public Country(String isoCode, String name) {
        this.isoCode = isoCode;
        this.name = name;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public void setIsoCode(String isoCode) {
        this.isoCode = isoCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Country)) {
            return false;
        }
        Country otherCountry = (Country) other;
        if (isoCode == null) {
            return false;
        }
        return isoCode.equals(otherCountry.getIsoCode());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(isoCode);
    }

    @Override
    public String toString() {
        return name + " (" + isoCode + ")";
    }
}
