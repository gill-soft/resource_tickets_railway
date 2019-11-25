package com.gillsoft.client;

import java.math.BigDecimal;

import com.gillsoft.model.AbstractJsonModel;
import com.gillsoft.model.Currency;

public class ServiceIdModel extends AbstractJsonModel {

	private static final long serialVersionUID = 4318484251645220464L;
	
	private String id;
	
	private String passId;
	
	private BigDecimal cost;
	
	private Currency currency;
	
	public ServiceIdModel() {
		
	}

	public ServiceIdModel(String id, String passId, BigDecimal cost, Currency currency) {
		super();
		this.id = id;
		this.passId = passId;
		this.cost = cost;
		this.currency = currency;
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

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	@Override
	public ServiceIdModel create(String json) {
		return (ServiceIdModel) super.create(json);
	}

}
