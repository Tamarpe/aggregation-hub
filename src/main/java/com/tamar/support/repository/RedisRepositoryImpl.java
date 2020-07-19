package com.tamar.support.repository;

import com.google.common.util.concurrent.RateLimiter;
import com.tamar.support.config.CrmConfig;
import com.tamar.support.model.Case;
import com.tamar.support.model.Crm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Repository
public class RedisRepositoryImpl implements RedisRepository {
  /**
   * The key to be used in Redis for a case.
   */
  private static final String KEY = "case";

  /**
   * The Redis template.
   */
  private RedisTemplate<String, Object> redisTemplate;

  /**
   * The Hash operations.
   */
  private HashOperations hashOperations;

  /**
   * The logger.
   */
  private Logger logger;

  /**
   * The CRM configuration.
   */
  @Autowired
  private CrmConfig crmConfig;

  /**
   * The period time that should passed to be able to refresh.
   */
  @Value("${aggregations.refresh.ratelimit}")
  private Integer aggregationsRefreshPeriodTime;

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
    logger = LoggerFactory.getLogger(RedisRepositoryImpl.class);
  }

  /**
   * Refresh the fetched data.
   *
   * @return true if a refresh is allowed, false otherwise.
   */
  @Scheduled(cron = "${aggregations.auto.refresh}")
  public boolean refresh() {
    Date currentDate = new Date();
    List<Crm> crmList = crmConfig.getList();

    for (int i = 0; i < crmList.size(); i++) {
      Date lastExecutionCrm = getLastExecutionResource(i);
      if (lastExecutionCrm != null && currentDate.getTime() - lastExecutionCrm.getTime() < aggregationsRefreshPeriodTime) {
        logger.warn("The data is already fetched less than the time limit.");
        return false;
      }
    }

    for (int i = 0; i < crmList.size(); i++) {
      refreshCrm(i, crmList.get(i).getUrl(), crmList.get(i).getName(), crmList.get(i).getRateLimit(), crmList.get(i).getPeriodTime());
    }
    return true;
  }

  /**
   * Refresh the data from a CRM.
   *
   * @param crmId      the CRM ID.
   * @param crmUrl     the URL of the CRM.
   * @param crmName    the CRM name.
   * @param rateLimit  the allowed number to execute an API call to the CRM.
   * @param periodTime The period time that should passed to be able to
   *                   execute a call to the CRM.
   */
  public void refreshCrm(int crmId, String crmUrl, String crmName, Integer rateLimit, Integer periodTime) {
    Date currentDate = new Date();
    RestTemplate restTemplate = new RestTemplate();
    RateLimiter rateLimiter = RateLimiter.create(rateLimit);

    if (hashOperations.hasKey("last_execution", crmId)) {
      Date lastExecutionCrm = (Date) hashOperations.get("last_execution", crmId);
      if (currentDate.getTime() - lastExecutionCrm.getTime() < periodTime) {
        logger.warn("The data is already fetched less than the time limit.");
        return;
      }
    }
    if (rateLimiter.tryAcquire()) {
      try {
        logger.info("Fetching data from CRM: " + crmName);
        ResponseEntity<List<Object>> res = restTemplate.exchange(
          crmUrl,
          HttpMethod.GET,
          null,
          new ParameterizedTypeReference<>() {
          });
        List<Object> result = res.getBody();

        for (int i = 0; i < result.size(); i++) {
          if (result.get(i) instanceof ArrayList<?>) {
            ArrayList<LinkedHashMap> fetchedCases = (ArrayList) result.get(i);
            for (LinkedHashMap fetchedCase : fetchedCases) {
              mapAndAdd(crmId, crmName, fetchedCase);
            }
          } else {
            ArrayList<LinkedHashMap> fetchedCases = (ArrayList) result;
            for (LinkedHashMap fetchedCase : fetchedCases) {
              mapAndAdd(crmId, crmName, fetchedCase);
            }
          }
        }

        hashOperations.put("last_execution", crmId, currentDate);
      } catch (HttpStatusCodeException e) {
        e.printStackTrace();
        logger.error("Error found on calling to the API", e);
      }
    }
  }

  /**
   * Map and add a case to Redis.
   *
   * @param crmId     the CRM ID.
   * @param crmName   the CRM Name.
   * @param caseToAdd the case that should be mapped and added.
   */
  public void mapAndAdd(int crmId, String crmName, LinkedHashMap caseToAdd) {
    String id = caseToAdd.get("CaseID") + ";resource:" + crmId;
    SimpleDateFormat fromApi = new SimpleDateFormat("M/d/yyyyh:mm");

    try {
      Date creationDate = fromApi.parse((String) caseToAdd.get("TICKET_CREATION_DATE"));
      Date lastModifiedDate = fromApi.parse((String) caseToAdd.get("LAST_MODIFIED_DATE"));
      Case caseObj = new Case(id,
        (Integer) caseToAdd.get("CaseID"),
        (Integer) caseToAdd.get("CustomerID"),
        (Integer) caseToAdd.get("Provider"),
        (Integer) caseToAdd.get("CREATED_ERROR_CODE"),
        (String) caseToAdd.get("STATUS"),
        creationDate,
        lastModifiedDate,
        (String) caseToAdd.get("PRODUCT_NAME"),
        crmName);

      hashOperations.put(KEY, caseObj.getId(), caseObj);
    } catch (ParseException e) {
      e.printStackTrace();
      logger.error("Error found on parsing the date format", e);
    }
  }

  /**
   * Delete all the data from Redis.
   */
  public void delete() {
    redisTemplate.delete(KEY);
    redisTemplate.delete("last_execution");
  }

  /**
   * Return all found cases.
   *
   * @return all the cases from Redis.
   */
  public Map<Object, Object> findAllCases() {
    return hashOperations.entries(KEY);
  }

  /**
   * Return last execution of fetching data from a CRM.
   *
   * @param crmId the crm ID.
   * @return the date of the last execution.
   */
  public Date getLastExecutionResource(int crmId) {
    if (hashOperations.hasKey("last_execution", crmId)) {
      return (Date) hashOperations.get("last_execution", crmId);
    }
    return null;
  }

}
