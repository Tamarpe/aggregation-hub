package com.tamar.support.config;

import com.tamar.support.model.Crm;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "crm")
public class CrmConfig {
  /** The CRMs list. */
  private List<Crm> list;

  /**
   * Get a CRMs list from the properties.
   *
   * @return CRMs list
   */
  public List<Crm> getList() {
    return list;
  }

  /**
   * A setter for the CRMs list.
   *
   * @param list CRMs list
   */
  public void setList(List<Crm> list) {
    this.list = list;
  }

}
