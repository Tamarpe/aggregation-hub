package com.tamar.support.repository;

import com.tamar.support.model.Case;

import java.util.Map;

public interface RedisRepository {

    /**
     * Return all found cases.
     *
     * @return all the cases from Redis.
     */
    Map<Object, Object> findAllCases();

    /**
     * Return refresh the fetched data.
     */
    void refresh();

    /**
     * Delete all the data from Redis.
     */
    void delete();

    /**
     * Add a case to Redis.
     *
     * @param caseToAdd the case that should be added.
     */
    void add(Case caseToAdd);
}
