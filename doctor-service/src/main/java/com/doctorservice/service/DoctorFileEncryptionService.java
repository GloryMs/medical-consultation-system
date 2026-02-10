package com.doctorservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Service for encrypting and decrypting files
 * Uses AES-256 encryption and GZIP compression
 */
@Service
@Slf4j
public class DoctorFileEncryptionService {
    @Value("${app.file.encryption.key:3F4A7B2C8E6D9F1A5B8C2E7F9D4A6B3C}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * Compress and encrypt file data
     */
    public byte[] compressAndEncrypt(byte[] data) throws Exception {
        log.debug("Starting compression and encryption. Original size: {} bytes", data.length);

        // Compress first
        byte[] compressed = compress(data);
        log.debug("Compressed size: {} bytes (Ratio: {:.2f}%)",
                compressed.length, (compressed.length * 100.0 / data.length));

        // Then encrypt
        byte[] encrypted = encrypt(compressed);
        log.debug("Encrypted size: {} bytes", encrypted.length);

        return encrypted;
    }

    /**
     * Decrypt and decompress file data
     */
    public byte[] decryptAndDecompress(byte[] encryptedData) throws Exception {
        log.debug("Starting decryption and decompression. Encrypted size: {} bytes", encryptedData.length);

        // Decrypt first
        byte[] decrypted = decrypt(encryptedData);
        log.debug("Decrypted size: {} bytes", decrypted.length);

        // Then decompress
        byte[] decompressed = decompress(decrypted);
        log.debug("Decompressed size: {} bytes", decompressed.length);

        return decompressed;
    }

    /**
     * Compress data using GZIP
     */
    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(data.length);
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(data);
        }
        return byteStream.toByteArray();
    }

    /**
     * Decompress GZIP data
     */
    private byte[] decompress(byte[] compressedData) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPInputStream gzipStream = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipStream.read(buffer)) > 0) {
                byteStream.write(buffer, 0, len);
            }
        }
        return byteStream.toByteArray();
    }

    /**
     * Encrypt data using AES
     */
    private byte[] encrypt(byte[] data) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return cipher.doFinal(data);
    }

    /**
     * Decrypt data using AES
     */
    private byte[] decrypt(byte[] encryptedData) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Save encrypted and compressed data to file
     */
    public void saveEncryptedFile(byte[] data, String filePath) throws Exception {
        byte[] processedData = compressAndEncrypt(data);
        Path path = Paths.get(filePath);
        Files.createDirectories(path.getParent());
        Files.write(path, processedData);
        log.info("File saved and encrypted at: {}", filePath);
    }

    /**
     * Read and decrypt file data
     */
    public byte[] readDecryptedFile(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }

        byte[] encryptedData = Files.readAllBytes(path);
        byte[] decryptedData = decryptAndDecompress(encryptedData);
        log.info("File read and decrypted from: {}", filePath);
        return decryptedData;
    }
}
