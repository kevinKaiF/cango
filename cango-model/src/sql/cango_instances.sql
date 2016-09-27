-- ----------------------------
-- Table structure for cango_instances
-- ----------------------------
DROP TABLE IF EXISTS `cango_instances`;
CREATE TABLE `cango_instances` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `host` varchar(255) NOT NULL,
  `port` int(11) NOT NULL,
  `db_name` varchar(255) NOT NULL,
  `db_type` int(11) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `slave_id` int(11) DEFAULT NULL,
  `black_tables` varchar(255) DEFAULT NULL,
  `state` int(11) NOT NULL,
  `create_time` date NOT NULL,
  `update_time` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
