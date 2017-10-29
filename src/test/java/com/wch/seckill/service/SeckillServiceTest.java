package com.wch.seckill.service;

import com.wch.seckill.dto.Exposer;
import com.wch.seckill.dto.SeckillExecution;
import com.wch.seckill.entity.Seckill;
import com.wch.seckill.exception.SeckillException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-*.xml"})
public class SeckillServiceTest {

    @Resource
    private SeckillService seckillService;

    private static final Logger log = LoggerFactory.getLogger(SeckillServiceTest.class);

    @Test
    public void getSeckillList() throws Exception {
        List<Seckill> seckillList = seckillService.getSeckillList();
        log.info("list: {}", seckillList);
    }

    @Test
    public void getSeckillById() throws Exception {
        long id = 1000;
        Seckill seckill = seckillService.getSeckillById(id);
        log.info("seckill: {}", seckill);
    }

    @Test
    public void exportSeckillUrl() throws Exception {
        long id = 1000;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        log.info("exposer: {}", exposer);
        /*
        Exposer{
            exposed=true,
            md5='e5f266cad5767c887f89a7613e2a657e',
            seckillId=1000,
            now=0,
            start=0,
            end=0
        }
         */
    }

    @Test
    public void executeSeckill() throws Exception {
        long id = 1000;
        long userPhone = 15261865887L;
        String md5 = "e5f266cad5767c887f89a7613e2a657e";
        SeckillExecution seckillExecution = seckillService.executeSeckill(id, userPhone, md5);
        log.info("execution: {}", seckillExecution);
    }

    @Test
    public void testSeckillLogic() throws Exception {
        long id = 1000;
        long userPhone = 15261865887L;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        if (exposer.isExposed()) {
            log.info("exposer: {}", exposer);
            String md5 = exposer.getMd5();
            try {
                SeckillExecution seckillExecution = seckillService.executeSeckill(id, userPhone, md5);
                log.info("execution: {}", seckillExecution);
            } catch (SeckillException e) {
                log.error("exception: {}", e.getMessage());
            }
        } else {
            log.warn("exposer: {}", exposer);
        }
    }

    @Test
    public void executeSeckillByProcedure() throws Exception {
        long id = 1000;
        long userPhone = 15261865887L;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        if (exposer.isExposed()) {
            String md5 = exposer.getMd5();
            SeckillExecution execution = seckillService.executeSeckillByProcedure(id, userPhone, md5);
            log.info("seckill state: {}", execution.getStateInfo());
        }
    }
}
