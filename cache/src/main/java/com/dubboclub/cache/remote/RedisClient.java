package com.dubboclub.cache.remote;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.dubboclub.cache.config.CacheConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bieber on 2015/5/26.
 */
public class RedisClient extends RemoteClient{

    private static final String REDIS_CONNECT="cache.redis.connect";
    
    private static  ShardedJedisPool JEDIS_POOL;
    
    static {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        CacheConfig.appendProperties(poolConfig,RedisClient.class);
        Object redisConnect = CacheConfig.getProperty(REDIS_CONNECT);
        if(redisConnect==null|| StringUtils.isEmpty(redisConnect.toString())){
            throw new IllegalArgumentException("cache.redis.connect must not empty");
        }
        String connectString = redisConnect.toString();
        String[] connects = Constants.COMMA_SPLIT_PATTERN.split(connectString);
        List<JedisShardInfo> jedisShardInfoList = new ArrayList<JedisShardInfo>();
        for(String connect:connects){
            String[] splits = connect.split(":");
            JedisShardInfo jedisShardInfo = new JedisShardInfo(splits[0],splits[1]);
            jedisShardInfoList.add(jedisShardInfo);
        }
        JEDIS_POOL=new ShardedJedisPool(poolConfig,jedisShardInfoList);

    }
    
    public void cacheValue(byte[] key,byte[] bytes,int expireSecond){
        ShardedJedis jedis = JEDIS_POOL.getResource();
        try{
            if(expireSecond>0){
                jedis.setex(key,expireSecond,bytes);
            }else{
                jedis.set(key,bytes);
            }
        }finally {
            JEDIS_POOL.returnResource(jedis);
        }
    }
    
    public byte[] getValue(byte[] key){
        ShardedJedis jedis = JEDIS_POOL.getResource();
        try{
            return jedis.get(key);
        }finally {
            JEDIS_POOL.returnResource(jedis);
        }
    }
    

    

}
