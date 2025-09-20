CREATE TABLE `herb_flavors` (
  `id` int NOT NULL AUTO_INCREMENT,
  `herb_id` int NOT NULL,
  `value` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_herb_flavors_pair` (`herb_id`,`value`),
  CONSTRAINT `fk_herb_flavors_herb` FOREIGN KEY (`herb_id`) REFERENCES `herbs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=614 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `herb_formulas` (
  `id` int NOT NULL AUTO_INCREMENT,
  `herb_id` int NOT NULL,
  `value` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_herb_formulas_pair` (`herb_id`,`value`),
  CONSTRAINT `fk_herb_formulas_herb` FOREIGN KEY (`herb_id`) REFERENCES `herbs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=28719 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `herb_images` (
  `id` int NOT NULL AUTO_INCREMENT,
  `herb_id` int NOT NULL,
  `filename` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `data` mediumblob NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_herbs_image_file` (`herb_id`,`filename`),
  CONSTRAINT `fk_herbs_image_herb` FOREIGN KEY (`herb_id`) REFERENCES `herbs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=391 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `herb_indications` (
  `id` int NOT NULL AUTO_INCREMENT,
  `herb_id` int NOT NULL,
  `value` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_herb_indications_pair` (`herb_id`,`value`),
  CONSTRAINT `fk_herb_indications_herb` FOREIGN KEY (`herb_id`) REFERENCES `herbs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1736 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `herb_meridians` (
  `id` int NOT NULL AUTO_INCREMENT,
  `herb_id` int NOT NULL,
  `value` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_herb_meridians_pair` (`herb_id`,`value`),
  CONSTRAINT `fk_herb_meridians_herb` FOREIGN KEY (`herb_id`) REFERENCES `herbs` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=925 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `herbs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `source_url` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_zh` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name_pinyin` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `desc_zh` longtext COLLATE utf8mb4_unicode_ci,
  `desc_en` longtext COLLATE utf8mb4_unicode_ci,
  `appearance` longtext COLLATE utf8mb4_unicode_ci,
  `property` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_source_url` (`source_url`)
) ENGINE=InnoDB AUTO_INCREMENT=391 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
