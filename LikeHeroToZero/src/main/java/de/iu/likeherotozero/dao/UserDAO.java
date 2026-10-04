package de.iu.likeherotozero.dao;

import de.iu.likeherotozero.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserDAO {

    @Inject
    private EntityManagerProvider entityManagerProvider;

    public UserDAO() {
    }

    UserDAO(EntityManagerProvider entityManagerProvider) {
        this.entityManagerProvider = entityManagerProvider;
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            List<User> users = entityManager
                    .createQuery("select user from User user where user.username = :username", User.class)
                    .setParameter("username", username)
                    .getResultList();
            if (users.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(users.get(0));
        } finally {
            entityManager.close();
        }
    }

    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        try {
            User user = entityManager.find(User.class, id);
            return Optional.ofNullable(user);
        } finally {
            entityManager.close();
        }
    }

    public User update(User user) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            User mergedUser = entityManager.merge(user);
            transaction.commit();
            return mergedUser;
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }

    public void create(User user) {
        EntityManager entityManager = entityManagerProvider.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.persist(user);
            transaction.commit();
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            entityManager.close();
        }
    }
}
