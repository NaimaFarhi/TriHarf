-- =====================================================
-- TRIHARF DATABASE - COMPLETE SCHEMA
-- Matches Java entities with proper enum mappings
-- =====================================================

DROP DATABASE IF EXISTS baccalaureat_db;
CREATE DATABASE baccalaureat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE baccalaureat_db;

-- =====================================================
-- TABLE: joueurs
-- =====================================================
CREATE TABLE joueurs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pseudo VARCHAR(50) NOT NULL,
  score_total INT DEFAULT 0,
  nb_parties INT DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_pseudo (pseudo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: categories
-- Langue: ORDINAL enum (0=FRANCAIS, 1=ANGLAIS, 2=ARABE)
-- =====================================================
CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nom VARCHAR(100) NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  actif TINYINT(1) NOT NULL DEFAULT 1,
  langue TINYINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_nom_langue (nom, langue)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: mots
-- langue_code: STRING enum ('FRANCAIS', 'ANGLAIS', 'ARABE')
-- =====================================================
CREATE TABLE mots (
  id BIGINT NOT NULL AUTO_INCREMENT,
  texte VARCHAR(100) NOT NULL,
  categorie_id BIGINT NOT NULL,
  lettre CHAR(1) NOT NULL,
  langue_code VARCHAR(20) NOT NULL,
  valide TINYINT(1) NOT NULL DEFAULT 1,
  nb_utilisations INT DEFAULT 1,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY UK_texte_categorie (texte, categorie_id),
  KEY idx_categorie (categorie_id),
  KEY idx_lettre (lettre),
  KEY idx_texte (texte),
  CONSTRAINT fk_mot_categorie FOREIGN KEY (categorie_id) REFERENCES categories (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: parties
-- langue_code: STRING enum ('FRANCAIS', 'ANGLAIS', 'ARABE')
-- =====================================================
CREATE TABLE parties (
  id BIGINT NOT NULL AUTO_INCREMENT,
  joueur_id BIGINT NOT NULL,
  lettre CHAR(1) NOT NULL,
  score INT NOT NULL DEFAULT 0,
  mode VARCHAR(50) NOT NULL,
  langue_code VARCHAR(20),
  duree_seconde INT DEFAULT NULL,
  date_partie DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_joueur (joueur_id),
  KEY idx_date (date_partie),
  CONSTRAINT fk_partie_joueur FOREIGN KEY (joueur_id) REFERENCES joueurs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABLE: resultats_partie
-- =====================================================
CREATE TABLE resultats_partie (
  id BIGINT NOT NULL AUTO_INCREMENT,
  partie_id BIGINT NOT NULL,
  categorie_id BIGINT DEFAULT NULL,
  nom_categorie VARCHAR(100),
  mot VARCHAR(50),
  valide TINYINT(1) NOT NULL DEFAULT 0,
  points INT DEFAULT 0,
  message VARCHAR(255),
  PRIMARY KEY (id),
  KEY idx_partie (partie_id),
  KEY idx_categorie (categorie_id),
  CONSTRAINT fk_resultat_partie FOREIGN KEY (partie_id) REFERENCES parties (id) ON DELETE CASCADE,
  CONSTRAINT fk_resultat_categorie FOREIGN KEY (categorie_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- INSERT DEFAULT CATEGORIES
-- langue: 0=FRANCAIS, 1=ANGLAIS, 2=ARABE
-- =====================================================

-- FRENCH CATEGORIES (langue = 0)
INSERT INTO categories (nom, description, actif, langue, created_at) VALUES
('Pays', 'Un pays du monde', 1, 0, NOW()),
('Ville', 'Une ville', 1, 0, NOW()),
('Animal', 'Un animal', 1, 0, NOW()),
('Fruit/Légume', 'Un fruit ou légume', 1, 0, NOW()),
('Métier', 'Une profession', 1, 0, NOW()),
('Prénom', 'Un prénom', 1, 0, NOW()),
('Marque', 'Une marque connue', 1, 0, NOW()),
('Sport', 'Un sport', 1, 0, NOW()),
('Célébrité', 'Une personnalité connue', 1, 0, NOW());

-- ENGLISH CATEGORIES (langue = 1)
INSERT INTO categories (nom, description, actif, langue, created_at) VALUES
('Country', 'A country in the world', 1, 1, NOW()),
('City', 'A city', 1, 1, NOW()),
('Animal', 'An animal', 1, 1, NOW()),
('Fruit/Vegetable', 'A fruit or vegetable', 1, 1, NOW()),
('Profession', 'A job or profession', 1, 1, NOW()),
('First Name', 'A first name', 1, 1, NOW()),
('Brand', 'A famous brand', 1, 1, NOW()),
('Sport', 'A sport', 1, 1, NOW()),
('Celebrity', 'A famous person', 1, 1, NOW());

-- ARABIC CATEGORIES (langue = 2)
INSERT INTO categories (nom, description, actif, langue, created_at) VALUES
('بلد', 'بلد في العالم', 1, 2, NOW()),
('مدينة', 'مدينة', 1, 2, NOW()),
('حيوان', 'حيوان', 1, 2, NOW()),
('فاكهة/خضار', 'فاكهة أو خضار', 1, 2, NOW()),
('مهنة', 'مهنة', 1, 2, NOW()),
('اسم', 'اسم شخصي', 1, 2, NOW()),
('علامة تجارية', 'علامة تجارية مشهورة', 1, 2, NOW()),
('رياضة', 'رياضة', 1, 2, NOW()),
('مشهور', 'شخصية مشهورة', 1, 2, NOW());

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================
SELECT '=== CATEGORIES BY LANGUAGE ===' as info;
SELECT 
    CASE langue 
        WHEN 0 THEN 'FRANCAIS'
        WHEN 1 THEN 'ANGLAIS'
        WHEN 2 THEN 'ARABE'
    END as langue,
    COUNT(*) as total
FROM categories 
GROUP BY langue;

SELECT '=== TOTAL CATEGORIES ===' as info;
SELECT COUNT(*) as total FROM categories;

SELECT '=== SAMPLE CATEGORIES ===' as info;
SELECT id, nom, langue FROM categories LIMIT 10;