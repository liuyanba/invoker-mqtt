alter table t_friend add column `_blacked` tinyint DEFAULT 0;
update t_friend set `_blacked` = 1 where `_state` = 2;
