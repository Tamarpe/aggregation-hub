package com.tamar.support.model;

public class Crm {
  private String name;
  private String url;
  private Integer rateLimit;
  private Integer periodTime;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Integer getPeriodTime() {
    return periodTime;
  }

  public void setPeriodTime(Integer periodTime) {
    this.periodTime = periodTime;
  }

  public Integer getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(Integer rateLimit) {
    this.rateLimit = rateLimit;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CRM{");
    sb.append("name=").append(name);
    sb.append(", url='").append(url);
    sb.append(", rateLimit=").append(rateLimit);
    sb.append(", periodTime=").append(periodTime);
    sb.append('}');
    return sb.toString();
  }
}
