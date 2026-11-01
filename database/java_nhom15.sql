-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Oct 27, 2026 at 04:12 PM
-- Server version: 8.4.3
-- PHP Version: 8.3.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `java_nhom15`
--

-- --------------------------------------------------------

--
-- Table structure for table `attachments`
--

CREATE TABLE `attachments` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `entity_type` enum('task','transaction') NOT NULL,
  `entity_id` bigint NOT NULL,
  `file_url` varchar(1000) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `uploaded_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `attachments`
--

INSERT INTO `attachments` (`id`, `user_id`, `entity_type`, `entity_id`, `file_url`, `file_name`, `uploaded_at`) VALUES
(1, 1, 'task', 1, '/uploads/tasks/qua-goi-y.pdf', 'qua-goi-y.pdf', '2026-10-23 17:36:21'),
(2, 1, 'transaction', 2, '/uploads/receipts/an-trua.jpg', 'an-trua.jpg', '2026-10-23 17:36:21');

-- --------------------------------------------------------

--
-- Table structure for table `budgets`
--

CREATE TABLE `budgets` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `category_id` bigint DEFAULT NULL COMMENT 'NULL la ngan sach tong',
  `amount` decimal(15,2) NOT NULL,
  `period` enum('weekly','monthly','yearly') NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `alert_threshold` int NOT NULL DEFAULT '80'
) ;

--
-- Dumping data for table `budgets`
--

INSERT INTO `budgets` (`id`, `user_id`, `category_id`, `amount`, `period`, `start_date`, `end_date`, `alert_threshold`) VALUES
(1, 1, 2, 3000000.00, 'monthly', '2026-10-01', '2026-10-31', 80),
(2, 1, NULL, 8000000.00, 'monthly', '2026-10-01', '2026-10-31', 85),
(3, 2, NULL, 5000000.00, 'monthly', '2026-10-01', '2026-10-31', 80);

-- --------------------------------------------------------

--
-- Table structure for table `categories`
--

CREATE TABLE `categories` (
  `id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL COMMENT 'NULL la danh muc mac dinh cua he thong',
  `parent_id` bigint DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `type` enum('income','expense') NOT NULL,
  `icon` varchar(100) DEFAULT NULL,
  `color` varchar(30) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `categories`
--

INSERT INTO `categories` (`id`, `user_id`, `parent_id`, `name`, `type`, `icon`, `color`) VALUES
(1, NULL, NULL, 'Luong', 'income', 'wallet', '#2E7D32'),
(2, NULL, NULL, 'An uong', 'expense', 'restaurant', '#E65100'),
(3, 1, NULL, 'Di lai', 'expense', 'bus', '#1565C0'),
(4, 1, 2, 'Ca phe', 'expense', 'coffee', '#795548');

-- --------------------------------------------------------

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `type` varchar(50) NOT NULL,
  `title` varchar(200) NOT NULL,
  `content` text NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `notifications`
--

INSERT INTO `notifications` (`id`, `user_id`, `type`, `title`, `content`, `is_read`, `created_at`) VALUES
(1, 1, 'task_reminder', 'Cong viec sap den han', 'Cong viec Mua qua sinh nhat sap den han.', 0, '2026-10-23 17:36:21'),
(2, 1, 'budget_alert', 'Canh bao ngan sach', 'Ban da su dung gan het ngan sach an uong.', 0, '2026-10-23 17:36:21');

-- --------------------------------------------------------

--
-- Table structure for table `recurring_transactions`
--

CREATE TABLE `recurring_transactions` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `wallet_id` bigint NOT NULL,
  `category_id` bigint DEFAULT NULL,
  `amount` decimal(15,2) NOT NULL,
  `type` enum('income','expense','transfer') NOT NULL,
  `note` text,
  `frequency` enum('daily','weekly','monthly','yearly') NOT NULL,
  `next_run_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1'
) ;

--
-- Dumping data for table `recurring_transactions`
--

INSERT INTO `recurring_transactions` (`id`, `user_id`, `wallet_id`, `category_id`, `amount`, `type`, `note`, `frequency`, `next_run_date`, `end_date`, `is_active`) VALUES
(1, 1, 2, 2, 2500000.00, 'expense', 'Tien thue nha', 'monthly', '2026-11-01', NULL, 1);

-- --------------------------------------------------------

--
-- Table structure for table `reminders`
--

CREATE TABLE `reminders` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `task_id` bigint DEFAULT NULL,
  `budget_id` bigint DEFAULT NULL,
  `remind_at` datetime NOT NULL,
  `is_sent` tinyint(1) NOT NULL DEFAULT '0'
) ;

--
-- Dumping data for table `reminders`
--

INSERT INTO `reminders` (`id`, `user_id`, `task_id`, `budget_id`, `remind_at`, `is_sent`) VALUES
(1, 1, 1, NULL, '2026-10-31 18:00:00', 0),
(2, 1, NULL, 1, '2026-10-28 09:00:00', 0);

-- --------------------------------------------------------

--
-- Table structure for table `savings_goals`
--

CREATE TABLE `savings_goals` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `name` varchar(150) NOT NULL,
  `target_amount` decimal(15,2) NOT NULL,
  `current_amount` decimal(15,2) NOT NULL DEFAULT '0.00',
  `deadline` date DEFAULT NULL,
  `status` enum('active','completed','cancelled') NOT NULL DEFAULT 'active'
) ;

--
-- Dumping data for table `savings_goals`
--

INSERT INTO `savings_goals` (`id`, `user_id`, `name`, `target_amount`, `current_amount`, `deadline`, `status`) VALUES
(1, 1, 'Mua laptop', 20000000.00, 1500000.00, '2027-06-30', 'active'),
(2, 2, 'Du lich', 10000000.00, 800000.00, '2027-03-31', 'active');

-- --------------------------------------------------------

--
-- Table structure for table `skins`
--

CREATE TABLE `skins` (
  `id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `code` varchar(50) NOT NULL,
  `primary_color` varchar(7) NOT NULL,
  `secondary_color` varchar(7) NOT NULL,
  `background_color` varchar(7) NOT NULL,
  `text_color` varchar(7) NOT NULL,
  `preview_url` varchar(500) DEFAULT NULL,
  `background_image_url` varchar(500) DEFAULT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT '0',
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `skins`
--

INSERT INTO `skins` (`id`, `name`, `code`, `primary_color`, `secondary_color`, `background_color`, `text_color`, `preview_url`, `background_image_url`, `is_default`, `is_active`, `created_at`) VALUES
(1, 'Mặc định', 'default', '#4568DF', '#EFA9C8', '#F7F8FD', '#202235', '/themes/default.svg', '/themes/default.svg', 1, 1, '2026-10-23 17:36:21'),
(3, 'Đại dương', 'ocean', '#0077B6', '#48CAE4', '#CAF0F8', '#03045E', '/themes/ocean.svg', '/themes/ocean.svg', 0, 1, '2026-10-23 17:36:21'),
(4, 'Sáng', 'light', '#1F4E79', '#5B9BD5', '#FFFFFF', '#222222', '/themes/light.svg', '/themes/light.svg', 0, 1, '2026-10-27 07:35:27');

-- --------------------------------------------------------

--
-- Table structure for table `tags`
--

CREATE TABLE `tags` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `color` varchar(30) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `tags`
--

INSERT INTO `tags` (`id`, `user_id`, `name`, `color`) VALUES
(1, 1, 'Gap', '#EF4444'),
(2, 1, 'Hoc tap', '#3B82F6'),
(3, 2, 'Ca nhan', '#8B5CF6');

-- --------------------------------------------------------

--
-- Table structure for table `tasks`
--

CREATE TABLE `tasks` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `list_id` bigint NOT NULL,
  `parent_task_id` bigint DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `description` text,
  `category` varchar(80) NOT NULL DEFAULT 'Cá nhân',
  `location` varchar(500) DEFAULT NULL,
  `status` enum('todo','in_progress','done') NOT NULL DEFAULT 'todo',
  `priority` enum('low','medium','high') NOT NULL DEFAULT 'medium',
  `due_date` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `estimated_cost` decimal(15,2) DEFAULT NULL,
  `recurrence_rule` varchar(255) DEFAULT NULL,
  `position` int NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ;

--
-- Dumping data for table `tasks`
--

INSERT INTO `tasks` (`id`, `user_id`, `list_id`, `parent_task_id`, `title`, `description`, `category`, `location`, `status`, `priority`, `due_date`, `completed_at`, `estimated_cost`, `recurrence_rule`, `position`, `created_at`, `updated_at`) VALUES
(1, 1, 1, NULL, 'Mua qua sinh nhat', 'Chon va mua qua', 'Cá nhân', NULL, 'todo', 'high', '2026-11-01 18:00:00', NULL, 500000.00, NULL, 1, '2026-10-23 17:36:21', '2026-10-23 17:36:21'),
(2, 1, 2, NULL, 'Hoan thanh bai tap Java', 'Lam bai tap chuong 3', 'Cá nhân', NULL, 'in_progress', 'medium', '2026-10-30 23:00:00', NULL, 0.00, NULL, 1, '2026-10-23 17:36:21', '2026-10-23 17:36:21'),
(3, 2, 3, NULL, 'Dat lich kham', 'Kham suc khoe dinh ky', 'Cá nhân', NULL, 'todo', 'low', '2026-11-05 09:00:00', NULL, 300000.00, NULL, 1, '2026-10-23 17:36:21', '2026-10-23 17:36:21'),
(4, 4, 4, NULL, 'đi học', NULL, 'Học tập', 'cao đẳng công thương', 'todo', 'medium', '2026-10-26 22:54:00', NULL, NULL, NULL, 0, '2026-10-27 08:54:05', '2026-10-27 08:54:05'),
(5, 4, 4, NULL, 'đi ăn', 'mì cay', 'Sức khỏe', 'mì cay sansin', 'todo', 'high', '2026-10-27 10:30:00', NULL, NULL, NULL, 0, '2026-10-27 09:10:32', '2026-10-27 09:10:32');

-- --------------------------------------------------------

--
-- Table structure for table `task_lists`
--

CREATE TABLE `task_lists` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `name` varchar(120) NOT NULL,
  `color` varchar(30) DEFAULT NULL,
  `position` int NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `task_lists`
--

INSERT INTO `task_lists` (`id`, `user_id`, `name`, `color`, `position`) VALUES
(1, 1, 'Cong viec ca nhan', '#4F46E5', 1),
(2, 1, 'Hoc tap', '#10B981', 2),
(3, 2, 'Viec can lam', '#F97316', 1),
(4, 4, 'Công việc', '#4568DF', 1);

-- --------------------------------------------------------

--
-- Table structure for table `task_tags`
--

CREATE TABLE `task_tags` (
  `task_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `task_tags`
--

INSERT INTO `task_tags` (`task_id`, `tag_id`) VALUES
(1, 1),
(2, 2),
(3, 3);

-- --------------------------------------------------------

--
-- Table structure for table `transactions`
--

CREATE TABLE `transactions` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `wallet_id` bigint NOT NULL,
  `category_id` bigint DEFAULT NULL,
  `type` enum('income','expense','transfer') NOT NULL,
  `amount` decimal(15,2) NOT NULL,
  `transaction_date` datetime NOT NULL,
  `note` text,
  `to_wallet_id` bigint DEFAULT NULL,
  `receipt_url` varchar(500) DEFAULT NULL,
  `task_id` bigint DEFAULT NULL,
  `recurring_id` bigint DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ;

--
-- Dumping data for table `transactions`
--

INSERT INTO `transactions` (`id`, `user_id`, `wallet_id`, `category_id`, `type`, `amount`, `transaction_date`, `note`, `to_wallet_id`, `receipt_url`, `task_id`, `recurring_id`, `deleted_at`, `created_at`) VALUES
(1, 1, 2, 1, 'income', 12000000.00, '2026-10-01 09:00:00', 'Luong thang 10', NULL, NULL, NULL, NULL, NULL, '2026-10-23 17:36:21'),
(2, 1, 1, 2, 'expense', 65000.00, '2026-10-22 12:00:00', 'An trua', NULL, '/uploads/hoa-don-an-trua.jpg', NULL, NULL, NULL, '2026-10-23 17:36:21'),
(3, 1, 2, 4, 'expense', 350000.00, '2026-10-24 14:00:00', 'Mua qua sinh nhat', NULL, NULL, 1, NULL, NULL, '2026-10-23 17:36:21'),
(4, 1, 1, NULL, 'transfer', 200000.00, '2026-10-24 16:00:00', 'Chuyen tien sang ngan hang', 2, NULL, NULL, NULL, NULL, '2026-10-23 17:36:21'),
(5, 1, 2, 2, 'expense', 2500000.00, '2026-10-01 08:00:00', 'Tien thue nha thang 10', NULL, NULL, NULL, 1, NULL, '2026-10-23 17:36:21');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint NOT NULL,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `full_name` varchar(150) NOT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `currency_default` varchar(3) NOT NULL DEFAULT 'VND',
  `timezone` varchar(60) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
  `skin_id` bigint DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `role` varchar(50) NOT NULL DEFAULT 'USER'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `email`, `password_hash`, `full_name`, `avatar_url`, `currency_default`, `timezone`, `skin_id`, `created_at`, `updated_at`, `role`) VALUES
(1, 'demo@example.com', 'REPLACE_WITH_BCRYPT_HASH', 'Nguyen Van An', NULL, 'VND', 'Asia/Ho_Chi_Minh', 1, '2026-10-23 17:36:21', '2026-10-23 17:36:21', 'USER'),
(2, 'binh@example.com', 'REPLACE_WITH_BCRYPT_HASH', 'Tran Thi Binh', NULL, 'VND', 'Asia/Ho_Chi_Minh', NULL, '2026-10-23 17:36:21', '2026-10-23 17:36:21', 'USER'),
(3, 'nguyenvanb@gmail.com', '$2a$10$cs7mKGyhXkR5zt0D4upW4.iY0H6o2UgDEMlChGAomGtAvUuTNEGr6', 'Nguyễn Văn B', NULL, 'VND', 'Asia/Ho_Chi_Minh', NULL, '2026-10-26 23:29:35', '2026-10-27 06:37:01', 'ADMIN'),
(4, 'nguyenvana@gmail.com', '$2a$10$dqLE04ah3t9l6PLjq2yhQeSx5iJ5chTVhmQFWGrQcwC26oEOW1IBa', 'Nguyễn Văn A', NULL, 'VND', 'Asia/Ho_Chi_Minh', 1, '2026-10-26 23:40:20', '2026-10-27 00:37:46', 'USER');

-- --------------------------------------------------------

--
-- Table structure for table `wallets`
--

CREATE TABLE `wallets` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `type` enum('cash','bank','credit','e-wallet') NOT NULL,
  `balance` decimal(15,2) NOT NULL DEFAULT '0.00',
  `currency` varchar(3) NOT NULL DEFAULT 'VND',
  `is_archived` tinyint(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `wallets`
--

INSERT INTO `wallets` (`id`, `user_id`, `name`, `type`, `balance`, `currency`, `is_archived`) VALUES
(1, 1, 'Tien mat', 'cash', 1500000.00, 'VND', 0),
(2, 1, 'Ngan hang', 'bank', 5000000.00, 'VND', 0),
(3, 2, 'Vi ca nhan', 'e-wallet', 800000.00, 'VND', 0);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `attachments`
--
ALTER TABLE `attachments`
  ADD PRIMARY KEY (`id`,`user_id`),
  ADD UNIQUE KEY `uq_attachments_id` (`id`),
  ADD KEY `fk_attachments_user` (`user_id`),
  ADD KEY `idx_attachments_entity` (`entity_type`,`entity_id`);

--
-- Indexes for table `budgets`
--
ALTER TABLE `budgets`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_budgets_user` (`user_id`),
  ADD KEY `fk_budgets_category` (`category_id`);

--
-- Indexes for table `categories`
--
ALTER TABLE `categories`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_categories_parent` (`parent_id`),
  ADD KEY `idx_categories_owner_type` (`user_id`,`type`);

--
-- Indexes for table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`,`user_id`),
  ADD UNIQUE KEY `uq_notifications_id` (`id`),
  ADD KEY `idx_notifications_user_read` (`user_id`,`is_read`);

--
-- Indexes for table `recurring_transactions`
--
ALTER TABLE `recurring_transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_recurring_user` (`user_id`),
  ADD KEY `fk_recurring_wallet` (`wallet_id`),
  ADD KEY `fk_recurring_category` (`category_id`),
  ADD KEY `idx_recurring_next_run` (`is_active`,`next_run_date`);

--
-- Indexes for table `reminders`
--
ALTER TABLE `reminders`
  ADD PRIMARY KEY (`id`,`user_id`),
  ADD UNIQUE KEY `uq_reminders_id` (`id`),
  ADD KEY `fk_reminders_user` (`user_id`),
  ADD KEY `fk_reminders_task` (`task_id`),
  ADD KEY `fk_reminders_budget` (`budget_id`),
  ADD KEY `idx_reminders_pending` (`is_sent`,`remind_at`);

--
-- Indexes for table `savings_goals`
--
ALTER TABLE `savings_goals`
  ADD PRIMARY KEY (`id`,`user_id`),
  ADD UNIQUE KEY `uq_savings_goals_id` (`id`),
  ADD KEY `fk_savings_goals_user` (`user_id`);

--
-- Indexes for table `skins`
--
ALTER TABLE `skins`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `code` (`code`);

--
-- Indexes for table `tags`
--
ALTER TABLE `tags`
  ADD PRIMARY KEY (`id`,`user_id`),
  ADD UNIQUE KEY `uq_tags_id` (`id`),
  ADD UNIQUE KEY `uq_tags_user_name` (`user_id`,`name`);

--
-- Indexes for table `tasks`
--
ALTER TABLE `tasks`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_tasks_list_user` (`list_id`,`user_id`),
  ADD KEY `fk_tasks_parent` (`parent_task_id`),
  ADD KEY `idx_tasks_user_status_due` (`user_id`,`status`,`due_date`);

--
-- Indexes for table `task_lists`
--
ALTER TABLE `task_lists`
  ADD PRIMARY KEY (`id`,`user_id`),
  ADD UNIQUE KEY `uq_task_lists_id` (`id`),
  ADD KEY `fk_task_lists_user` (`user_id`);

--
-- Indexes for table `task_tags`
--
ALTER TABLE `task_tags`
  ADD PRIMARY KEY (`task_id`,`tag_id`),
  ADD KEY `fk_task_tags_tag` (`tag_id`);

--
-- Indexes for table `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_transactions_wallet` (`wallet_id`),
  ADD KEY `fk_transactions_category` (`category_id`),
  ADD KEY `fk_transactions_to_wallet` (`to_wallet_id`),
  ADD KEY `fk_transactions_recurring` (`recurring_id`),
  ADD KEY `idx_transactions_user_date` (`user_id`,`transaction_date`),
  ADD KEY `idx_transactions_task` (`task_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `fk_users_skin` (`skin_id`);

--
-- Indexes for table `wallets`
--
ALTER TABLE `wallets`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_wallets_user` (`user_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `attachments`
--
ALTER TABLE `attachments`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `budgets`
--
ALTER TABLE `budgets`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `categories`
--
ALTER TABLE `categories`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `recurring_transactions`
--
ALTER TABLE `recurring_transactions`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `reminders`
--
ALTER TABLE `reminders`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `savings_goals`
--
ALTER TABLE `savings_goals`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `skins`
--
ALTER TABLE `skins`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `tags`
--
ALTER TABLE `tags`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `tasks`
--
ALTER TABLE `tasks`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `task_lists`
--
ALTER TABLE `task_lists`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `wallets`
--
ALTER TABLE `wallets`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `attachments`
--
ALTER TABLE `attachments`
  ADD CONSTRAINT `fk_attachments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `budgets`
--
ALTER TABLE `budgets`
  ADD CONSTRAINT `fk_budgets_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_budgets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `categories`
--
ALTER TABLE `categories`
  ADD CONSTRAINT `fk_categories_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_categories_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `notifications`
--
ALTER TABLE `notifications`
  ADD CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `recurring_transactions`
--
ALTER TABLE `recurring_transactions`
  ADD CONSTRAINT `fk_recurring_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_recurring_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_recurring_wallet` FOREIGN KEY (`wallet_id`) REFERENCES `wallets` (`id`);

--
-- Constraints for table `reminders`
--
ALTER TABLE `reminders`
  ADD CONSTRAINT `fk_reminders_budget` FOREIGN KEY (`budget_id`) REFERENCES `budgets` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reminders_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reminders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `savings_goals`
--
ALTER TABLE `savings_goals`
  ADD CONSTRAINT `fk_savings_goals_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `tags`
--
ALTER TABLE `tags`
  ADD CONSTRAINT `fk_tags_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `tasks`
--
ALTER TABLE `tasks`
  ADD CONSTRAINT `fk_tasks_list_user` FOREIGN KEY (`list_id`,`user_id`) REFERENCES `task_lists` (`id`, `user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_tasks_parent` FOREIGN KEY (`parent_task_id`) REFERENCES `tasks` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_tasks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `task_lists`
--
ALTER TABLE `task_lists`
  ADD CONSTRAINT `fk_task_lists_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `task_tags`
--
ALTER TABLE `task_tags`
  ADD CONSTRAINT `fk_task_tags_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_task_tags_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `transactions`
--
ALTER TABLE `transactions`
  ADD CONSTRAINT `fk_transactions_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_transactions_recurring` FOREIGN KEY (`recurring_id`) REFERENCES `recurring_transactions` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_transactions_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_transactions_to_wallet` FOREIGN KEY (`to_wallet_id`) REFERENCES `wallets` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_transactions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_transactions_wallet` FOREIGN KEY (`wallet_id`) REFERENCES `wallets` (`id`);

--
-- Constraints for table `users`
--
ALTER TABLE `users`
  ADD CONSTRAINT `fk_users_skin` FOREIGN KEY (`skin_id`) REFERENCES `skins` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `wallets`
--
ALTER TABLE `wallets`
  ADD CONSTRAINT `fk_wallets_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;


-- --------------------------------------------------------
-- Bảng banners: ảnh banner hiển thị ở trang giới thiệu (cũng được tự tạo khi chạy ứng dụng)
CREATE TABLE IF NOT EXISTS `banners` (
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
