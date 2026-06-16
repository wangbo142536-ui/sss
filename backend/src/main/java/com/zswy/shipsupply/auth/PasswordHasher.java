package com.zswy.shipsupply.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final String PREFIX = "sha256";
    private final SecureRandom secureRandom = new SecureRandom();

    public String hash(String rawPassword) {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        byte[] digest = digest(salt, rawPassword);
        return PREFIX + "$" + encode(salt) + "$" + encode(digest);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\$");
        if (parts.length != 3 || !PREFIX.equals(parts[0])) {
            return false;
        }
        byte[] salt = decode(parts[1]);
        byte[] expected = decode(parts[2]);
        byte[] actual = digest(salt, rawPassword);
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] digest(byte[] salt, String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            return digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
