package com.ats.poc;

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class EmbeddingService {

    @Autowired
    private JdbcTemplate jdbc;
    private ZooModel<String, float[]> model;

    // bge-large-en-v1.5 max is 512 tokens ≈ ~300 words safely
    private static final int CHUNK_WORDS = 300;
    private static final int OVERLAP_WORDS = 50;

    // bge retrieval instruction — only applied to the query side (JD)
    private static final String BGE_QUERY_PREFIX = "Represent this sentence for searching relevant passages: ";

    @PostConstruct
    public void init() {
        try {
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/BAAI/bge-large-en-v1.5")
                    .optEngine("PyTorch")
                    .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                    .build();
            this.model = criteria.loadModel();
            System.out.println(">>> DJL bge-large-en-v1.5 loaded successfully.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DJL model", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (model != null)
            model.close();
    }

    public void embedJd(UUID jdId, String fullText, List<String> mustHaveKeywords) {
        try {
            // JD is the query side — apply bge prefix
            float[] vector = generateChunkedEmbedding(fullText, true);

            float[] keywordsVector = null;
            if (mustHaveKeywords != null && !mustHaveKeywords.isEmpty()) {
                // Keywords are also query side
                keywordsVector = generateSingleEmbedding(
                        BGE_QUERY_PREFIX + String.join(", ", mustHaveKeywords));
            }

            saveVector("jd_embeddings", "jd_id", jdId, vector, "keywords_embedding", keywordsVector);
            jdbc.update("UPDATE job_descriptions SET embedding_status='DONE' WHERE id=?", jdId);
        } catch (Exception e) {
            jdbc.update("UPDATE job_descriptions SET embedding_status='FAILED' WHERE id=?", jdId);
            throw new RuntimeException("JD embedding failed", e);
        }
    }

    public void embedResume(UUID candidateId, String fullText, List<String> skills) {
        try {
            // Resume is the document side — NO prefix
            float[] vector = generateChunkedEmbedding(fullText, false);

            float[] skillsVector = null;
            if (skills != null && !skills.isEmpty()) {
                // Skills are document side — no prefix
                skillsVector = generateSingleEmbedding(String.join(", ", skills));
            }

            saveVector("resume_embeddings", "candidate_id", candidateId, vector, "skills_embedding", skillsVector);
            jdbc.update("UPDATE candidates SET embedding_status='DONE' WHERE id=?", candidateId);
        } catch (Exception e) {
            jdbc.update("UPDATE candidates SET embedding_status='FAILED' WHERE id=?", candidateId);
            throw new RuntimeException("Resume embedding failed", e);
        }
    }

    // isQuery=true adds bge prefix (JD side), isQuery=false skips it (resume side)
    private float[] generateChunkedEmbedding(String text, boolean isQuery) {
        List<String> chunks = chunkText(text, CHUNK_WORDS, OVERLAP_WORDS);

        System.out.println(">>> CHUNKING: word_count=" + text.split("\\s+").length
                + " chunks=" + chunks.size() + " isQuery=" + isQuery);

        List<float[]> vectors = new ArrayList<>();
        try (Predictor<String, float[]> predictor = model.newPredictor()) {
            for (String chunk : chunks) {
                if (!chunk.isBlank()) {
                    // Only prepend prefix on first chunk — prefix on every chunk
                    // distorts mean-pooling for long documents
                    String input = isQuery ? BGE_QUERY_PREFIX + chunk : chunk;
                    vectors.add(predictor.predict(input));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Chunked embedding failed", e);
        }

        if (vectors.isEmpty())
            throw new RuntimeException("No chunks generated");
        return meanPool(vectors);
    }

    private float[] generateSingleEmbedding(String text) {
        try (Predictor<String, float[]> predictor = model.newPredictor()) {
            return predictor.predict(text);
        } catch (Exception e) {
            throw new RuntimeException("Single embedding failed", e);
        }
    }

    private List<String> chunkText(String text, int chunkSize, int overlap) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        for (int i = 0; i < words.length; i += step) {
            int end = Math.min(i + chunkSize, words.length);
            chunks.add(String.join(" ", Arrays.copyOfRange(words, i, end)));
            if (end == words.length)
                break;
        }
        return chunks;
    }

    private float[] meanPool(List<float[]> vectors) {
        int dim = vectors.get(0).length;
        float[] result = new float[dim];
        for (float[] v : vectors) {
            for (int i = 0; i < dim; i++)
                result[i] += v[i];
        }
        for (int i = 0; i < dim; i++)
            result[i] /= vectors.size();
        return result;
    }

    private void saveVector(String table, String col, UUID id, float[] v,
            String secondaryCol, float[] secondaryVector) {
        String pgVec = toPgVector(v);
        String pgSecondaryVec = toPgVector(secondaryVector);
        jdbc.update(
                "INSERT INTO " + table + " (" + col + ", embedding, " + secondaryCol + ", model_name) " +
                        "VALUES (?, ?::vector, ?::vector, 'bge-large-en-v1.5') " +
                        "ON CONFLICT (" + col + ") DO UPDATE SET embedding = EXCLUDED.embedding, "
                        + secondaryCol + " = EXCLUDED." + secondaryCol,
                id, pgVec, pgSecondaryVec);
    }

    private String toPgVector(float[] v) {
        if (v == null)
            return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            sb.append(v[i]);
            if (i < v.length - 1)
                sb.append(",");
        }
        return sb.append("]").toString();
    }

    public float[] embedText(String text, boolean isQuery) {
        String[] words = text.split("\\s+");

        if (words.length <= CHUNK_WORDS) {
            // Short text — no chunking needed, single embedding
            String input = isQuery ? BGE_QUERY_PREFIX + text : text;
            return generateSingleEmbedding(input);
        }

        // Long text — chunk and mean-pool
        return generateChunkedEmbedding(text, isQuery);
    }
}