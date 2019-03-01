package com.gillsoft.client;

import java.util.List;

import com.gillsoft.model.AbstractJsonModel;

public class OrderIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 4318484251645220464L;

	private List<ServiceIdModel> ids;

	public OrderIdModel() {

	}

	public OrderIdModel(List<ServiceIdModel> ids) {
		this.ids = ids;
	}

	public List<ServiceIdModel> getIds() {
		return ids;
	}

	public void setIds(List<ServiceIdModel> ids) {
		this.ids = ids;
	}

	@Override
	public OrderIdModel create(String json) {
		return (OrderIdModel) super.create(json);
	}

}
