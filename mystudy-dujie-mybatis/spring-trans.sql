/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80024
 Source Host           : localhost:3306
 Source Schema         : spring-trans

 Target Server Type    : MySQL
 Target Server Version : 80024
 File Encoding         : 65001

 Date: 30/12/2021 14:30:02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for account_info
-- ----------------------------
DROP TABLE IF EXISTS `account_info`;
CREATE TABLE `account_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `account_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `blance` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of account_info
-- ----------------------------
BEGIN;
INSERT INTO `account_info` VALUES (3, '9988776644', 500);
INSERT INTO `account_info` VALUES (4, '9988776644', 500);
COMMIT;

-- ----------------------------
-- Table structure for dept
-- ----------------------------
DROP TABLE IF EXISTS `dept`;
CREATE TABLE `dept` (
  `id` int NOT NULL AUTO_INCREMENT,
  `department_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of dept
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for employee
-- ----------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
  `id` int NOT NULL AUTO_INCREMENT,
  `last_name` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `gender` int DEFAULT NULL,
  `dept_id` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of employee
-- ----------------------------
BEGIN;
INSERT INTO `employee` VALUES (1, '1', '1', 1, 1);
COMMIT;

-- ----------------------------
-- Table structure for product_info
-- ----------------------------
DROP TABLE IF EXISTS `product_info`;
CREATE TABLE `product_info` (
  `product_id` int NOT NULL AUTO_INCREMENT,
  `product_name` varchar(255) NOT NULL,
  `store` int DEFAULT NULL,
  PRIMARY KEY (`product_id`,`product_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of product_info
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `user_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of user_info
-- ----------------------------
BEGIN;
INSERT INTO `user_info` VALUES ('test', 'test', 1);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
