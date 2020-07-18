package com.tamar.support.repository;

import com.tamar.support.model.Case;

import java.util.Date;
import java.util.Map;

public interface RedisRepository {

    /**
     * Return all found cases.
     *
     * @return all the cases from Redis.
     */
    Map<Object, Object> findAllCases();

    /**
     * Refresh the fetched data.
     *
     * @return true if refresh is allowed, false otherwise.
     */
    boolean refresh();

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

    /**
     * Return last execution of fetching a resource.
     *
     * @param aggregationsCrmResource the resource.
     * @return the date of the last execution.
     */
    Date getLastExecutionResource(String aggregationsCrmResource);
}
