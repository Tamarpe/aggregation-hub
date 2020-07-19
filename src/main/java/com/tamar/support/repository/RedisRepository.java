package com.tamar.support.repository;

import java.util.Date;
import java.util.LinkedHashMap;
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
   * @param crmId     the CRM ID.
   * @param crmName   the CRM Name.
   * @param caseToAdd the case that should be mapped and added.
   */
  void mapAndAdd(int crmId, String crmName, LinkedHashMap caseToAdd);

  /**
   * Return last execution of fetching data from a CRM.
   *
   * @param crmId the crm ID.
   * @return the date of the last execution.
   */
  Date getLastExecutionResource(int crmId);
}
