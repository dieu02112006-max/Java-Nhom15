USE `java_nhom15`;

-- Run once on an existing Dayflow database.
ALTER TABLE `skins`
  ADD COLUMN `background_image_url` varchar(500) DEFAULT NULL AFTER `preview_url`;

UPDATE `skins`
SET `name` = 'Mặc định',
    `code` = 'default',
    `primary_color` = '#4568DF',
    `secondary_color` = '#EFA9C8',
    `background_color` = '#F7F8FD',
    `text_color` = '#202235',
    `preview_url` = '/themes/default.svg',
    `background_image_url` = '/themes/default.svg',
    `is_default` = 1,
    `is_active` = 1
WHERE `id` = 1;

UPDATE `skins`
SET `name` = 'Tối',
    `preview_url` = '/themes/dark.svg',
    `background_image_url` = '/themes/dark.svg',
    `is_default` = 0,
    `is_active` = 1
WHERE `id` = 2;

UPDATE `skins`
SET `name` = 'Đại dương',
    `preview_url` = '/themes/ocean.svg',
    `background_image_url` = '/themes/ocean.svg',
    `is_default` = 0,
    `is_active` = 1
WHERE `id` = 3;

INSERT INTO `skins` (`id`, `name`, `code`, `primary_color`, `secondary_color`, `background_color`, `text_color`, `preview_url`, `background_image_url`, `is_default`, `is_active`)
VALUES (4, 'Sáng', 'light', '#1F4E79', '#5B9BD5', '#FFFFFF', '#222222', '/themes/light.svg', '/themes/light.svg', 0, 1)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `primary_color` = VALUES(`primary_color`),
  `secondary_color` = VALUES(`secondary_color`),
  `background_color` = VALUES(`background_color`),
  `text_color` = VALUES(`text_color`),
  `preview_url` = VALUES(`preview_url`),
  `background_image_url` = VALUES(`background_image_url`),
  `is_default` = 0,
  `is_active` = 1;

UPDATE `skins` SET `is_default` = 0 WHERE `id` <> 1;
ALTER TABLE `skins` AUTO_INCREMENT = 5;
