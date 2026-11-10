package com.fadaiting.camp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

@SpringBootTest//如果在測試類添加了這個註解，單元測試方法之前，會先初始化Spring容器
public class RedisTest {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Test
    public void testSet(){
        //往REDIS中存取一個鍵值 StringRedisTemplate
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
        operations.set("username","jack");
        operations.set("id","1",15, TimeUnit.SECONDS);
    }
    @Test
    public void testGet(){
        ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
//        System.out.println(operations.get("useername"));
    }
}
