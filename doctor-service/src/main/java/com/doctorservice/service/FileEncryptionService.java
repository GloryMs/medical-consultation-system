package com.doctorservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Service for encrypting and decrypting PDF files
 * Uses AES encryption and GZIP compression
 */
@Service
@Slf4j
public class FileEncryptionService {

    @Value("${app.file.encryption.key:3F4A7B2C8E6D9F1A5B8C2E7F9D4A6B3C}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * Compress and encrypt file data
     * Used when saving files to storage
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
     * Used when retrieving files from storage
     */
    public byte[] decryptAndDecompress(byte[] encryptedData) throws Exception {
        log.debug("Starting decryption and decompression. Encrypted size: {} bytes", encryptedData.length);

        // Decrypt first
        byte[] decrypted = decrypt(encryptedData);
        log.debug("Decrypted (compressed) size: {} bytes", decrypted.length);

        // Then decompress
        byte[] decompressed = decompress(decrypted);
        log.debug("Decompressed (original) size: {} bytes", decompressed.length);

        return decompressed;
    }

    /**
     * Compress data using GZIP
     */
    private byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {

            gzipOut.write(data);
            gzipOut.finish();
            return baos.toByteArray();

        } catch (IOException ex) {
            log.error("Error compressing data", ex);
            throw ex;
        }
    }

    /**
     * Decompress GZIP data
     */
    private byte[] decompress(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();

        } catch (IOException ex) {
            log.error("Error decompressing data", ex);
            throw ex;
        }
    }

    /**
     * Encrypt data using AES
     */
    private byte[] encrypt(byte[] data) throws Exception {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return cipher.doFinal(data);

        } catch (Exception ex) {
            log.error("Error encrypting data", ex);
            throw ex;
        }
    }

    /**
     * Decrypt data using AES
     */
    private byte[] decrypt(byte[] encryptedData) throws Exception {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return cipher.doFinal(encryptedData);

        } catch (Exception ex) {
            log.error("Error decrypting data", ex);
            throw ex;
        }
    }
}