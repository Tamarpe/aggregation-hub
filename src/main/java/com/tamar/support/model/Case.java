package com.tamar.support.model;

import java.io.Serializable;
import java.util.Date;

public class Case implements Serializable {
	private static final long serialVersionUID = 1L;

	private String id;

	private int caseId;
	private int customerId;
	private int provider;
	private int errorCode;
	private String status;
	private Date creationDate;
	private Date lastModifiedDate;
	private String productName;
	private String resourceName;

	public Case(String id, int caseId, int customerId, int provider, int errorCode, String status, Date creationDate, Date lastModifiedDate, String productName, String resourceName) {
		this.id = id;
		this.caseId = caseId;
		this.customerId = customerId;
		this.provider = provider;
		this.errorCode = errorCode;
		this.status = status;
		this.creationDate = creationDate;
		this.lastModifiedDate = lastModifiedDate;
		this.productName = productName;
		this.resourceName = resourceName;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getCaseId() {
		return caseId;
	}
	public void setCaseId(int caseId) {
		this.caseId = caseId;
	}
	public int getCustomerId() {
		return customerId;
	}
	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}
	public int getProvider() {
		return provider;
	}
	public void setProvider(int provider) {
		this.provider = provider;
	}
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Date getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}
	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getResourceName() {
		return resourceName;
	}
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Case{");
		sb.append("id=").append(id);
		sb.append(", name='").append(productName).append('\'');
		sb.append(", creationDate=").append(creationDate);
		sb.append(", status=").append(status);
		sb.append('}');
		return sb.toString();
	}
}


