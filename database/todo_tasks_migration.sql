USE `java_nhom15`;

-- Run once on an existing Dayflow database.
ALTER TABLE `tasks`
  ADD COLUMN `category` varchar(80) NOT NULL DEFAULT 'Cá nhân' AFTER `description`,
  ADD COLUMN `location` varchar(500) DEFAULT NULL AFTER `category`;

UPDATE `tasks`
SET `category` = CASE
  WHEN `title` LIKE '%Java%' OR `title` LIKE '%bài tập%' THEN 'Học tập'
  ELSE 'Cá nhân'
END
WHERE `category` = 'Cá nhân';
