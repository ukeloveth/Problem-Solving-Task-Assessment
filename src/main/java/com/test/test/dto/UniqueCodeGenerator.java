package com.test.test.dto;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for generating unique codes in format: AAA-NNN-SSSS
 * Total length: 12 characters (including hyphens)
 */
@Service
public class UniqueCodeGenerator {

    private static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String ALPHANUMERIC = DIGITS + LOWERCASE_LETTERS;

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z]{2}-\\d{2}-[0-9a-z]{4}$");
    private static final int MAX_GENERATION_ATTEMPTS = 1000;

    private final Set<String> generatedCodes = new HashSet<>();
    private final Random random = new Random();


    public String generateCode() {
        int attempts = 0;
        String code;

        do {
            if (attempts >= MAX_GENERATION_ATTEMPTS) {
                throw new IllegalStateException(
                        "Unable to generate unique code after " + MAX_GENERATION_ATTEMPTS + " attempts"
                );
            }

            // Generate AAA (2 uppercase letters)
            String letters = generateRandomString(UPPERCASE_LETTERS, 2);

            // Generate NNN (2 digits)
            String digits = generateRandomString(DIGITS, 2);

            // Generate SSSS (4 alphanumeric characters - digits or lowercase)
            String suffix = generateRandomString(ALPHANUMERIC, 4);

            code = letters + "-" + digits + "-" + suffix;
            attempts++;

        } while (generatedCodes.contains(code));

        generatedCodes.add(code);
        return code;
    }

    public boolean validateCodeFormat(String code) {
        if (code == null || code.length() != 12) {
            return false;
        }
        return CODE_PATTERN.matcher(code).matches();
    }

    public boolean codeExists(String code) {
        return generatedCodes.contains(code);
    }

    public Set<String> getAllCodes() {
        return new HashSet<>(generatedCodes);
    }

    public void registerCode(String code) {
        if (validateCodeFormat(code)) {
            generatedCodes.add(code);
        }
    }

    private String generateRandomString(String charset, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(charset.length());
            sb.append(charset.charAt(index));
        }
        return sb.toString();
    }

    public void clearAllCodes() {
        generatedCodes.clear();
    }
}