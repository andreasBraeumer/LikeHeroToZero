package de.iu.likeherotozero.dao;

import de.iu.likeherotozero.model.Country;
import de.iu.likeherotozero.model.EmissionRecord;
import de.iu.likeherotozero.model.RecordSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class EmissionRecordDAO {

    public record ImportCounts(int insertedRecords, int updatedRecords, int unchangedRecords,
                               int keptResearchRecords) {
    }

    @Inject
    private EntityManagerProvider entityManagerProvider;

    public EmissionRecordDAO() {
    }

    EmissionRecordDAO(EntityManagerProvider entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
    }

    public Optional<EmissionRecord> findLatestByCountry(String countryIsoCode) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            List<EmissionRecord> latestRecords = entityManager
                    .createQuery("select emissionRecord from EmissionRecord emissionRecord"
                                 + " join fetch emissionRecord.country"
                                 + " where emissionRecord.country.isoCode = :countryIsoCode"
                                 + " order by emissionRecord.year desc", EmissionRecord.class)
                    .setParameter("countryIsoCode", countryIsoCode)
                    .setMaxResults(1)
                    .getResultList();
            if (latestRecords.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(latestRecords.get(0));
        } finally {
            entityManager.close();
        }
    }

    public List<EmissionRecord> findByCountry(String countryIsoCode) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            return entityManager
                    .createQuery("select emissionRecord from EmissionRecord emissionRecord"
                                 + " join fetch emissionRecord.country"
                                 + " where emissionRecord.country.isoCode = :countryIsoCode"
                                 + " order by emissionRecord.year desc", EmissionRecord.class)
                    .setParameter("countryIsoCode", countryIsoCode)
                    .getResultList();
        } finally {
            entityManager.close();
        }
    }

    public List<EmissionRecord> findAll(String countryIsoCodeFilter) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            String queryText = "select emissionRecord from EmissionRecord emissionRecord"
                               + " join fetch emissionRecord.country"
                               + " left join fetch emissionRecord.changedBy";
            if (countryIsoCodeFilter != null) {
                queryText = queryText + " where emissionRecord.country.isoCode = :countryIsoCode";
            }
            queryText = queryText + " order by emissionRecord.country.name, emissionRecord.year desc";

            TypedQuery<EmissionRecord> query = entityManager.createQuery(queryText, EmissionRecord.class);
            if (countryIsoCodeFilter != null) {
                query.setParameter("countryIsoCode", countryIsoCodeFilter);
            }
            return query.getResultList();
        } finally {
            entityManager.close();
        }
    }

    public Optional<EmissionRecord> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            List<EmissionRecord> foundRecords = entityManager
                    .createQuery("select emissionRecord from EmissionRecord emissionRecord"
                                 + " join fetch emissionRecord.country"
                                 + " left join fetch emissionRecord.changedBy"
                                 + " where emissionRecord.id = :id", EmissionRecord.class)
                    .setParameter("id", id)
                    .getResultList();
            if (foundRecords.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(foundRecords.get(0));
        } finally {
            entityManager.close();
        }
    }

    public Optional<Long> findIdByCountryAndYear(String countryIsoCode, int year) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            List<Long> foundIds = entityManager
                    .createQuery("select emissionRecord.id from EmissionRecord emissionRecord"
                                 + " where emissionRecord.country.isoCode = :countryIsoCode"
                                 + " and emissionRecord.year = :year", Long.class)
                    .setParameter("countryIsoCode", countryIsoCode)
                    .setParameter("year", year)
                    .getResultList();
            if (foundIds.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(foundIds.get(0));
        } finally {
            entityManager.close();
        }
    }

    public long count() {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            return entityManager
                    .createQuery("select count(emissionRecord) from EmissionRecord emissionRecord", Long.class)
                    .getSingleResult();
        } finally {
            entityManager.close();
        }
    }

    public void create(EmissionRecord emissionRecord) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            String countryIsoCode = emissionRecord.getCountry().getIsoCode();
            emissionRecord.setCountry(entityManager.getReference(Country.class, countryIsoCode));
            entityManager.persist(emissionRecord);
            transaction.commit();
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    public EmissionRecord update(EmissionRecord emissionRecord) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            String countryIsoCode = emissionRecord.getCountry().getIsoCode();
            emissionRecord.setCountry(entityManager.getReference(Country.class, countryIsoCode));
            EmissionRecord mergedRecord = entityManager.merge(emissionRecord);
            transaction.commit();
            return mergedRecord;
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    public ImportCounts importValues(Collection<EmissionRecord> importedRecords) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();

            Map<String, EmissionRecord> existingRecordsByKey = new HashMap<>();
            List<EmissionRecord> existingRecords = entityManager
                    .createQuery("select emissionRecord from EmissionRecord emissionRecord", EmissionRecord.class)
                    .getResultList();
            for (EmissionRecord existingRecord : existingRecords) {
                String key = createKey(existingRecord.getCountry().getIsoCode(), existingRecord.getYear());
                existingRecordsByKey.put(key, existingRecord);
            }

            int insertedRecords = 0;
            int updatedRecords = 0;
            int unchangedRecords = 0;
            int keptResearchRecords = 0;
            LocalDateTime importTime = LocalDateTime.now();

            for (EmissionRecord importedRecord : importedRecords) {
                String countryIsoCode = importedRecord.getCountry().getIsoCode();
                String key = createKey(countryIsoCode, importedRecord.getYear());
                EmissionRecord existingRecord = existingRecordsByKey.get(key);

                if (existingRecord == null) {
                    Country countryReference = entityManager.getReference(Country.class, countryIsoCode);
                    EmissionRecord newRecord = new EmissionRecord(countryReference, importedRecord.getYear(),
                            importedRecord.getCo2Megatonnes(), RecordSource.WORLD_BANK);
                    newRecord.setChangedAt(importTime);
                    entityManager.persist(newRecord);
                    existingRecordsByKey.put(key, newRecord);
                    insertedRecords++;
                } else if (existingRecord.getSource() == RecordSource.RESEARCH) {
                    keptResearchRecords++;
                } else if (existingRecord.getCo2Megatonnes().compareTo(importedRecord.getCo2Megatonnes()) != 0) {
                    existingRecord.setCo2Megatonnes(importedRecord.getCo2Megatonnes());
                    existingRecord.setChangedAt(importTime);
                    updatedRecords++;
                } else {
                    unchangedRecords++;
                }
            }

            transaction.commit();
            return new ImportCounts(insertedRecords, updatedRecords, unchangedRecords, keptResearchRecords);
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    private static String createKey(String countryIsoCode, int year) {
        return countryIsoCode + ":" + year;
    }
}
