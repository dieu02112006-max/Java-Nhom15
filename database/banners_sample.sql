-- Chạy trong MySQL (database java_nhom15): tạo bảng banners và nạp 3 ảnh banner mẫu.
CREATE TABLE IF NOT EXISTS `banners` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `title` varchar(200) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `link_url` varchar(300) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DROP TABLE IF EXISTS `topics`;
CREATE TABLE IF NOT EXISTS `topics` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `title` varchar(200) NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `link_url` varchar(300) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO `banners` (`title`,`image_url`,`link_url`,`sort_order`,`is_active`) VALUES
('Sắp xếp ngày. Sống chủ động.','/home/banners/banner-1.png',NULL,1,1),
('Hiểu rõ tiền. Chi tiêu thông minh.','/home/banners/banner-2.png',NULL,2,1),
('Việc nhỏ mỗi ngày. Kết quả lớn.','/home/banners/banner-3.png',NULL,3,1);

INSERT INTO `topics` (`title`,`image_url`,`link_url`,`sort_order`,`is_active`) VALUES
('Giáng sinh an lành.','/home/topics/noel.png',NULL,1,1),
('Đại dương mênh mông.','/home/topics/ocean.png',NULL,2,1),
('Tết đoàn viên.','/home/topics/tet.png',NULL,3,1);