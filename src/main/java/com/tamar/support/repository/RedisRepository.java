package com.tamar.support.repository;

import java.util.Map;

public interface RedisRepository {

  /**
   * Return all entries for a given key.
   *
   * @param key the given key.
   * @return all entries.
   */
  Map<Object, Object> getAllEntries(String key);

  /**
   * Delete all the data from Redis.
   *
   * @param key the given key.
   */
  void delete(String key);

  /**
   * Check if an entry exists in Redis.
   *
   * @param key     the given key.
   * @param hashKey the entry's hash key.
   * @return if there is an entry with a given key and value in Redis.
   */
  Boolean exists(String key, Object hashKey);

  /**
   * Get entry from Redis.
   *
   * @param key     the entry's key.
   * @param hashKey the entry's hash key.
   * @return the entry from Redis.
   */
  Object get(String key, Object hashKey);

  /**
   * Add an entry to Redis.
   *
   * @param key      the entry's key.
   * @param id       the ID of the entry.
   * @param objToAdd the object that should be added.
   */
  void add(String key, Object id, Object objToAdd);
}
