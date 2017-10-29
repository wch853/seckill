package com.wch.seckill.dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.wch.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisDao {

    private final JedisPool jedisPool;

    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

    private static final Logger log = LoggerFactory.getLogger(RedisDao.class);

    public RedisDao(String host, int port, String password) {
        jedisPool = new JedisPool(new JedisPoolConfig(), host, port, 0, password);
    }

    /**
     * 在缓存中获取Seckill对象
     *
     * @param seckillId
     * @return
     */
    public Seckill getSeckill(long seckillId) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String key = "seckill:" + seckillId;
            // 序列化操作
            byte[] bytes = jedis.get(key.getBytes());
            if (null != bytes) {
                // 空对象
                Seckill seckill = schema.newMessage();
                // 将seckill赋值(反序列化)
                ProtostuffIOUtil.mergeFrom(bytes, seckill, schema);
                return seckill;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return null;
    }

    /**
     * 将Seckill对象存入缓存
     *
     * @param seckill
     * @return 存入成功返回字符串"OK"
     */
    public String putSeckill(Seckill seckill) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String key = "seckill:" + seckill.getSeckillId();
            byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema,
                    LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
            // 设置超时缓存(1小时)
            int timeout = 60 * 60;
            return jedis.setex(key.getBytes(), timeout, bytes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (null != jedis) {
                jedis.close();
            }
        }
        return null;
    }
}
