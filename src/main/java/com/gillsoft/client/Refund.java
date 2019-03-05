package com.gillsoft.client;

import java.io.Serializable;
import java.math.BigDecimal;

import com.gillsoft.model.Currency;

public class Refund implements Serializable {

	private static final long serialVersionUID = -1173138241821637851L;

	private BigDecimal amount;
	private Currency currency;
	private boolean success;

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

}
