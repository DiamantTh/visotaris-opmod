package systems.diath.visotaris_opmod.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Argon2id-Hashing für den ausschließlich lokalen Systemzugang. */
public final class SystemPasswordService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MEMORY_KIB = 131_072; // 128 MiB
    private static final int ITERATIONS = 4;
    private static final int PARALLELISM = 2;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;

    private SystemPasswordService() { }

    public static boolean isConfigured(String value) {
        return value != null && value.startsWith("$argon2id$");
    }

    public static String hash(char[] password) {
        validate(password);
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, MEMORY_KIB, ITERATIONS, PARALLELISM, HASH_BYTES);
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            return "$argon2id$v=19$m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM
                + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(derived);
        } finally {
            Arrays.fill(salt, (byte) 0);
            Arrays.fill(derived, (byte) 0);
        }
    }

    public static boolean verify(char[] password, String encoded) {
        if (!isConfigured(encoded) || password == null) return false;
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 6 || !"argon2id".equals(parts[1]) || !"v=19".equals(parts[2])) return false;
            String[] params = parts[3].split(",");
            if (params.length != 3) return false;
            int memory = Integer.parseInt(params[0].substring(2));
            int iterations = Integer.parseInt(params[1].substring(2));
            int parallelism = Integer.parseInt(params[2].substring(2));
            // Verifikation akzeptiert keine bewusst geschwächten gespeicherten Werte.
            if (memory < MEMORY_KIB || iterations < ITERATIONS || parallelism < 1 || memory > 524_288 || iterations > 12 || parallelism > 8) return false;
            Base64.Decoder decoder = Base64.getUrlDecoder();
            byte[] salt = decoder.decode(parts[4]);
            byte[] expected = decoder.decode(parts[5]);
            byte[] actual = derive(password, salt, memory, iterations, parallelism, expected.length);
            try { return expected.length == HASH_BYTES && MessageDigest.isEqual(actual, expected); }
            finally { Arrays.fill(salt, (byte) 0); Arrays.fill(expected, (byte) 0); Arrays.fill(actual, (byte) 0); }
        } catch (RuntimeException ex) { return false; }
    }

    private static byte[] derive(char[] password, byte[] salt, int memory, int iterations, int parallelism, int length) {
        byte[] passwordBytes = new String(password).getBytes(StandardCharsets.UTF_8);
        try {
            Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13).withMemoryAsKB(memory)
                .withIterations(iterations).withParallelism(parallelism).withSalt(salt).build();
            byte[] output = new byte[length];
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(parameters);
            generator.generateBytes(passwordBytes, output);
            return output;
        } finally { Arrays.fill(passwordBytes, (byte) 0); }
    }

    private static void validate(char[] password) {
        if (password == null || password.length < 12 || password.length > 256)
            throw new IllegalArgumentException("Das Systempasswort muss 12 bis 256 Zeichen haben.");
    }
}
