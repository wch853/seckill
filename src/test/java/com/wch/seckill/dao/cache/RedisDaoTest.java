package com.wch.seckill.dao.cache;

import com.wch.seckill.dao.SeckillDao;
import com.wch.seckill.entity.Seckill;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:spring/spring-dao.xml")
public class RedisDaoTest {

    @Resource
    private RedisDao redisDao;

    @Resource
    private SeckillDao seckillDao;

    private static final Logger log = LoggerFactory.getLogger(RedisDaoTest.class);

    @Test
    public void testSeckill() throws Exception {
        long id = 1002;
        Seckill seckill = redisDao.getSeckill(id);
        if (null == seckill) {
            seckill = seckillDao.queryById(id);
            if (null != seckill) {
                String result = redisDao.putSeckill(seckill);
                log.info("put result: {}", result);
                seckill = redisDao.getSeckill(id);
                log.info("get result: {}", seckill);
            }
        }
    }

}