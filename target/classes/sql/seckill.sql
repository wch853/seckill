-- 数据库初始化脚本

-- 创建数据库
DROP DATABASE IF EXISTS seckill;
CREATE DATABASE seckill;
-- 使用数据库
USE seckill;
DROP TABLE IF EXISTS seckill;
CREATE TABLE seckill (
  `seckill_id`  BIGINT       NOT NULL AUTO_INCREMENT
  COMMENT '商品库存id',
  `name`        VARCHAR(120) NOT NULL
  COMMENT '商品名称',
  `number`      INT          NOT NULL
  COMMENT '库存数量',
  `create_time` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
  COMMENT '创建时间',
  `start_time`  TIMESTAMP    NOT NULL
  COMMENT '秒杀开始时间',
  `end_time`    TIMESTAMP    NOT NULL
  COMMENT '秒杀结束时间',
  PRIMARY KEY (seckill_id),
  KEY idx_start_time(start_time),
  KEY idx_end_time(end_time),
  KEY idx_create_time(create_time)
)
  ENGINE = INNODB
  AUTO_INCREMENT = 1000
  DEFAULT CHARSET = utf8
  COMMENT ='秒杀库存表';

-- 初始化数据
INSERT INTO seckill (name, number, start_time, end_time)
VALUES
  ('1000元秒杀iphone6', 100, '2017-10-28 00:00:00', '2017-10-30 00:00:00'),
  ('800元秒杀iPad2', 200, '2017-10-28 00:00:00', '2017-10-30 00:00:00'),
  ('600元秒杀小米4', 300, '2017-10-28 00:00:00', '2017-10-30 00:00:00'),
  ('400元秒杀红米Note', 400, '2017-10-28 00:00:00', '2017-10-30 00:00:00');

-- 秒杀成功明细表
-- 用户登录认证相关信息
DROP TABLE IF EXISTS success_killed;
CREATE TABLE success_killed (
  `seckill_id`  BIGINT    NOT NULL
  COMMENT '秒杀商品id',
  `user_phone`  BIGINT    NOT NULL
  COMMENT '用户手机号',
  `state`       TINYINT   NOT NULL DEFAULT -1
  COMMENT '状态标识:-1:无效 0:成功 1:已付款 2:已发货',
  `create_time` TIMESTAMP NOT NULL
  COMMENT '创建时间',
  PRIMARY KEY (seckill_id, user_phone), /*联合主键*/
  KEY idx_create_time(create_time)
)
  ENGINE = INNODB
  DEFAULT CHARSET = utf8
  COMMENT ='秒杀成功明细表';

-- 秒杀执行存储过程
DELIMITER //
CREATE PROCEDURE seckill.execute_seckill(IN v_seckill_id BIGINT,
  IN v_phone BIGINT, IN v_kill_time TIMESTAMP, OUT r_result INT)
  BEGIN
    DECLARE insert_count INT DEFAULT 0;
    START TRANSACTION;
    INSERT IGNORE success_killed (seckill_id, user_phone, state, create_time)
      VALUES (v_seckill_id, v_phone, 0, v_kill_time);
    /* 返回上条操作影响的行数，<0时表示sql错误/未执行 */
    SELECT ROW_COUNT() INTO insert_count;
    IF insert_count < 0 THEN
      ROLLBACK;
      /*标明原因-2：系统异常 */
      SET r_result = -2;
    ELSEIF insert_count = 0 THEN
      ROLLBACK;
      /*标明原因-1：重复秒杀 */
      SET r_result = -1;
    ELSE
      UPDATE seckill
      SET number = number - 1
      WHERE seckill_id = v_seckill_id
            AND end_time > v_kill_time
            AND start_time < v_kill_time
            AND number > 0;
      SELECT ROW_COUNT() INTO insert_count;
      IF insert_count < 0 THEN
        ROLLBACK;
        SET r_result = -2;
      ELSEIF insert_count = 0 THEN
          ROLLBACK;
          SET r_result = 0;
      ELSE
        COMMIT;
        SET r_result = 1;
      END IF;
    END IF;
  END //
DELIMITER ;