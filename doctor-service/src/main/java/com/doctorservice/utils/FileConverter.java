package com.doctorservice.utils;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

public class FileConverter {

    public MultipartFile convert(byte[] pdfContent, String fileName) {
        // You can change the content type and original file name as needed
        return new MockMultipartFile(
                "file",                 // name of the parameter in the multipart form
                fileName,               // original file name
                "application/pdf",      // content type
                pdfContent              // byte array
        );
    }
}
