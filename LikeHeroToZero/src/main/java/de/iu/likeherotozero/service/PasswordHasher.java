package de.iu.likeherotozero.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {

    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_FORMAT_PREFIX = "pbkdf2";
    private static final int ITERATION_COUNT = 210_000;
    private static final int SALT_LENGTH_IN_BYTES = 16;
    private static final int HASH_LENGTH_IN_BITS = 256;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String HASH_FOR_UNKNOWN_USERS = hash("placeholder-password-for-unknown-users");

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_LENGTH_IN_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = deriveKey(password.toCharArray(), salt, ITERATION_COUNT);
        Base64.Encoder base64Encoder = Base64.getEncoder();
        return HASH_FORMAT_PREFIX + "$" + ITERATION_COUNT + "$" + base64Encoder.encodeToString(salt)
               + "$" + base64Encoder.encodeToString(hash);
    }

    public static boolean verify(String password, String storedHash) {
        if (password == null) {
            return false;
        }

        String hashToCompare = storedHash;
        if (hashToCompare == null) {
            hashToCompare = HASH_FOR_UNKNOWN_USERS;
        }

        String[] hashParts = hashToCompare.split("\\$");
        if (hashParts.length != 4 || !HASH_FORMAT_PREFIX.equals(hashParts[0])) {
            return false;
        }

        try {
            int iterationCount = Integer.parseInt(hashParts[1]);
            byte[] salt = Base64.getDecoder().decode(hashParts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(hashParts[3]);
            byte[] actualHash = deriveKey(password.toCharArray(), salt, iterationCount);
            boolean hashesMatch = MessageDigest.isEqual(expectedHash, actualHash);
            return hashesMatch && storedHash != null;
        } catch (IllegalArgumentException invalidHashFormat) {
            return false;
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterationCount) {
        PBEKeySpec keySpecification = new PBEKeySpec(password, salt, iterationCount, HASH_LENGTH_IN_BITS);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
            return keyFactory.generateSecret(keySpecification).getEncoded();
        } catch (GeneralSecurityException securityException) {
            throw new IllegalStateException("PBKDF2 nicht verfügbar", securityException);
        } finally {
            keySpecification.clearPassword();
        }
    }
}
