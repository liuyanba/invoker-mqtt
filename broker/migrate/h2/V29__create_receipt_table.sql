
DROP TABLE IF EXISTS `t_read_report`;
CREATE TABLE `t_read_report` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NULL,
  `_type` tinyint NULL,
  `_line` int(11) NULL,
  `_target` varchar(64) NULL,
  `_dt` bigint(20) NOT NULL DEFAULT 0,
  INDEX `read_report_index` (`_uid`, `_type`, `_line`, `_target`)
);

DROP TABLE IF EXISTS `t_user_read_report`;
CREATE TABLE `t_user_read_report` (
    `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `_rid` int(11) NOT NULL,
    `_uid` varchar(64) NOT NULL,
    `_seq` bigint(20) NOT NULL,
    `_dt` DATETIME NOT NULL DEFAULT NOW(),
    INDEX `user_read_report_index` (`_uid` DESC, `_seq` DESC),
    UNIQUE INDEX `user_read_report_index2` (`_uid` DESC, `_rid` DESC)
);

DROP TABLE IF EXISTS `t_delivery_report`;
CREATE TABLE `t_delivery_report` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_uid` varchar(64) NOT NULL,
  `_dt` bigint(20) NOT NULL,
  UNIQUE INDEX `delivery_index` (`_uid`)
);


DROP TABLE IF EXISTS `t_user_delivery_report`;
CREATE TABLE `t_user_delivery_report` (
  `id` int(11) NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `_rid` varchar(64) NOT NULL,
  `_uid` varchar(64) NOT NULL,
  `_seq` bigint(20) NOT NULL,
  `_dt` DATETIME NOT NULL DEFAULT NOW(),
  INDEX `user_delivery_index` (`_uid` DESC, `_seq` DESC),
  UNIQUE INDEX `user_delivery_index2` (`_uid` DESC, `_rid` DESC)
);

DROP TABLE IF EXISTS `t_user_token`;
CREATE TABLE `t_user_token` (
        `id` int(11) NOT NULL AUTO_INCREMENT,
        `user_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
        `user_client` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
        `user_token` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
        `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
        `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        `meny_fluous` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;