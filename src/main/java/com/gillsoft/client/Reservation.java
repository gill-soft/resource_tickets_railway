package com.gillsoft.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Reservation implements Serializable {

	private static final long serialVersionUID = 7901030726391116884L;
	
    private String id;
    
    @JsonProperty("train_number")
    private String trainNumber;
    
    private String seats;
    
    @JsonProperty("car_number")
    private String carNumber;
    
    @JsonProperty("car_type")
    private String carType;
    
    private BigDecimal cost;
    private String currency;
    
    @JsonProperty("expiration_time")
    @JsonFormat(shape = Shape.STRING, pattern = "dd.MM.yyyy HH:mm:ss")
    private Date expirationTime;
    
    @JsonFormat(shape = Shape.STRING, pattern = "dd.MM.yyyy HH:mm:ss")
    private Date departure;
    
    @JsonFormat(shape = Shape.STRING, pattern = "dd.MM.yyyy HH:mm:ss")
    private Date arrival;
    
    @JsonProperty("operation_type")
    private String operationType;
    
    @JsonProperty("buyout_time")
    @JsonFormat(shape = Shape.STRING, pattern = "dd.MM.yyyy HH:mm:ss")
    private Date buyoutTime;
    
    @JsonProperty("passenger_departure_code")
    private String passengerDepartureCode;
	
	@JsonProperty("passenger_arrival_code")
    private String passengerArrivalCode;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTrainNumber() {
		return trainNumber;
	}

	public void setTrainNumber(String trainNumber) {
		this.trainNumber = trainNumber;
	}

	public String getSeats() {
		return seats;
	}

	public void setSeats(String seats) {
		this.seats = seats;
	}

	public String getCarNumber() {
		return carNumber;
	}

	public void setCarNumber(String carNumber) {
		this.carNumber = carNumber;
	}

	public String getCarType() {
		return carType;
	}

	public void setCarType(String carType) {
		this.carType = carType;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Date getExpirationTime() {
		return expirationTime;
	}

	public void setExpirationTime(Date expirationTime) {
		this.expirationTime = expirationTime;
	}

	public Date getDeparture() {
		return departure;
	}

	public void setDeparture(Date departure) {
		this.departure = departure;
	}

	public Date getArrival() {
		return arrival;
	}

	public void setArrival(Date arrival) {
		this.arrival = arrival;
	}

	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public Date getBuyoutTime() {
		return buyoutTime;
	}

	public void setBuyoutTime(Date buyoutTime) {
		this.buyoutTime = buyoutTime;
	}

	public String getPassengerDepartureCode() {
		return passengerDepartureCode;
	}

	public void setPassengerDepartureCode(String passengerDepartureCode) {
		this.passengerDepartureCode = passengerDepartureCode;
	}

	public String getPassengerArrivalCode() {
		return passengerArrivalCode;
	}

	public void setPassengerArrivalCode(String passengerArrivalCode) {
		this.passengerArrivalCode = passengerArrivalCode;
	}

}
