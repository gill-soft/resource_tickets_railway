package com.gillsoft.client;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Car implements Serializable {

	private static final long serialVersionUID = -3733851663172668222L;
	
	private String id;
	
	private String number;
	
	@JsonProperty("operation_types")
	private String operationTypes;
	
    private Seats seats;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getOperationTypes() {
		return operationTypes;
	}

	public void setOperationTypes(String operationTypes) {
		this.operationTypes = operationTypes;
	}

	public Seats getSeats() {
		return seats;
	}

	public void setSeats(Seats seats) {
		this.seats = seats;
	}

}
