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
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `task_id` int NOT NULL AUTO_INCREMENT,
  `project_id` int DEFAULT '1',
  `title` varchar(200) NOT NULL,
  `description` text,
  `priority` enum('높음','중간','낮음') DEFAULT '중간',
  `status` enum('대기','진행중','완료','삭제') DEFAULT '대기',
  `creator_id` int NOT NULL,
  `assignee_id` int DEFAULT NULL,
  `completed_by_id` int DEFAULT NULL,
  `due_date` datetime DEFAULT NULL,
  `is_overdue` tinyint(1) DEFAULT '0',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`task_id`),
  KEY `project_id` (`project_id`),
  KEY `creator_id` (`creator_id`),
  KEY `completed_by_id` (`completed_by_id`),
  KEY `idx_tasks_status` (`status`),
  KEY `idx_tasks_assignee` (`assignee_id`,`status`),
  CONSTRAINT `tasks_ibfk_1` FOREIGN KEY (`project_id`) REFERENCES `projects` (`project_id`),
  CONSTRAINT `tasks_ibfk_2` FOREIGN KEY (`creator_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `tasks_ibfk_3` FOREIGN KEY (`assignee_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `tasks_ibfk_4` FOREIGN KEY (`completed_by_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tasks`
--

LOCK TABLES `tasks` WRITE;
/*!40000 ALTER TABLE `tasks` DISABLE KEYS */;
INSERT INTO `tasks` VALUES (1,1,'테스트 프로젝트','이거 성공해야 밥먹음','높음','삭제',1,1,NULL,'2025-11-11 20:00:00',0,'2025-11-11 09:11:25','2025-11-11 09:31:28',NULL),(2,1,'기획부','기획 처리','높음','삭제',4,1,NULL,NULL,0,'2025-11-11 09:31:45','2025-11-11 14:15:21',NULL),(3,1,'프로젝트','','높음','삭제',2,3,NULL,NULL,0,'2025-11-11 14:15:50','2025-11-11 14:17:39',NULL),(4,1,'테스트중','테스트한다','높음','삭제',1,3,NULL,NULL,0,'2025-11-11 14:20:11','2025-11-11 15:24:00',NULL),(5,1,'진행 처리 테스트','진행 처리 테스트','높음','완료',1,2,1,NULL,0,'2025-11-11 21:35:10','2025-11-11 22:15:02','2025-11-11 22:15:02'),(6,1,'확인좀','확인좀','높음','완료',1,3,1,NULL,0,'2025-11-11 21:58:31','2025-11-11 22:14:58','2025-11-11 22:14:58'),(7,1,'처리중','처리중','높음','대기',1,NULL,NULL,'2025-11-12 07:16:00',0,'2025-11-11 22:15:27','2025-11-11 22:15:27',NULL);
/*!40000 ALTER TABLE `tasks` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-12  7:16:52
