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
-- Temporary view structure for view `project_statistics`
--

DROP TABLE IF EXISTS `project_statistics`;
/*!50001 DROP VIEW IF EXISTS `project_statistics`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `project_statistics` AS SELECT 
 1 AS `project_id`,
 1 AS `project_name`,
 1 AS `total_tasks`,
 1 AS `completed_tasks`,
 1 AS `in_progress_tasks`,
 1 AS `overdue_tasks`,
 1 AS `member_count`,
 1 AS `total_comments`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `user_task_statistics`
--

DROP TABLE IF EXISTS `user_task_statistics`;
/*!50001 DROP VIEW IF EXISTS `user_task_statistics`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `user_task_statistics` AS SELECT 
 1 AS `user_id`,
 1 AS `username`,
 1 AS `assigned_tasks`,
 1 AS `created_tasks`,
 1 AS `completed_tasks`,
 1 AS `overdue_tasks`*/;
SET character_set_client = @saved_cs_client;

--
-- Final view structure for view `project_statistics`
--

/*!50001 DROP VIEW IF EXISTS `project_statistics`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`CollaborationSystem`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `project_statistics` AS select `p`.`project_id` AS `project_id`,`p`.`project_name` AS `project_name`,count(distinct `t`.`task_id`) AS `total_tasks`,sum((case when (`t`.`status` = '완료') then 1 else 0 end)) AS `completed_tasks`,sum((case when (`t`.`status` = '진행중') then 1 else 0 end)) AS `in_progress_tasks`,sum((case when (`t`.`is_overdue` = true) then 1 else 0 end)) AS `overdue_tasks`,count(distinct `pm`.`user_id`) AS `member_count`,count(distinct `c`.`comment_id`) AS `total_comments` from (((`projects` `p` left join `tasks` `t` on(((`p`.`project_id` = `t`.`project_id`) and (`t`.`status` <> '삭제')))) left join `project_members` `pm` on((`p`.`project_id` = `pm`.`project_id`))) left join `comments` `c` on((`t`.`task_id` = `c`.`task_id`))) where (`p`.`is_active` = true) group by `p`.`project_id`,`p`.`project_name` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `user_task_statistics`
--

/*!50001 DROP VIEW IF EXISTS `user_task_statistics`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`CollaborationSystem`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `user_task_statistics` AS select `u`.`user_id` AS `user_id`,`u`.`username` AS `username`,count(distinct (case when (`t`.`assignee_id` = `u`.`user_id`) then `t`.`task_id` end)) AS `assigned_tasks`,count(distinct (case when (`t`.`creator_id` = `u`.`user_id`) then `t`.`task_id` end)) AS `created_tasks`,count(distinct (case when (`t`.`completed_by_id` = `u`.`user_id`) then `t`.`task_id` end)) AS `completed_tasks`,count(distinct (case when ((`t`.`assignee_id` = `u`.`user_id`) and (`t`.`is_overdue` = true)) then `t`.`task_id` end)) AS `overdue_tasks` from (`users` `u` left join `tasks` `t` on((((`t`.`assignee_id` = `u`.`user_id`) or (`t`.`creator_id` = `u`.`user_id`) or (`t`.`completed_by_id` = `u`.`user_id`)) and (`t`.`status` <> '삭제')))) group by `u`.`user_id`,`u`.`username` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-11-12  7:16:53
