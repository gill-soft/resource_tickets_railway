package com.gillsoft.client;

import com.gillsoft.model.AbstractJsonModel;

public class ServiceIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 4318484251645220464L;
	
	private String id;
	
	private String passId;
	
	public ServiceIdModel() {
		
	}

	public ServiceIdModel(String id, String passId) {
		this.id = id;
		this.passId = passId;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPassId() {
		return passId;
	}

	public void setPassId(String passId) {
		this.passId = passId;
	}

	@Override
	public ServiceIdModel create(String json) {
		return (ServiceIdModel) super.create(json);
	}

}
