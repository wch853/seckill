package com.wch.seckill.service.impl;

import com.wch.seckill.dao.SeckillDao;
import com.wch.seckill.dao.SuccessKilledDao;
import com.wch.seckill.dao.cache.RedisDao;
import com.wch.seckill.dto.Exposer;
import com.wch.seckill.dto.SeckillExecution;
import com.wch.seckill.entity.Seckill;
import com.wch.seckill.entity.SuccessKilled;
import com.wch.seckill.enums.SeckillStateEnum;
import com.wch.seckill.exception.RepeatKillException;
import com.wch.seckill.exception.SeckillCloseException;
import com.wch.seckill.exception.SeckillException;
import com.wch.seckill.service.SeckillService;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Resource
    private SeckillDao seckillDao;

    @Resource
    private SuccessKilledDao successKilledDao;

    @Resource
    private RedisDao redisDao;

    private static final Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);

    // 用于混淆的盐值
    private static final String SLAT = "!@#$%^&*seckill*&^%$#@!";

    /**
     * 查询所有秒杀记录
     *
     * @return
     */
    @Override
    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0, Short.MAX_VALUE);
    }

    /**
     * 查询单个秒杀记录
     *
     * @param seckillId
     * @return
     */
    @Override
    public Seckill getSeckillById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    /**
     * 秒杀开启时输出秒杀接口地址，否则输出系统时间和秒杀时间
     *
     * @param seckillId
     * @return
     */
    @Override
    public Exposer exportSeckillUrl(long seckillId) {
        /**
         * 缓存优化
         * get from cache -> null -> get from db -> put cache
         * 维护一致性：设置超时
         */
        Seckill seckill = redisDao.getSeckill(seckillId);
        if (null == seckill) {
            // 缓存中为null，访问数据库查找
            seckill = seckillDao.queryById(seckillId);
            if (null == seckill) {
                // 数据库中为null
                return new Exposer(false, seckillId);
            } else {
                // 数据库中能找到相应记录，将其存入缓存
                redisDao.putSeckill(seckill);
            }
        }

        // 不在秒杀时间范围内
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        Date now = new Date();
        if (now.getTime() < startTime.getTime() || now.getTime() > endTime.getTime()) {
            return new Exposer(false, seckillId, now.getTime(), startTime.getTime(), endTime.getTime());
        }

        // 转化特定字符串的过程，不可逆
        String md5 = this.getMD5(seckillId);
        return new Exposer(true, md5, seckillId);
    }

    private String getMD5(long seckillId) {
        String base = seckillId + "/" + SLAT;
        return DigestUtils.md5DigestAsHex(base.getBytes());
    }

    /**
     * 执行秒杀操作
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    @Override
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, SeckillCloseException, RepeatKillException {
        try {
            // md5验证出错
            if (null == md5 || !getMD5(seckillId).equals(md5)) {
                throw new SeckillException("seckill data rewrite");
            }

            // 执行秒杀逻辑：减库存 + 记录购买行为
            Date now = new Date();
            // 记录购买行为
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            if (insertCount <= 0) {
                // 重复秒杀
                throw new RepeatKillException("seckill repeat");
            } else {
                // 减库存
                int updateCount = seckillDao.reduceNumber(seckillId, now);
                if (updateCount <= 0) {
                    // 没有更新记录
                    throw new SeckillCloseException("seckill is closed");
                } else {
                    // 秒杀成功，commit
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException | RepeatKillException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new SeckillException("seckill error: " + e.getMessage());
        }
    }

    /**
     * 通过存储过程执行秒杀操作
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    @Override
    public SeckillExecution executeSeckillByProcedure(long seckillId, long userPhone, String md5) {
        // md5验证出错
        if (null == md5 || !getMD5(seckillId).equals(md5)) {
            return new SeckillExecution(seckillId, SeckillStateEnum.DATA_REWRITE);
        }

        Date killTime = new Date();
        Map<String, Object> map = new HashMap<>();
        map.put("seckillId", seckillId);
        map.put("userPhone", userPhone);
        map.put("killTime", killTime);
        map.put("result", null);
        try {
            seckillDao.executeSeckill(map);
            Integer result = MapUtils.getInteger(map, "result", -2);
            if (result == SeckillStateEnum.SUCCESS.getState()) {
                SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS);
            } else {
                return new SeckillExecution(seckillId, SeckillStateEnum.stateOf(result));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
        }
    }
}
