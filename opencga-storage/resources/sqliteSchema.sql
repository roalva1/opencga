CREATE TABLE IF NOT EXISTS variant (
  id_variant INTEGER PRIMARY KEY AUTOINCREMENT,
  chromosome TEXT,
  position   INT64,
  snp_id     TEXT,
  ref        TEXT,
  alt        TEXT
--   qual DOUBLE,
--   filter TEXT,
);

CREATE TABLE IF NOT EXISTS sample (
  id_sample   INTEGER PRIMARY KEY AUTOINCREMENT,
  sample_name TEXT
);

CREATE TABLE IF NOT EXISTS sample_info (
  id_sample_info INTEGER PRIMARY KEY AUTOINCREMENT,
  id_variant     INTEGER,
  id_sample      INTEGER,
  allele_1       INTEGER,
  allele_2       INTEGER,
  data           TEXT,
  FOREIGN KEY (id_variant) REFERENCES variant (id_variant),
  FOREIGN KEY (id_sample) REFERENCES sample (id_sample)
);

CREATE TABLE IF NOT EXISTS variant_info (
  id_variant_info INTEGER PRIMARY KEY AUTOINCREMENT,
  id_variant      INTEGER,
  key             TEXT,
  value           TEXT,
  FOREIGN KEY (id_variant) REFERENCES variant (id_variant)
);

CREATE TABLE IF NOT EXISTS global_stats (
  name  TEXT,
  title TEXT,
  value TEXT,
  PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS variant_stats (
  id_variant_stats           INTEGER PRIMARY KEY AUTOINCREMENT,
  id_variant                 INTEGER,
  maf                        DOUBLE,
  mgf                        DOUBLE,
  allele_maf                 TEXT,
  genotype_maf               TEXT,
  miss_allele                INT,
  miss_gt                    INT,
  mendel_err                 INT,
  is_indel                   INT,
  cases_percent_dominant     DOUBLE,
  controls_percent_dominant  DOUBLE,
  cases_percent_recessive    DOUBLE,
  controls_percent_recessive DOUBLE,
  genotypes                  TEXT,
  FOREIGN KEY (id_variant) REFERENCES variant (id_variant)
);

CREATE TABLE IF NOT EXISTS variant_effect (
  id_variant_effect     INTEGER PRIMARY KEY AUTOINCREMENT,
  feature_id            TEXT,
  feature_name          TEXT,
  feature_type          TEXT,
  feature_biotype       TEXT,
  feature_chromosome    TEXT,
  feature_start         INT64,
  feature_end           INT64,
  feature_strand        TEXT,
  snp_id                TEXT,
  ancestral             TEXT,
  alternative           TEXT,
  gene_id               TEXT,
  transcript_id         TEXT,
  gene_name             TEXT,
  consequence_type      TEXT,
  consequence_type_obo  TEXT,
  consequence_type_desc TEXT,
  consequence_type_type TEXT,
  aa_position           INT64,
  aminoacid_change      TEXT,
  codon_change          TEXT,
--   polyphen_score        DOUBLE,
--   polyphen_effect       INT,
--   sift_score            DOUBLE,
--   sift_effect           INT,
  FOREIGN KEY (id_variant) REFERENCES variant (id_variant)

);



