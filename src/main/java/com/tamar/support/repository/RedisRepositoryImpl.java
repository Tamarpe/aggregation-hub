package com.tamar.support.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.*;

@Repository
public class RedisRepositoryImpl implements RedisRepository {

  /**
   * The Redis template.
   */
  private RedisTemplate<String, Object> redisTemplate;

  /**
   * The Hash operations.
   */
  private HashOperations hashOperations;

  /**
   * Create a new RedisRepositoryImpl.
   *
   * @param redisTemplate the redis template.
   */
  @Autowired
  public RedisRepositoryImpl(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @PostConstruct
  private void init() {
    hashOperations = redisTemplate.opsForHash();
  }

  /**
   * Get entry from Redis.
   *
   * @param key     the entry's key.
   * @param hashKey the entry's hash key.
   * @return the entry from Redis.
   */
  public Object get(String key, Object hashKey) {
    return hashOperations.get(key, hashKey);
  }

  /**
   * Check if an entry exists in Redis.
   *
   * @param key     the given key.
   * @param hashKey the entry's hash key.
   * @return if there is an entry with a given key and value in Redis.
   */
  public Boolean exists(String key, Object hashKey) {
    return hashOperations.hasKey(key, hashKey);
  }

  /**
   * Add an entry to Redis.
   *
   * @param key      the entry's key.
   * @param id       the ID of the entry.
   * @param objToAdd the object that should be added.
   */
  public void add(String key, Object id, Object objToAdd) {
    hashOperations.put(key, id, objToAdd);
  }

  /**
   * Delete all the data from Redis.
   *
   * @param key the Entry's key.
   */
  public void delete(String key) {
    redisTemplate.delete(key);
  }

  /**
   * Return all entries for a given key.
   *
   * @param key the given key.
   * @return all entries.
   */
  public Map<Object, Object> getAllEntries(String key) {
    return hashOperations.entries(key);
  }

}
