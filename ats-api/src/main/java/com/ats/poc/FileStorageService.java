package com.ats.poc;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {
    
    // Save to a local directory inside the project workspace
    private final String uploadDir = "d:/ATSPoc/uploads/";

    public String saveFile(MultipartFile file, String subDir) {
        try {
            Path dirPath = Paths.get(uploadDir, subDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            
            // Generate a unique filename to avoid overwrites
            String originalName = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + (originalName != null ? originalName : "uploaded.pdf");
            Path filePath = dirPath.resolve(fileName);
            
            // Transfer the multipart file to the local disk
            file.transferTo(filePath.toFile());
            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Could not store file locally", e);
        }
    }
}
