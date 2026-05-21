CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE job_descriptions (
    id                 UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    title              VARCHAR(255) NOT NULL,
    description        TEXT,                        
    must_have_keywords TEXT[]       DEFAULT '{}',   
    skills             TEXT[]       DEFAULT '{}',   
    embedding_status   VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jd_skills    ON job_descriptions USING GIN(skills);
CREATE INDEX idx_jd_keywords  ON job_descriptions USING GIN(must_have_keywords);

CREATE TABLE candidates (
    id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name             VARCHAR(255) NOT NULL,
    email            VARCHAR(255),
    skills           TEXT[]       DEFAULT '{}',   
    experience_years FLOAT,
    resume_text      TEXT,                         
    embedding_status VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    jd_id            UUID         REFERENCES job_descriptions(id) ON DELETE SET NULL,  -- added
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_candidate_skills ON candidates USING GIN(skills);
CREATE INDEX idx_candidate_jd_id  ON candidates(jd_id);                               -- added

CREATE TABLE jd_embeddings (
    id                 UUID      PRIMARY KEY DEFAULT uuid_generate_v4(),
    jd_id              UUID      NOT NULL UNIQUE REFERENCES job_descriptions(id) ON DELETE CASCADE,
    embedding          vector(1024),       
    keywords_embedding vector(1024),
    model_name         VARCHAR(100) NOT NULL DEFAULT 'all-MiniLM-L6-v2',
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_jd_emb_hnsw
    ON jd_embeddings USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE TABLE resume_embeddings (
    id           UUID      PRIMARY KEY DEFAULT uuid_generate_v4(),
    candidate_id UUID      NOT NULL UNIQUE REFERENCES candidates(id) ON DELETE CASCADE,
    embedding    vector(1024),
    skills_embedding vector(1024),
    model_name   VARCHAR(100) NOT NULL DEFAULT 'all-MiniLM-L6-v2',
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resume_emb_hnsw
    ON resume_embeddings USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);




-- CREATE EXTENSION IF NOT EXISTS vector;
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- CREATE TABLE job_descriptions (
--     id                 UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
--     title              VARCHAR(255) NOT NULL,
--     description        TEXT,                        
--     must_have_keywords TEXT[]       DEFAULT '{}',   
--     skills             TEXT[]       DEFAULT '{}',   
--     embedding_status   VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  
--     created_at         TIMESTAMP    NOT NULL DEFAULT NOW()
-- );

-- CREATE INDEX idx_jd_skills    ON job_descriptions USING GIN(skills);
-- CREATE INDEX idx_jd_keywords  ON job_descriptions USING GIN(must_have_keywords);

-- CREATE TABLE candidates (
--     id               UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
--     name             VARCHAR(255) NOT NULL,
--     email            VARCHAR(255),
--     skills           TEXT[]       DEFAULT '{}',   
--     experience_years FLOAT,
--     resume_text      TEXT,                         
--     embedding_status VARCHAR(50)  NOT NULL DEFAULT 'PENDING',  
--     created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
-- );

-- CREATE INDEX idx_candidate_skills ON candidates USING GIN(skills);

-- CREATE TABLE jd_embeddings (
--     id         UUID      PRIMARY KEY DEFAULT uuid_generate_v4(),
--     jd_id      UUID      NOT NULL UNIQUE REFERENCES job_descriptions(id) ON DELETE CASCADE,
--     embedding  vector(1024),       
--     keywords_embedding vector(1024),
--     model_name VARCHAR(100) NOT NULL DEFAULT 'all-MiniLM-L6-v2',
--     created_at TIMESTAMP NOT NULL DEFAULT NOW()
-- );

-- CREATE INDEX idx_jd_emb_hnsw
--     ON jd_embeddings USING hnsw (embedding vector_cosine_ops)
--     WITH (m = 16, ef_construction = 64);

-- CREATE TABLE resume_embeddings (
--     id           UUID      PRIMARY KEY DEFAULT uuid_generate_v4(),
--     candidate_id UUID      NOT NULL UNIQUE REFERENCES candidates(id) ON DELETE CASCADE,
--     embedding    vector(1024),
--     skills_embedding vector(1024),
--     model_name   VARCHAR(100) NOT NULL DEFAULT 'all-MiniLM-L6-v2',
--     created_at   TIMESTAMP NOT NULL DEFAULT NOW()
-- );

-- CREATE INDEX idx_resume_emb_hnsw
--     ON resume_embeddings USING hnsw (embedding vector_cosine_ops)
--     WITH (m = 16, ef_construction = 64);
