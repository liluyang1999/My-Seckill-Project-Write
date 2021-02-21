package com.example.lly.controller;

import com.example.lly.dao.mapper.rbac.PermissionMapper;
import com.example.lly.dao.mapper.rbac.RoleMapper;
import com.example.lly.dao.mapper.rbac.UserMapper;
import com.example.lly.dto.ExecutedResult;
import com.example.lly.entity.rbac.Permission;
import com.example.lly.entity.rbac.Role;
import com.example.lly.entity.rbac.User;
import com.example.lly.service.HttpService;
import com.example.lly.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RestController
public class TestingController {

    @GetMapping("/queryUser")
    public User query1() {
        return userMapper.queryById(1);
    }

    @GetMapping("/queryRole")
    public Role query2() {
        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return roleMapper.queryById(1);
    }

    @GetMapping("/queryPermission")
    public Permission query3() {
        return permissionMapper.queryById(11);
    }


    @RequestMapping("/sendCurrentTime")
    public void sendCurrentTime() throws JSONException {
        String result = httpService.sendMessage("http://localhost:8081/getCurrentTime", HttpMethod.GET, null);
        JSONObject object = new JSONObject(result);
        System.out.println(object.get("data"));
    }


    private static final int corePoolScale = Runtime.getRuntime().availableProcessors();   //JVM可用的核心数量
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolScale, corePoolScale + 1, 101,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>(2000));
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private PermissionMapper permissionMapper;
    @Autowired
    private HttpService httpService;
    @Autowired
    private SeckillService seckillService;

    @PostMapping("/startRedissonLock")
    public void startRedissonLock(HttpServletRequest request) {
        Integer seckillInfoId = Integer.valueOf(request.getParameter("seckillInfoId"));
        String username = request.getParameter("username");
        logger.info("开始模拟秒杀");
        for (int i = 0; i < 100000; i++) {
            Runnable task = () -> {
                ExecutedResult executedResult = seckillService.executeSeckillTask(username, seckillInfoId);
                logger.info(executedResult.getStateMsg());
            };
            executor.submit(task);
        }
        logger.info("结束模拟秒杀");
    }
}
