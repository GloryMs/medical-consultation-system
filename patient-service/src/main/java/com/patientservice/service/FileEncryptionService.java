package com.patientservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
@Slf4j
public class FileEncryptionService {

    @Value("${app.file.encryption.key:YourSecretKeyHere1234567890123456}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * Compress and encrypt file data
     */
    public byte[] compressAndEncrypt(byte[] data) throws Exception {
        // Compress first
        byte[] compressed = compress(data);
        
        // Then encrypt
        return encrypt(compressed);
    }

    /**
     * Decrypt and decompress file data
     */
    public byte[] decryptAndDecompress(byte[] encryptedData) throws Exception {
        // Decrypt first
        byte[] decrypted = decrypt(encryptedData);
        
        // Then decompress
        return decompress(decrypted);
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
        }
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