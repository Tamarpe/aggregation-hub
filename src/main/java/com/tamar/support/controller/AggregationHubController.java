package com.tamar.support.controller;

import com.tamar.support.model.Case;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.tamar.support.repository.RedisRepository;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class AggregationHubController {

  /**
   * Redis repository.
   */
  @Autowired
  private RedisRepository redisRepository;

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
    Map<Object, Object> allCasesFromRedis = redisRepository.findAllCases();
    ArrayList<Map<String, Object>> result = new ArrayList<>();
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    for (Map.Entry<Object, Object> entry : allCasesFromRedis.entrySet()) {
      Map<String, Object> map = new HashMap<String, Object>();
      Case caseObj = (Case) entry.getValue();

      if (product != null && !product.equals("") && !caseObj.getProductName().toLowerCase().contains(product.toLowerCase())) {
        continue;
      }
      if (provider != null && !provider.equals("") && isParsable(provider)
        && Integer.parseInt(provider) != caseObj.getProvider()) {
        continue;
      }
      if (errorCode != null && !errorCode.equals("") && isParsable(errorCode)
        && Integer.parseInt(errorCode) != caseObj.getErrorCode()) {
        continue;
      }
      if (customerId != null && !customerId.equals("") && isParsable(customerId)
        && Integer.parseInt(customerId) != caseObj.getCustomerId()) {
        continue;
      }
      if (status != null && !status.equals("") && !status.equalsIgnoreCase(caseObj.getStatus())) {
        continue;
      }

      if (fromCreationDate != null && !fromCreationDate.equals("")) {
        try {
          Date parsed = inputDateFormat.parse(fromCreationDate);
          if (parsed.compareTo(caseObj.getCreationDate()) > 0) {
            continue;
          }
        } catch (ParseException e) {
          e.printStackTrace();
        }
      }

      if (fromLastModifiedDate != null && !fromLastModifiedDate.equals("")) {
        try {
          Date parsed = inputDateFormat.parse(fromLastModifiedDate);
          if (parsed.compareTo(caseObj.getLastModifiedDate()) > 0) {
            continue;
          }
        } catch (ParseException e) {
          e.printStackTrace();
        }

      }
      if (untilCreationDate != null && !untilCreationDate.equals("")) {
        try {
          Date parsed = inputDateFormat.parse(untilCreationDate);
          if (caseObj.getCreationDate().compareTo(parsed) > 0) {
            continue;
          }
        } catch (ParseException e) {
          e.printStackTrace();
        }

      }

      if (untilLastModifiedDate != null && !untilLastModifiedDate.equals("")) {
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
  public ResponseEntity<String> refresh() {
    redisRepository.refresh();
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * The API call delete all cases.
   *
   * @return the response entity status.
   */
  @RequestMapping(value = "/delete", method = RequestMethod.GET)
  public ResponseEntity<String> delete() {
    redisRepository.delete();
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
}
