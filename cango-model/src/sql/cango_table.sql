-- ----------------------------
-- Table structure for cango_table
-- ----------------------------
DROP TABLE IF EXISTS `cango_table`;
CREATE TABLE `cango_table` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instances_name` varchar(255) NOT NULL,
  `table_name` varchar(255) NOT NULL,
  `create_time` date DEFAULT NULL,
  `update_time` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
