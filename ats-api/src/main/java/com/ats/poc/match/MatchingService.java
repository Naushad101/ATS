package com.ats.poc.match;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MatchingService {

    @Autowired private JdbcTemplate jdbc;


    private static final String MATCH_SQL = """
    SELECT
        c.id,
        c.name,
        c.email,
        c.skills                                           AS all_skills,

        ROUND(CAST(1 - (re.embedding <=> jde.embedding) AS numeric), 4)
            AS semantic_score,

        ROUND(CAST(
            COALESCE(1 - (re.skills_embedding <=> jde.keywords_embedding), 0)
        AS numeric), 4)
            AS keyword_score,

        ARRAY(SELECT s FROM unnest(c.skills) s
              WHERE s = ANY(jd.must_have_keywords))        AS matched_keywords,

        ARRAY(SELECT k FROM unnest(jd.must_have_keywords) k
              WHERE k != ALL(c.skills))                    AS missing_keywords,

        ROUND(CAST(
            0.4 * COALESCE(1 - (re.skills_embedding <=> jde.keywords_embedding), 0)
            + 0.6 * (1 - (re.embedding <=> jde.embedding))
        AS numeric), 4)
            AS final_score

    FROM  resume_embeddings  re
    JOIN  candidates         c   ON c.id      = re.candidate_id
    JOIN  jd_embeddings      jde ON jde.jd_id  = ?
    JOIN  job_descriptions   jd  ON jd.id      = ?

    WHERE c.embedding_status = 'DONE'
      AND c.jd_id = ?                                      -- <-- only this line added

    ORDER BY final_score DESC
    LIMIT 10
""";

public List<Map<String, Object>> rankCandidates(UUID jdId) {
    return jdbc.query(MATCH_SQL,
        (rs, i) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("candidateId",   rs.getObject("id").toString());
            row.put("name",          rs.getString("name"));
            row.put("email",         rs.getString("email"));
            row.put("semanticScore", Math.round(rs.getDouble("semantic_score") * 100) + "%");
            row.put("keywordScore",  Math.round(rs.getDouble("keyword_score") * 100) + "%");
            return row;
        },
        jdId, jdId, jdId   // <-- pass jdId three times now
    );
}

    // private static final String MATCH_SQL = """
    //     SELECT
    //         c.id,
    //         c.name,
    //         c.email,
    //         c.skills                                           AS all_skills,

    //         ROUND(CAST(1 - (re.embedding <=> jde.embedding) AS numeric), 4)
    //             AS semantic_score,

    //         ROUND(CAST(
    //             COALESCE(1 - (re.skills_embedding <=> jde.keywords_embedding), 0)
    //         AS numeric), 4)
    //             AS keyword_score,

    //         ARRAY(SELECT s FROM unnest(c.skills) s
    //               WHERE s = ANY(jd.must_have_keywords))        AS matched_keywords,

    //         ARRAY(SELECT k FROM unnest(jd.must_have_keywords) k
    //               WHERE k != ALL(c.skills))                    AS missing_keywords,

    //         ROUND(CAST(
    //             0.4 * COALESCE(1 - (re.skills_embedding <=> jde.keywords_embedding), 0)
    //             + 0.6 * (1 - (re.embedding <=> jde.embedding))
    //         AS numeric), 4)
    //             AS final_score

    //     FROM  resume_embeddings  re
    //     JOIN  candidates         c   ON c.id      = re.candidate_id
    //     JOIN  jd_embeddings      jde ON jde.jd_id  = ?
    //     JOIN  job_descriptions   jd  ON jd.id      = ?

    //     WHERE c.embedding_status = 'DONE'

    //     ORDER BY final_score DESC
    //     LIMIT 10
    // """;

    // public List<Map<String, Object>> rankCandidates(UUID jdId) {
    //     return jdbc.query(MATCH_SQL,
    //         (rs, i) -> {
    //             Map<String, Object> row = new LinkedHashMap<>();
    //             row.put("candidateId",      rs.getObject("id").toString());
    //             row.put("name",             rs.getString("name"));
    //             row.put("email",            rs.getString("email"));
    //             row.put("semanticScore",    Math.round(rs.getDouble("semantic_score") * 100) + "%");
    //             row.put("keywordScore",     Math.round(rs.getDouble("keyword_score") * 100) + "%");
    //             // row.put("finalScore",       Math.round(rs.getDouble("final_score") * 100) + "%");
    //             // row.put("matchedKeywords",  toList(rs.getArray("matched_keywords")));
    //             // row.put("missingKeywords",  toList(rs.getArray("missing_keywords")));
    //             // row.put("allSkills",        toList(rs.getArray("all_skills")));
    //             return row;
    //         },
    //         jdId, jdId
    //     );
    // }

    // private List<String> toList(java.sql.Array arr) throws java.sql.SQLException {
    //     if (arr == null) return List.of();
    //     return List.of((String[]) arr.getArray());
    // }
}
