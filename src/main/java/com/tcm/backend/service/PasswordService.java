package com.tcm.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    @Value("${app.password.min-length:12}")
    private int minLength;

    @Value("${app.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${app.password.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${app.password.require-digits:true}")
    private boolean requireDigits;

    @Value("${app.password.require-special-chars:true}")
    private boolean requireSpecialChars;

    @Value("${app.password.expiration-days:90}")
    private int expirationDays;

    @Value("${app.password.min-entropy:50}")
    private double minEntropy;

    // Common weak passwords to reject
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "123456", "123456789", "12345678", "12345", "1234567",
        "qwerty", "abc123", "password123", "admin", "administrator",
        "root", "toor", "pass", "test", "guest", "user", "demo"
    );

    // Special characters pattern
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGITS = Pattern.compile("[0-9]");

    public record PasswordValidationResult(
        boolean isValid,
        String errorMessage,
        int score,
        Set<String> suggestions
    ) {}

    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }

    public PasswordValidationResult validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return new PasswordValidationResult(false, "Password is required", 0, Set.of());
        }

        password = password.trim();
        Set<String> suggestions = new java.util.HashSet<>();
        int score = 0;

        // Check minimum length
        if (password.length() < minLength) {
            return new PasswordValidationResult(false,
                String.format("Password must be at least %d characters long", minLength),
                score, suggestions);
        }
        score += 20;

        // Check for common weak passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            return new PasswordValidationResult(false,
                "Password is too common and easily guessable",
                0, Set.of("Use a unique password", "Combine multiple words", "Add numbers and symbols"));
        }

        // Check uppercase requirement
        if (requireUppercase && !UPPERCASE.matcher(password).find()) {
            return new PasswordValidationResult(false,
                "Password must contain at least one uppercase letter",
                score, Set.of("Add uppercase letters (A-Z)"));
        }
        if (UPPERCASE.matcher(password).find()) score += 15;

        // Check lowercase requirement
        if (requireLowercase && !LOWERCASE.matcher(password).find()) {
            return new PasswordValidationResult(false,
                "Password must contain at least one lowercase letter",
                score, Set.of("Add lowercase letters (a-z)"));
        }
        if (LOWERCASE.matcher(password).find()) score += 15;

        // Check digits requirement
        if (requireDigits && !DIGITS.matcher(password).find()) {
            return new PasswordValidationResult(false,
                "Password must contain at least one digit",
                score, Set.of("Add numbers (0-9)"));
        }
        if (DIGITS.matcher(password).find()) score += 15;

        // Check special characters requirement
        if (requireSpecialChars && !SPECIAL_CHARS.matcher(password).find()) {
            return new PasswordValidationResult(false,
                "Password must contain at least one special character",
                score, Set.of("Add special characters (!@#$%^&*)"));
        }
        if (SPECIAL_CHARS.matcher(password).find()) score += 15;

        // Check for password entropy (complexity)
        double entropy = calculateEntropy(password);
        if (entropy < minEntropy) {
            suggestions.add("Use a longer password");
            suggestions.add("Mix different character types");
            suggestions.add("Avoid predictable patterns");
        } else {
            score += 20;
        }

        // Additional security checks
        if (containsRepeatingPatterns(password)) {
            score -= 10;
            suggestions.add("Avoid repeating patterns (aaa, 123, abc)");
        }

        if (containsSequentialChars(password)) {
            score -= 10;
            suggestions.add("Avoid sequential characters (abc, 123, qwe)");
        }

        if (containsPersonalInfo(password)) {
            score -= 15;
            suggestions.add("Avoid personal information in passwords");
        }

        // Bonus points for good practices
        if (password.length() > minLength + 4) score += 10; // Extra length bonus
        if (hasMultipleCharacterTypes(password) >= 4) score += 10; // All character types

        // Final validation
        boolean isValid = score >= 70 && entropy >= minEntropy;

        if (!isValid && suggestions.isEmpty()) {
            suggestions.add("Create a longer, more complex password");
            suggestions.add("Use a passphrase with multiple words");
            suggestions.add("Consider using a password manager");
        }

        return new PasswordValidationResult(isValid,
            isValid ? "Password meets security requirements" : "Password does not meet security requirements",
            Math.max(0, Math.min(100, score)), suggestions);
    }

    public boolean isPasswordExpired(Instant passwordCreatedAt) {
        if (passwordCreatedAt == null) {
            return true; // Force password change if creation date is unknown
        }
        Instant expirationDate = passwordCreatedAt.plus(expirationDays, ChronoUnit.DAYS);
        return Instant.now().isAfter(expirationDate);
    }

    public Instant calculatePasswordExpirationDate() {
        return Instant.now().plus(expirationDays, ChronoUnit.DAYS);
    }

    public int getDaysUntilExpiration(Instant passwordExpiresAt) {
        if (passwordExpiresAt == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(Instant.now(), passwordExpiresAt);
        return Math.max(0, (int) days);
    }

    public boolean isPasswordExpiringSoon(Instant passwordExpiresAt, int warningDays) {
        int daysUntilExpiration = getDaysUntilExpiration(passwordExpiresAt);
        return daysUntilExpiration <= warningDays && daysUntilExpiration > 0;
    }

    private double calculateEntropy(String password) {
        int charsetSize = 0;

        if (LOWERCASE.matcher(password).find()) charsetSize += 26;
        if (UPPERCASE.matcher(password).find()) charsetSize += 26;
        if (DIGITS.matcher(password).find()) charsetSize += 10;
        if (SPECIAL_CHARS.matcher(password).find()) charsetSize += 32;

        if (charsetSize == 0) return 0;

        // Shannon entropy calculation: length * log2(charset size)
        return password.length() * (Math.log(charsetSize) / Math.log(2));
    }

    private int hasMultipleCharacterTypes(String password) {
        int types = 0;
        if (LOWERCASE.matcher(password).find()) types++;
        if (UPPERCASE.matcher(password).find()) types++;
        if (DIGITS.matcher(password).find()) types++;
        if (SPECIAL_CHARS.matcher(password).find()) types++;
        return types;
    }

    private boolean containsRepeatingPatterns(String password) {
        // Check for 3+ repeating characters (aaa, 111, etc.)
        return password.matches(".*(..)\\1+.*") ||
               password.matches(".*([a-zA-Z0-9])\\1{2,}.*");
    }

    private boolean containsSequentialChars(String password) {
        String lower = password.toLowerCase();

        // Check for sequential alphabets (abc, def, etc.)
        for (int i = 0; i < lower.length() - 2; i++) {
            char c1 = lower.charAt(i);
            char c2 = lower.charAt(i + 1);
            char c3 = lower.charAt(i + 2);

            if ((c1 + 1 == c2 && c2 + 1 == c3) ||
                (c1 - 1 == c2 && c2 - 1 == c3)) {
                return true;
            }
        }

        // Check for sequential numbers (123, 987, etc.)
        return lower.contains("123") || lower.contains("234") || lower.contains("345") ||
               lower.contains("456") || lower.contains("567") || lower.contains("678") ||
               lower.contains("789") || lower.contains("890") ||
               lower.contains("987") || lower.contains("876") || lower.contains("765") ||
               lower.contains("654") || lower.contains("543") || lower.contains("432") ||
               lower.contains("321") || lower.contains("210");
    }

    private boolean containsPersonalInfo(String password) {
        String lower = password.toLowerCase();

        // Check for common personal info patterns
        return lower.contains("admin") || lower.contains("user") ||
               lower.contains("test") || lower.contains("demo") ||
               lower.contains("tcm") || lower.contains("app") ||
               lower.matches(".*\\d{4}.*"); // Years like 2023, 1990, etc.
    }

    public String generateSecurePassword(int length) {
        if (length < minLength) {
            length = minLength;
        }

        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        String allChars = uppercase + lowercase + digits + special;
        java.util.Random random = new java.security.SecureRandom();

        StringBuilder password = new StringBuilder();

        // Ensure at least one character from each required type
        if (requireUppercase) password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        if (requireLowercase) password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        if (requireDigits) password.append(digits.charAt(random.nextInt(digits.length())));
        if (requireSpecialChars) password.append(special.charAt(random.nextInt(special.length())));

        // Fill the rest randomly
        for (int i = password.length(); i < length; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }
}