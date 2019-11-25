package com.gillsoft.client;

import java.util.List;

import com.gillsoft.model.AbstractJsonModel;

public class OrderIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 4318484251645220464L;
	
	private List<ServiceIdModel> orders;

	public OrderIdModel() {

	}

	public List<ServiceIdModel> getOrders() {
		return orders;
	}

	public void setOrders(List<ServiceIdModel> orders) {
		this.orders = orders;
	}

	@Override
	public OrderIdModel create(String json) {
		return (OrderIdModel) super.create(json);
	}

}
