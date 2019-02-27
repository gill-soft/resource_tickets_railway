package com.gillsoft.client;

import java.io.Serializable;

public class ResponseContainer implements Serializable {

	private static final long serialVersionUID = -5736991494171433874L;
	
	private Response response;

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

}
