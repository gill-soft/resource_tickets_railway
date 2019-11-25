package com.gillsoft.client;

import java.io.Serializable;
import java.math.BigDecimal;

public class Exchange implements Serializable {

	private static final long serialVersionUID = -7875809499848234069L;
	
	private String currency;
	private BigDecimal cost;

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

}
