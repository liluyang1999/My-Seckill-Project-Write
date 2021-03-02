package com.example.lly.service.impl;

import com.example.lly.aop.SeckillLock;
import com.example.lly.dao.mapper.OrderInfoMapper;
import com.example.lly.dao.mapper.ProductMapper;
import com.example.lly.dao.mapper.SeckillInfoMapper;
import com.example.lly.dao.mapper.rbac.UserMapper;
import com.example.lly.dto.ExecutedResult;
import com.example.lly.dto.StateExposer;
import com.example.lly.entity.OrderInfo;
import com.example.lly.entity.Product;
import com.example.lly.entity.SeckillInfo;
import com.example.lly.entity.rbac.User;
import com.example.lly.module.redis.RedisComponent;
import com.example.lly.service.SeckillService;
import com.example.lly.service.UserSecurityService;
import com.example.lly.util.BaseUtil;
import com.example.lly.util.encryption.MD5Util;
import com.example.lly.util.enumeration.SeckillStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SeckillServiceImpl implements SeckillService {


    private static final Logger logger = LoggerFactory.getLogger(SeckillServiceImpl.class);
    private final SeckillInfoMapper seckillInfoMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Serializable> redisTemplate;
    private final RedisComponent redisComponent;
    private final UserSecurityService userSecurityService;

    @Autowired
    public SeckillServiceImpl(SeckillInfoMapper seckillInfoMapper, OrderInfoMapper orderInfoMapper, ProductMapper productMapper, UserMapper userMapper, RedisTemplate<String, Serializable> redisTemplate, RedisComponent redisComponent, UserSecurityService userSecurityService) {
        this.seckillInfoMapper = seckillInfoMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.productMapper = productMapper;
        this.redisTemplate = redisTemplate;
        this.redisComponent = redisComponent;
        this.userSecurityService = userSecurityService;
    }

    @Override
    public SeckillInfo getSeckillInfoById(Integer id) {
        SeckillInfo seckillInfo = (SeckillInfo) redisTemplate.opsForHash().get("seckillInfos", id);
        if (seckillInfo == null) {
            seckillInfo = seckillInfoMapper.queryById(id);
        }
        return seckillInfo;
    }

    @Override
    public Product getProductById(Integer id) {
        Product product = (Product) redisTemplate.opsForHash().get("products", id);
        if (product == null) {
            product = productMapper.queryById(id);
        }
        return product;
    }

    @Override
    public List<SeckillInfo> getAllSeckillInfo() {
        List<SeckillInfo> seckillInfoList = redisComponent.getAllSeckillInfos();
        if (seckillInfoList == null) {
            seckillInfoList = seckillInfoMapper.queryAll();
        }
        return seckillInfoList;
    }

    @Override
    public List<Product> getAllProduct() {
        List<Product> productList = redisComponent.getAllProducts();
        if (productList == null) {
            productList = productMapper.queryAll();
        }
        return productList;
    }

    @Override
    public List<SeckillInfo> getAllSeckillInfoInProgress() {
        List<SeckillInfo> seckillInfoInProgressList = redisComponent.getAllSeckillInfosInProgress();
        if (seckillInfoInProgressList == null) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            seckillInfoInProgressList = seckillInfoMapper.queryAllSeckillInfoInProgress(now);
        }
        return seckillInfoInProgressList;
    }

    @Override
    public List<SeckillInfo> getAllSeckillInfoInFuture() {
        List<SeckillInfo> seckillInfoInFutureList = redisComponent.getAllSeckillInfosInFuture();
        if (seckillInfoInFutureList == null) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            seckillInfoInFutureList = seckillInfoMapper.queryAllSeckillInfoInFuture(now);
        }
        return seckillInfoInFutureList;
    }


    @Override
    public StateExposer getCorrespondingStateExposer(Integer seckillInfoId, User user) {
        HashOperations<String, String, Serializable> hashOperations = redisTemplate.opsForHash();
        String stateExposerKey = "stateExposer:" + seckillInfoId;
        String seckillInfoKey = "seckillInfoCollection";
        SeckillInfo seckillInfo;

        String username = user.getUsername();
//        if (hashOperations.hasKey(stateExposerKey, username)) {
//            return (StateExposer) hashOperations.get(stateExposerKey, username);
//        }

        if (hashOperations.hasKey(seckillInfoKey, seckillInfoId)) {
            seckillInfo = (SeckillInfo) hashOperations.get(seckillInfoKey, seckillInfoId);
        } else {
            seckillInfo = seckillInfoMapper.queryById(seckillInfoId);  //缓存中没有，则从数据库中查找
        }

        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime startTime = seckillInfo.getStartTime().toLocalDateTime();
        LocalDateTime endTime = seckillInfo.getEndTime().toLocalDateTime();
        System.out.println("seckillInfo: " + seckillInfo);
        StateExposer stateExposer;
        if (currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
            //在秒杀时间段内，响应一个地址给客户端，同时存入缓存
            String encodedUrl = addSaltAndEncode(seckillInfoId, username);
            stateExposer = new StateExposer(true, encodedUrl, seckillInfoId,
                    Timestamp.valueOf(currentTime),
                    Timestamp.valueOf(startTime),
                    Timestamp.valueOf(endTime));
        } else {
            stateExposer = new StateExposer(false, null, seckillInfoId,
                    Timestamp.valueOf(currentTime),
                    Timestamp.valueOf(startTime),
                    Timestamp.valueOf(endTime));
        }
        hashOperations.put(stateExposerKey, username, stateExposer);
        return stateExposer;
    }

    private String addSaltAndEncode(Integer seckillInfoId, String username) {
        String randomSalt = BaseUtil.generateRandomSalt(10);
        String url = seckillInfoId + "/" + randomSalt + "/" + username;
        return MD5Util.encodeString(url);
    }


    /**
     * @param username          该用户的账号
     * @param seckillInfoId     参加的秒杀活动的id
     * @param checkedEncodedUrl 用户早前拿到的md5加密过的url，用以验证真实性
     * @return 返回秒杀处理结果，成功与否
     */
    @Override
    @SeckillLock
    @Transactional(rollbackFor = Exception.class)
    public ExecutedResult executeSeckillTask(String username, Integer seckillInfoId, String checkedEncodedUrl) {
        //缓存中拿出加密Url值进行比对, 为空则非法访问
        String cacheKey = "stateExposer:" + seckillInfoId;
        StateExposer stateExposer = ((StateExposer) redisTemplate.opsForHash().get(cacheKey, username));
        if (stateExposer == null) {
            logger.error("未检测到该链接");
            return new ExecutedResult(seckillInfoId, SeckillStateType.EMPTY);
        }

        String trueEncodedUrl = stateExposer.getEncodedUrl();
        if (trueEncodedUrl == null || !trueEncodedUrl.equals(checkedEncodedUrl)) {
            logger.error("请勿篡改秒杀");
            return new ExecutedResult(seckillInfoId, SeckillStateType.TAMPER);
        }

        LocalDateTime currentTime = LocalDateTime.now();
        int reduceCount = seckillInfoMapper.decreaseNumber(seckillInfoId, Timestamp.valueOf(currentTime));
        if (reduceCount <= 0) {
            //秒杀失败
            logger.error("没有成功减少库存, 活动已结束, 秒杀失败");
            return new ExecutedResult(seckillInfoId, SeckillStateType.FINISH);
        } else {
            //扣减库存成功，继续检查是否属于重复秒杀
            OrderInfo orderInfo = new OrderInfo(seckillInfoId,
                    userSecurityService.getUserByUsername(username).getId(),
                    Short.valueOf("1"),
                    Timestamp.valueOf(currentTime));
            int insertCount = orderInfoMapper.insert(orderInfo);
            if (insertCount <= 0) {
                logger.error("请勿重复秒杀");
                return new ExecutedResult(seckillInfoId, SeckillStateType.DUPLICATE);
            } else {
                redisComponent.putAllSeckillInfos();
                //秒杀成功, 返回包含订单信息的orderinfo
                return new ExecutedResult(seckillInfoId, SeckillStateType.SUCCESS, orderInfo);
            }
        }
    }

    public boolean hasOrderBefore(Integer seckillInfoId, Integer userId) {
        return (orderInfoMapper.queryById(seckillInfoId, userId) != null);
    }


    @Override
    @SeckillLock
    @Transactional(rollbackFor = Exception.class)
    public ExecutedResult executeSeckillTask(String username, Integer seckillInfoId) {
        LocalDateTime currentTime = LocalDateTime.now();
        int reduceCount = seckillInfoMapper.decreaseNumber(seckillInfoId, Timestamp.valueOf(currentTime));
        if (reduceCount <= 0) {
            //秒杀失败
            logger.error("没有成功减少库存, 不在时间范围内或商品已售完, 秒杀失败");
            return new ExecutedResult(seckillInfoId, SeckillStateType.FINISH);
        } else {
            //扣减库存成功，继续检查是否属于重复秒杀
            OrderInfo orderInfo = new OrderInfo(seckillInfoId,
                    userSecurityService.getUserByUsername(username).getId(),
                    Short.valueOf("1"),
                    Timestamp.valueOf(currentTime));
            //秒杀成功, 返回包含订单信息的orderinfo
            return new ExecutedResult(seckillInfoId, SeckillStateType.SUCCESS, orderInfo);
        }
    }


}
