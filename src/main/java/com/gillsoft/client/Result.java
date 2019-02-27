package com.gillsoft.client;

import java.io.Serializable;

public class Result implements Serializable {

	private static final long serialVersionUID = 6458784880423170079L;
	
	private String code;
	private String description;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
}
