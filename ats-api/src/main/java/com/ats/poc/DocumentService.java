package com.ats.poc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EmbeddingService embeddingService;

    public UUID saveJd(String title, String description, List<String> mustHaveKeywords) {
        UUID jdId = UUID.randomUUID();
        String[] keywordsArr = mustHaveKeywords == null ? new String[0] : mustHaveKeywords.toArray(new String[0]);
        String safeTitle = title != null ? title : "Untitled JD";

        jdbc.update(
            "INSERT INTO job_descriptions (id, title, description, must_have_keywords, embedding_status) " +
            "VALUES (?, ?, ?, ?, 'PENDING')",
            jdId, safeTitle, description,
            new SqlParameterValue(Types.ARRAY, keywordsArr)
        );

        // Generate embedding
        embeddingService.embedJd(jdId, description, mustHaveKeywords);
        return jdId;
    }

    public UUID saveResume(String name, String email, String resumeText, 
                       List<String> skills, UUID jdId) {          // <-- add jdId
    UUID candidateId = UUID.randomUUID();
    String[] skillsArr = skills == null ? new String[0] : skills.toArray(new String[0]);
    String safeName = name != null ? name : "Unknown Candidate";

    jdbc.update(
        "INSERT INTO candidates (id, name, email, skills, resume_text, embedding_status, jd_id) " +
        "VALUES (?, ?, ?, ?, ?, 'PENDING', ?)",                   // <-- add jd_id
        candidateId, safeName, email,
        new SqlParameterValue(Types.ARRAY, skillsArr),
        resumeText,
        jdId                                                       // <-- pass it
    );

    embeddingService.embedResume(candidateId, resumeText, skills);
    return candidateId;
}

    // public UUID saveResume(String name, String email, String resumeText, List<String> skills) {
    //     UUID candidateId = UUID.randomUUID();
    //     String[] skillsArr = skills == null ? new String[0] : skills.toArray(new String[0]);
    //     String safeName = name != null ? name : "Unknown Candidate";

    //     jdbc.update(
    //         "INSERT INTO candidates (id, name, email, skills, resume_text, embedding_status) " +
    //         "VALUES (?, ?, ?, ?, ?, 'PENDING')",
    //         candidateId, safeName, email,
    //         new SqlParameterValue(Types.ARRAY, skillsArr),
    //         resumeText
    //     );

    //     // Generate embedding
    //     embeddingService.embedResume(candidateId, resumeText, skills);
    //     return candidateId;
    // }
}
