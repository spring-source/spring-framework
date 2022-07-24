package com.dujie.version3.config;

import com.dujie.version3.support.RedisCfgProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@PropertySource(value = {"classpath:rediscfg.properties"})
public class SpringRedisConfig {

	@Bean
	public RedisCfgProperties redisCfgProperties() {
		RedisCfgProperties redisCfgProperties = new RedisCfgProperties();
		return redisCfgProperties;
	}

	@Bean
	public JedisPoolConfig jedisPoolConfig(RedisCfgProperties redisCfgProperties) {
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxTotal(redisCfgProperties.getMaxTotal());
		jedisPoolConfig.setMaxIdle(redisCfgProperties.getMaxIdle());
		return jedisPoolConfig;
	}

	@Bean
	public JedisConnectionFactory jedisConnectionFactory(RedisCfgProperties redisCfgProperties) {
		RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(redisCfgProperties.getHost(), redisCfgProperties.getPort());
		standaloneConfig.setDatabase(0);
		JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(standaloneConfig);
		return jedisConnectionFactory;
	}

	@Bean
	public RedisTemplate redisTemplate() {
		RedisTemplate redisTemplate = new RedisTemplate();
		redisTemplate.setConnectionFactory(jedisConnectionFactory(redisCfgProperties()));
		redisTemplate.setDefaultSerializer(new Jackson2JsonRedisSerializer<Object>(Object.class));
		return redisTemplate;
	}
}
