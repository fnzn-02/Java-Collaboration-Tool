-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: collaboration_system
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `task_history`
--

DROP TABLE IF EXISTS `task_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_history` (
  `history_id` int NOT NULL AUTO_INCREMENT,
  `task_id` int NOT NULL,
  `user_id` int NOT NULL,
  `action` enum('생성','수정','완료','삭제','복구','대기','진행중','진행') NOT NULL,
  `field_changed` varchar(50) DEFAULT NULL,
  `old_value` text,
  `new_value` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`history_id`),
  KEY `task_id` (`task_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `task_history_ibfk_1` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`task_id`) ON DELETE CASCADE,
  CONSTRAINT `task_history_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_history`
--

LOCK TABLES `task_history` WRITE;
/*!40000 ALTER TABLE `task_history` DISABLE KEYS */;
INSERT INTO `task_history` VALUES (1,1,1,'생성',NULL,NULL,NULL,'2025-11-11 09:11:25'),(2,1,1,'수정','assignee',NULL,'1','2025-11-11 09:11:41'),(3,1,1,'수정','assignee',NULL,'1','2025-11-11 09:14:58'),(4,1,1,'수정','assignee',NULL,'1','2025-11-11 09:15:04'),(5,1,1,'수정','assignee',NULL,'1','2025-11-11 09:15:20'),(6,1,1,'수정','assignee',NULL,'1','2025-11-11 09:16:41'),(7,1,1,'수정','assignee',NULL,'1','2025-11-11 09:20:50'),(8,1,1,'수정','assignee',NULL,'1','2025-11-11 09:21:57'),(9,1,1,'수정','assignee',NULL,'1','2025-11-11 09:22:03'),(10,1,4,'삭제',NULL,NULL,NULL,'2025-11-11 09:31:28'),(11,2,4,'생성',NULL,NULL,NULL,'2025-11-11 09:31:45'),(12,2,1,'수정','assignee',NULL,'2','2025-11-11 14:12:53'),(13,2,1,'수정','assignee',NULL,'3','2025-11-11 14:12:58'),(14,2,1,'수정','assignee',NULL,'1','2025-11-11 14:13:19'),(15,2,2,'삭제',NULL,NULL,NULL,'2025-11-11 14:15:21'),(16,3,2,'생성',NULL,NULL,NULL,'2025-11-11 14:15:50'),(17,3,2,'수정','assignee',NULL,'3','2025-11-11 14:15:56'),(18,3,3,'삭제',NULL,NULL,NULL,'2025-11-11 14:17:39'),(19,4,1,'생성',NULL,NULL,NULL,'2025-11-11 14:20:11'),(20,4,1,'수정','assignee',NULL,'3','2025-11-11 14:20:17'),(21,4,1,'삭제',NULL,NULL,NULL,'2025-11-11 15:24:00'),(22,5,1,'생성',NULL,NULL,NULL,'2025-11-11 21:35:10'),(23,5,1,'완료',NULL,NULL,NULL,'2025-11-11 21:43:52'),(24,6,1,'생성',NULL,NULL,NULL,'2025-11-11 21:58:31'),(25,6,1,'수정','assignee',NULL,'3','2025-11-11 21:59:21'),(26,6,1,'완료',NULL,NULL,NULL,'2025-11-11 22:05:30'),(27,5,1,'수정','assignee',NULL,'3','2025-11-11 22:06:08'),(28,5,1,'진행',NULL,NULL,NULL,'2025-11-11 22:11:00'),(29,5,1,'대기',NULL,NULL,NULL,'2025-11-11 22:11:03'),(30,5,1,'진행',NULL,NULL,NULL,'2025-11-11 22:11:14'),(31,5,1,'대기',NULL,NULL,NULL,'2025-11-11 22:11:16'),(32,5,1,'진행',NULL,NULL,NULL,'2025-11-11 22:11:20'),(33,5,1,'완료',NULL,NULL,NULL,'2025-11-11 22:11:22'),(34,5,1,'진행',NULL,NULL,NULL,'2025-11-11 22:11:25'),(35,5,1,'대기',NULL,NULL,NULL,'2025-11-11 22:11:30'),(36,6,1,'진행',NULL,NULL,NULL,'2025-11-11 22:11:39'),(37,6,1,'대기',NULL,NULL,NULL,'2025-11-11 22:11:42'),(38,6,1,'완료',NULL,NULL,NULL,'2025-11-11 22:14:38'),(39,5,1,'완료',NULL,NULL,NULL,'2025-11-11 22:14:41'),(40,6,1,'진행',NULL,NULL,NULL,'2025-11-11 22:14:47'),(41,5,1,'진행',NULL,NULL,NULL,'2025-11-11 22:14:49'),(42,5,1,'수정','assignee',NULL,'2','2025-11-11 22:14:54'),(43,6,1,'완료',NULL,NULL,NULL,'2025-11-11 22:14:58'),(44,5,1,'완료',NULL,NULL,NULL,'2025-11-11 22:15:02'),(45,7,1,'생성',NULL,NULL,NULL,'2025-11-11 22:15:27');
/*!40000 ALTER TABLE `task_history` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-12  7:16:53
