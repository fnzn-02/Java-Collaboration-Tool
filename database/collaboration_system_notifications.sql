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
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `notification_id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `type` enum('TASK_ASSIGNED','TASK_COMPLETED','COMMENT_ADDED','MENTION','DUE_DATE_REMINDER','PROJECT_INVITED') NOT NULL,
  `title` varchar(200) NOT NULL,
  `message` text,
  `related_task_id` int DEFAULT NULL,
  `related_project_id` int DEFAULT NULL,
  `is_read` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notification_id`),
  KEY `idx_notification_user` (`user_id`,`is_read`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,1,'TASK_ASSIGNED','새로운 작업이 할당되었습니다','작업 \"테스트 프로젝트\"이(가) 할당되었습니다.',1,NULL,1,'2025-11-11 09:11:41'),(2,2,'TASK_ASSIGNED','새로운 작업이 할당되었습니다','작업 \"기획부\"이(가) 할당되었습니다.',2,NULL,1,'2025-11-11 14:12:53'),(3,3,'TASK_ASSIGNED','새로운 작업이 할당되었습니다','작업 \"기획부\"이(가) 할당되었습니다.',2,NULL,1,'2025-11-11 14:12:58'),(4,1,'TASK_ASSIGNED','새로운 작업이 할당되었습니다','작업 \"기획부\"이(가) 할당되었습니다.',2,NULL,1,'2025-11-11 14:13:19'),(5,3,'TASK_ASSIGNED','새로운 작업이 할당되었습니다','작업 \"프로젝트\"이(가) 할당되었습니다.',3,NULL,1,'2025-11-11 14:15:56'),(6,3,'TASK_ASSIGNED','새로운 작업이 할당되었습니다','작업 \"테스트중\"이(가) 할당되었습니다.',4,NULL,1,'2025-11-11 14:20:17'),(7,3,'TASK_ASSIGNED','새로운 작업이 할당되었습니다','작업 \"확인좀\"이(가) 할당되었습니다.',6,NULL,1,'2025-11-11 21:59:21'),(8,3,'TASK_ASSIGNED','새로운 작업이 할당되었습니다','작업 \"진행 처리 테스트\"이(가) 할당되었습니다.',5,NULL,0,'2025-11-11 22:06:08'),(9,2,'TASK_ASSIGNED','새로운 작업이 할당되었습니다','작업 \"진행 처리 테스트\"이(가) 할당되었습니다.',5,NULL,0,'2025-11-11 22:14:54');
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
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
