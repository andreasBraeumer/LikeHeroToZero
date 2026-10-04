package de.iu.likeherotozero.dao;

import de.iu.likeherotozero.model.Country;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class CountryDAO {

    @Inject
    private EntityManagerProvider entityManagerProvider;

    public CountryDAO() {
    }

    CountryDAO(EntityManagerProvider entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
    }

    public List<Country> findAll() {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            List<Country> countries = entityManager
                    .createQuery("select country from Country country", Country.class)
                    .getResultList();
            Collator germanCollator = Collator.getInstance(Locale.GERMAN);
            countries.sort(Comparator.comparing(Country::getName, germanCollator));
            return countries;
        } finally {
            entityManager.close();
        }
    }

    public Optional<Country> findByIsoCode(String isoCode) {
        if (isoCode == null) {
            return Optional.empty();
        }
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            Country country = entityManager.find(Country.class, isoCode);
            return Optional.ofNullable(country);
        } finally {
            entityManager.close();
        }
    }

    public void saveAll(Collection<Country> countries) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            for (Country country : countries) {
                entityManager.merge(country);
            }
            transaction.commit();
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }
}
