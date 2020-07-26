package com.tamar.support.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.tamar.support.config.CrmConfig;
import com.tamar.support.model.Crm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.tamar.support.repository.RedisRepository;
import com.tamar.support.model.Case;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class AggregationHubController {

  /**
   * The key to be used in Redis for a case.
   */
  private static final String CASE_KEY = "case";

  /**
   * The key to be used in Redis for the last execution.
   */
  private static final String LAST_EXECUTION_KEY = "last_execution";

  /**
   * The logger.
   */
  private Logger logger;

  /**
   * The period time that should passed to be able to refresh.
   */
  @Value("${aggregations.refresh.ratelimit}")
  private Integer aggregationsRefreshPeriodTime;

  /**
   * The CRM configuration.
   */
  @Autowired
  private CrmConfig crmConfig;

  /**
   * Redis repository.
   */
  @Autowired
  private RedisRepository redisRepository;


  @PostConstruct
  private void init() {
    logger = LoggerFactory.getLogger(AggregationHubController.class);
  }

  /**
   * MyAggregatedHub homepage.
   *
   * @param model the model name to display.
   * @return template's model
   */
  @GetMapping("/myAggregatedHub")

  public String getCases(Model model) {

    return "myAggregatedHub";
  }

  /**
   * The API call for the cases searching.
   *
   * @param product               the given product.
   * @param provider              the given provider.
   * @param errorCode             the given error code.
   * @param customerId            the given customer ID.
   * @param fromCreationDate      the given from creation date.
   * @param untilCreationDate     the given until creation date.
   * @param fromLastModifiedDate  the given from last modified date.
   * @param untilLastModifiedDate the given until last modified date.
   * @param status                the given status.
   * @return an ArrayList of all the found cases.
   */
  @RequestMapping(value = "/search", method = RequestMethod.GET)
  public @ResponseBody
  ArrayList<Map<String, Object>> searchCases(
    @RequestParam("product") String product,
    @RequestParam("provider") String provider,
    @RequestParam("errorCode") String errorCode,
    @RequestParam("customerId") String customerId,
    @RequestParam("fromCreationDate") String fromCreationDate,
    @RequestParam("untilCreationDate") String untilCreationDate,
    @RequestParam("fromLastModifiedDate") String fromLastModifiedDate,
    @RequestParam("untilLastModifiedDate") String untilLastModifiedDate,
    @RequestParam("status") String status) {
    SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Map<Object, Object> allCasesFromRedis = redisRepository.getAllEntries(CASE_KEY);
    if (allCasesFromRedis.isEmpty()) {
      refresh();
    }
    ArrayList<Map<String, Object>> result = new ArrayList<>();
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    for (Map.Entry<Object, Object> entry : allCasesFromRedis.entrySet()) {
      Map<String, Object> map = new HashMap<String, Object>();
      Case caseObj = (Case) entry.getValue();

      if (!isInputEmpty(product) && !caseObj.getProductName().toLowerCase().contains(product.toLowerCase())) {
        continue;
      }
      if (!isInputEmpty(provider) && isParsable(provider)
        && Integer.parseInt(provider) != caseObj.getProvider()) {
        continue;
      }
      if (!isInputEmpty(errorCode) && isParsable(errorCode)
        && Integer.parseInt(errorCode) != caseObj.getErrorCode()) {
        continue;
      }
      if (!isInputEmpty(customerId) && isParsable(customerId)
        && Integer.parseInt(customerId) != caseObj.getCustomerId()) {
        continue;
      }
      if (!isInputEmpty(status) && !status.equalsIgnoreCase(caseObj.getStatus())) {
        continue;
      }

      if (!isInputEmpty(fromCreationDate)) {
        try {
          Date parsed = inputDateFormat.parse(fromCreationDate);
          if (parsed.compareTo(caseObj.getCreationDate()) > 0) {
            continue;
          }
        } catch (ParseException e) {
          e.printStackTrace();
        }
      }

      if (!isInputEmpty(fromLastModifiedDate)) {
        try {
          Date parsed = inputDateFormat.parse(fromLastModifiedDate);
          if (parsed.compareTo(caseObj.getLastModifiedDate()) > 0) {
            continue;
          }
        } catch (ParseException e) {
          e.printStackTrace();
        }

      }
      if (!isInputEmpty(untilCreationDate)) {
        try {
          Date parsed = inputDateFormat.parse(untilCreationDate);
          if (caseObj.getCreationDate().compareTo(parsed) > 0) {
            continue;
          }
        } catch (ParseException e) {
          e.printStackTrace();
        }

      }
      if (!isInputEmpty(untilLastModifiedDate)) {
        try {
          Date parsed = inputDateFormat.parse(untilLastModifiedDate);
          if (caseObj.getLastModifiedDate().compareTo(parsed) > 0) {
            continue;
          }
        } catch (ParseException e) {
          e.printStackTrace();
        }
      }

      map.put("productName", caseObj.getProductName());
      map.put("provider", caseObj.getProvider());
      map.put("caseId", caseObj.getCaseId());
      map.put("lastModifiedDate", dateFormat.format(caseObj.getLastModifiedDate()));
      map.put("status", caseObj.getStatus());
      map.put("customerId", caseObj.getCustomerId());
      map.put("creationDate", dateFormat.format(caseObj.getCreationDate()));
      map.put("errorCode", caseObj.getErrorCode());
      map.put("resourceName", caseObj.getResourceName());
      result.add(map);
    }
    result.sort(Comparator.comparingInt(c -> ((int) c.get("caseId"))));
    return result;

  }

  /**
   * The API call to refresh the cases.
   *
   * @return the response entity status.
   */
  @RequestMapping(value = "/refresh", method = RequestMethod.GET)
  @Scheduled(cron = "${aggregations.auto.refresh}")
  public ResponseEntity<String> refresh() {
    Date currentDate = new Date();
    List<Crm> crmList = crmConfig.getList();

    for (int i = 0; i < crmList.size(); i++) {
      Date lastExecutionCrm = getLastExecutionResource(i);
      if (lastExecutionCrm != null && currentDate.getTime() - lastExecutionCrm.getTime() < aggregationsRefreshPeriodTime) {
        logger.warn("The data is already fetched less than the time limit.");
        return new ResponseEntity<>(HttpStatus.OK);
      }
    }

    for (int i = 0; i < crmList.size(); i++) {
      refreshCrm(i, crmList.get(i).getUrl(), crmList.get(i).getName(), crmList.get(i).getRateLimit(), crmList.get(i).getPeriodTime());
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * The API call to delete all cases.
   *
   * @return the response entity status.
   */
  @RequestMapping(value = "/delete", method = RequestMethod.GET)
  public ResponseEntity<String> delete() {
    redisRepository.delete(LAST_EXECUTION_KEY);
    redisRepository.delete(CASE_KEY);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Helper function to valid if a string is parsable.
   *
   * @param input the given input from the user.
   * @return true if is parsable, false otherwise.
   */
  public static boolean isParsable(String input) {
    try {
      Integer.parseInt(input);
      return true;
    } catch (final NumberFormatException e) {
      return false;
    }
  }

  /**
   * Helper function to check it an input is empty.
   *
   * @param input the given input from the user.
   * @return true if is empty, false otherwise.
   */
  public static boolean isInputEmpty(String input) {
    if (input != null && !input.equals("")) {
      return false;
    }
    return true;
  }

  /**
   * Mapping the data to a case.
   *
   * @param crmId     the CRM ID.
   * @param crmName   the CRM Name.
   * @param caseToAdd the case that should be mapped and added.
   */
  public void mapCase(int crmId, String crmName, LinkedHashMap caseToAdd) {
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
      redisRepository.add(CASE_KEY, id, caseObj);
    } catch (ParseException e) {
      e.printStackTrace();
      logger.error("Error found on parsing the date format", e);
    }
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

    if (redisRepository.exists(LAST_EXECUTION_KEY, crmId)) {
      Date lastExecutionCrm = (Date) redisRepository.get(LAST_EXECUTION_KEY, crmId);
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
              mapCase(crmId, crmName, fetchedCase);
            }
          } else {
            ArrayList<LinkedHashMap> fetchedCases = (ArrayList) result;
            for (LinkedHashMap fetchedCase : fetchedCases) {
              mapCase(crmId, crmName, fetchedCase);
            }
          }
        }
        redisRepository.add(LAST_EXECUTION_KEY, crmId, currentDate);
      } catch (HttpStatusCodeException e) {
        e.printStackTrace();
        logger.error("Error found on calling to the API", e);
      }
    }
  }

  /**
   * Return last execution of fetching data from a CRM.
   *
   * @param crmId the crm ID.
   * @return the date of the last execution.
   */
  public Date getLastExecutionResource(int crmId) {
    if (redisRepository.exists(LAST_EXECUTION_KEY, crmId)) {
      return (Date) redisRepository.get(LAST_EXECUTION_KEY, crmId);
    }
    return null;
  }

}
