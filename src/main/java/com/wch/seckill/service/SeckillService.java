package com.wch.seckill.service;

import com.wch.seckill.dto.Exposer;
import com.wch.seckill.dto.SeckillExecution;
import com.wch.seckill.entity.Seckill;
import com.wch.seckill.exception.RepeatKillException;
import com.wch.seckill.exception.SeckillCloseException;
import com.wch.seckill.exception.SeckillException;

import java.util.List;

public interface SeckillService {

    /**
     * 查询所有秒杀记录
     *
     * @return
     */
    List<Seckill> getSeckillList();

    /**
     * 查询单个秒杀记录
     *
     * @param seckillId
     * @return
     */
    Seckill getSeckillById(long seckillId);

    /**
     * 秒杀开启时输出秒杀接口地址，否则输出系统时间和秒杀时间
     *
     * @param seckillId
     * @return
     */
    Exposer exportSeckillUrl(long seckillId);


    /**
     * 执行秒杀操作
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5)
            throws SeckillException, SeckillCloseException, RepeatKillException;

    /**
     * 通过存储过程执行秒杀操作
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     */
    SeckillExecution executeSeckillByProcedure(long seckillId, long userPhone, String md5);

}
