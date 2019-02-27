package com.gillsoft.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CarClass implements Serializable {

	private static final long serialVersionUID = -7644250446202033489L;

	private String name;
	private String subclass;
	private String type;
	private String services;
	private String nutrition;
	
	@JsonProperty("not_firm_car")
	private String notFirmCar;
	
	private BigDecimal cost;
	private Map<String, BigDecimal> exchanges;
	private List<Car> cars;
	
	private Seats seats;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSubclass() {
		return subclass;
	}

	public void setSubclass(String subclass) {
		this.subclass = subclass;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getServices() {
		return services;
	}

	public void setServices(String services) {
		this.services = services;
	}

	public String getNutrition() {
		return nutrition;
	}

	public void setNutrition(String nutrition) {
		this.nutrition = nutrition;
	}

	public String getNotFirmCar() {
		return notFirmCar;
	}

	public void setNotFirmCar(String notFirmCar) {
		this.notFirmCar = notFirmCar;
	}

	public Seats getSeats() {
		return seats;
	}

	public void setSeats(Seats seats) {
		this.seats = seats;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

	public Map<String, BigDecimal> getExchanges() {
		return exchanges;
	}

	public void setExchanges(Map<String, BigDecimal> exchanges) {
		this.exchanges = exchanges;
	}

	public List<Car> getCars() {
		return cars;
	}

	public void setCars(List<Car> cars) {
		this.cars = cars;
	}

}
