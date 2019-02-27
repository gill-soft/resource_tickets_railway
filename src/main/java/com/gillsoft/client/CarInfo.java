package com.gillsoft.client;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CarInfo implements Serializable {

	private static final long serialVersionUID = 1780806423935165549L;
	
	private String subclass;
	private String schema;
	
	@JsonProperty("class")
    private CarClass clas;
	
	private String number;
	
	@JsonProperty("train_model")
	private String trainModel;
	
	private String services;
	
	@JsonProperty("train_firm")
	private String trainFirm;
	
	@JsonProperty("seats_count")
    private int seatsCount;

	private Map<String, Map<String, Integer>> seats;

	public String getSubclass() {
		return subclass;
	}

	public void setSubclass(String subclass) {
		this.subclass = subclass;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public CarClass getClas() {
		return clas;
	}

	public void setClas(CarClass clas) {
		this.clas = clas;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getTrainModel() {
		return trainModel;
	}

	public void setTrainModel(String trainModel) {
		this.trainModel = trainModel;
	}

	public String getServices() {
		return services;
	}

	public void setServices(String services) {
		this.services = services;
	}

	public String getTrainFirm() {
		return trainFirm;
	}

	public void setTrainFirm(String trainFirm) {
		this.trainFirm = trainFirm;
	}

	public int getSeatsCount() {
		return seatsCount;
	}

	public void setSeatsCount(int seatsCount) {
		this.seatsCount = seatsCount;
	}

	public Map<String, Map<String, Integer>> getSeats() {
		return seats;
	}

	public void setSeats(Map<String, Map<String, Integer>> seats) {
		this.seats = seats;
	}
	
}
