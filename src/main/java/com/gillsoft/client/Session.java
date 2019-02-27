package com.gillsoft.client;

import java.io.Serializable;

public class Session implements Serializable {
	
	private static final long serialVersionUID = -2903294989259254620L;
	
	private String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
