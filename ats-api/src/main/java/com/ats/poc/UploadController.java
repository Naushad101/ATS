package com.ats.poc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/document")
public class UploadController {

    @Autowired private PdfTextExtractor pdfTextExtractor;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private DocumentService documentService;

    // @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    // public ResponseEntity<Map<String, Object>> uploadFile(
    //         @RequestParam(value = "type", defaultValue = "resume") String type,
    //         @RequestParam(value = "title", required = false) String title,
    //         @RequestParam(value = "mustHaveKeywords", required = false) List<String> mustHaveKeywords,
    //         @RequestParam(value = "name", required = false) String name,
    //         @RequestParam(value = "email", required = false) String email,
    //         @RequestParam(value = "skills", required = false) List<String> skills,
    //         @RequestPart("file") MultipartFile file) {

    //     // 1. Parse and Extract text from PDF first, because saveFile calls transferTo() which moves the temp file
    //     String extractedText = pdfTextExtractor.extractText(file);

    //     // 2. Save the file locally into a subfolder based on the 'type' (e.g., jd or resume)
    //     String savedPath = fileStorageService.saveFile(file, type);
        
    //     // 3. Save to DB based on type
    //     UUID dbId = null;
    //     if ("jd".equalsIgnoreCase(type)) {
    //         dbId = documentService.saveJd(title, extractedText, mustHaveKeywords);
    //     } else {
    //         dbId = documentService.saveResume(name, email, extractedText, skills);
    //     }

    //     return ResponseEntity.ok().body(Map.of(
    //         "id", dbId,
    //         "localPath", savedPath,
    //         "extractedTextPreview", extractedText.substring(0, Math.min(extractedText.length(), 100)) + "...",
    //         "type", type,
    //         "message", "Document saved locally, inserted into DB, and embedding generated."
    //     ));
    // }


    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
public ResponseEntity<Map<String, Object>> uploadFile(
        @RequestParam(value = "type", defaultValue = "resume") String type,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(value = "mustHaveKeywords", required = false) List<String> mustHaveKeywords,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "email", required = false) String email,
        @RequestParam(value = "skills", required = false) List<String> skills,
        @RequestParam(value = "jdId", required = false) String jdId, 
        @RequestPart("file") MultipartFile file) {

    String extractedText = pdfTextExtractor.extractText(file);
    String savedPath = fileStorageService.saveFile(file, type);

    UUID dbId = null;
    if ("jd".equalsIgnoreCase(type)) {
        dbId = documentService.saveJd(title, extractedText, mustHaveKeywords);
    } else {
        // Validate jdId is provided for resume
        if (jdId == null || jdId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "jdId is required when uploading a resume"
            ));
        }
        dbId = documentService.saveResume(name, email, extractedText, 
                                          skills, UUID.fromString(jdId)); // <-- pass it
    }

    return ResponseEntity.ok().body(Map.of(
        "id", dbId,
        "localPath", savedPath,
        "extractedTextPreview", extractedText.substring(0, Math.min(extractedText.length(), 100)) + "...",
        "type", type,
        "message", "Document saved locally, inserted into DB, and embedding generated."
    ));
}
}
