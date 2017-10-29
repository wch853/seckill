package com.wch.seckill.web;

import com.wch.seckill.dto.Exposer;
import com.wch.seckill.dto.SeckillExecution;
import com.wch.seckill.dto.SeckillResult;
import com.wch.seckill.entity.Seckill;
import com.wch.seckill.enums.SeckillStateEnum;
import com.wch.seckill.exception.RepeatKillException;
import com.wch.seckill.exception.SeckillCloseException;
import com.wch.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController {

    @Resource
    private SeckillService seckillService;

    private static final Logger log = LoggerFactory.getLogger(SeckillController.class);

    /**
     * 列表页
     *
     * @param model
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String list(Model model) {
        List<Seckill> seckillList = seckillService.getSeckillList();
        model.addAttribute("list", seckillList);
        return "list";
    }

    /**
     * 详情页
     *
     * @param seckillId
     * @param model
     * @return
     */
    @RequestMapping(value = "/{seckillId}/detail", method = RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Long seckillId, Model model) {
        if (null == seckillId) {
            return "redirect:/seckill/list";
        }

        Seckill seckill = seckillService.getSeckillById(seckillId);
        if (null == seckill) {
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill", seckill);
        return "detail";
    }

    /**
     * 暴露秒杀接口
     *
     * @param seckillId
     * @return
     */
    @RequestMapping(value = "/{seckillId}/exposer",
            method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<Exposer> export(@PathVariable("seckillId") Long seckillId) {
        SeckillResult<Exposer> result;
        try {
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            result = new SeckillResult<Exposer>(true, exposer);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result = new SeckillResult<Exposer>(false, e.getMessage());
        }
        return result;
    }

    /**
     * 执行秒杀
     *
     * @param seckillId
     * @param md5
     * @param userPhone
     * @return
     */
    @RequestMapping(value = "/{seckillId}/{md5}/execution",
            method = RequestMethod.POST,
            produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long seckillId,
                                                   @PathVariable("md5") String md5,
                                                   @CookieValue(value = "killPhone", required = false) Long userPhone) {
        if (null == userPhone) {
            return new SeckillResult<>(false, "未注册");
        }
        SeckillExecution seckillExecution;
        SeckillResult<SeckillExecution> result;
        try {
            /*
             // 使用普通方式
             seckillExecution = seckillService.executeSeckill(seckillId, userPhone, md5);
              */
            // 使用存储过程方式应对高并发
            seckillExecution = seckillService.executeSeckillByProcedure(seckillId, userPhone, md5);
            result = new SeckillResult<>(true, seckillExecution);
        } catch (RepeatKillException e) {
            seckillExecution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            result = new SeckillResult<>(true, seckillExecution);
        } catch (SeckillCloseException e) {
            seckillExecution = new SeckillExecution(seckillId, SeckillStateEnum.END);
            result = new SeckillResult<>(true, seckillExecution);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            seckillExecution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            result = new SeckillResult<>(true, seckillExecution);
        }
        return result;
    }

    /**
     * 获取系统时间
     *
     * @return
     */
    @RequestMapping(value = "/time/now", method = RequestMethod.GET)
    @ResponseBody
    public SeckillResult<Long> time() {
        Date now = new Date();
        return new SeckillResult<>(true, now.getTime());
    }
}
