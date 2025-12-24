CREATE DATABASE IF NOT EXISTS baccalaureat_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE baccalaureat_db;

CREATE TABLE IF NOT EXISTS categories (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          nom VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    actif BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mots (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    texte VARCHAR(100) NOT NULL,
    categorie_id BIGINT NOT NULL,
    lettre CHAR(1) NOT NULL,
    valide BOOLEAN DEFAULT TRUE,
    nb_utilisations INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (categorie_id) REFERENCES categories(id) ON DELETE CASCADE,
    UNIQUE KEY unique_mot_categorie (texte, categorie_id),
    INDEX idx_lettre (lettre),
    INDEX idx_texte (texte)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS joueurs (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       pseudo VARCHAR(50) NOT NULL UNIQUE,
    score_total INT DEFAULT 0,
    nb_parties INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS parties (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       joueur_id BIGINT NOT NULL,
                                       lettre CHAR(1) NOT NULL,
    score INT DEFAULT 0,
    mode VARCHAR(50) NOT NULL,
    duree_seconde INT,
    date_partie TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (joueur_id) REFERENCES joueurs(id) ON DELETE CASCADE,
    INDEX idx_date (date_partie)
    ) ENGINE=InnoDB;

-- Insertion de catégories par défaut
INSERT INTO categories (nom, description) VALUES
                                              ('Prénom', 'Prénoms de personnes'),
                                              ('Pays', 'Noms de pays'),
                                              ('Ville', 'Noms de villes'),
                                              ('Animal', 'Espèces animales'),
                                              ('Métier', 'Professions'),
                                              ('Fruit/Légume', 'Fruits et légumes'),
                                              ('Marque', 'Marques commerciales'),
                                              ('Célébrité', 'Personnalités connues')
    ON DUPLICATE KEY UPDATE nom=nom;