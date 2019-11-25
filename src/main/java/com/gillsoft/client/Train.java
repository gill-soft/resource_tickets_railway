package com.gillsoft.client;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Train implements Serializable {

	private static final long serialVersionUID = 9189918545967775552L;
	
	private boolean added;
	
	@JsonProperty("reservation_id")
	private String reservationId;
	
	@JsonProperty("booking_id")
	private String bookingId;
	
	private BigDecimal cost;
	private String currency;
	private int status;
	private boolean cancelled;
	private boolean paid;
	
	@JsonProperty("train_number")
	private String trainNumber;
	
	@JsonProperty("car_number")
	private String carNumber;
	
	@JsonProperty("car_type")
	private String carType;
	
	private String number;
	private String name;
	
	@JsonProperty("train_departure_code")
    private String departureCode;
	
	@JsonProperty("train_arrival_code")
    private String arrivalCode;
	
	@JsonProperty("train_departure_name")
    private String departureName;
	
	@JsonProperty("train_arrival_name")
    private String arrivalName;
	
	@JsonProperty("passenger_departure_code")
    private String passengerDepartureCode;
	
	@JsonProperty("passenger_arrival_code")
    private String passengerArrivalCode;
	
	@JsonProperty("passenger_departure_name")
    private String passengerDepartureName;
	
	@JsonProperty("passenger_arrival_name")
    private String passengerArrivalName;
	
	@JsonProperty("travel_time")
    private int travelTime;
	
	@JsonProperty("departure_time")
    private String departureTime;
	
	@JsonProperty("departure_date")
    private String departureDate;
	
	@JsonProperty("arrival_time")
    private String arrivalTime;
	
	@JsonProperty("arrival_date")
    private String arrivalDate;
	
	@JsonProperty("arrival_timezone")
    private String arrivalTimezone;
	
	@JsonProperty("departure_timezone")
    private String departureTimezone;
	
	@JsonProperty("train_class")
    private String clas;
	
	@JsonProperty("train_speed")
    private String speed;
	
	@JsonProperty("train_firm")
    private String firm;
	
	@JsonProperty("provider_name")
    private String providerName;
	
	private List<CarClass> classes;
	
    private List<Exchange> exchanges;
    
    private List<Document> documents;

	public boolean isAdded() {
		return added;
	}

	public void setAdded(boolean added) {
		this.added = added;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDepartureCode() {
		return departureCode;
	}

	public void setDepartureCode(String departureCode) {
		this.departureCode = departureCode;
	}

	public String getArrivalCode() {
		return arrivalCode;
	}

	public void setArrivalCode(String arrivalCode) {
		this.arrivalCode = arrivalCode;
	}

	public String getDepartureName() {
		return departureName;
	}

	public void setDepartureName(String departureName) {
		this.departureName = departureName;
	}

	public String getArrivalName() {
		return arrivalName;
	}

	public void setArrivalName(String arrivalName) {
		this.arrivalName = arrivalName;
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

	public String getPassengerDepartureName() {
		return passengerDepartureName;
	}

	public void setPassengerDepartureName(String passengerDepartureName) {
		this.passengerDepartureName = passengerDepartureName;
	}

	public String getPassengerArrivalName() {
		return passengerArrivalName;
	}

	public void setPassengerArrivalName(String passengerArrivalName) {
		this.passengerArrivalName = passengerArrivalName;
	}

	public int getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(int travelTime) {
		this.travelTime = travelTime;
	}

	public String getDepartureTime() {
		return departureTime;
	}

	public void setDepartureTime(String departureTime) {
		this.departureTime = departureTime;
	}

	public String getDepartureDate() {
		return departureDate;
	}

	public void setDepartureDate(String departureDate) {
		this.departureDate = departureDate;
	}
	
	public Date getDeparture() {
		try {
			return RestClient.dateTimeFormat.parse(departureDate + " " + departureTime);
		} catch (ParseException e) {
			return null;
		}
	}

	public String getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(String arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	
	public Date getArrival() {
		try {
			return RestClient.dateTimeFormat.parse(arrivalDate + " " + arrivalTime);
		} catch (ParseException e) {
			return null;
		}
	}

	public String getArrivalDate() {
		return arrivalDate;
	}

	public void setArrivalDate(String arrivalDate) {
		this.arrivalDate = arrivalDate;
	}

	public String getArrivalTimezone() {
		return arrivalTimezone;
	}

	public void setArrivalTimezone(String arrivalTimezone) {
		this.arrivalTimezone = arrivalTimezone;
	}

	public String getDepartureTimezone() {
		return departureTimezone;
	}

	public void setDepartureTimezone(String departureTimezone) {
		this.departureTimezone = departureTimezone;
	}

	public String getClas() {
		return clas;
	}

	public void setClas(String clas) {
		this.clas = clas;
	}

	public String getSpeed() {
		return speed;
	}

	public void setSpeed(String speed) {
		this.speed = speed;
	}

	public String getFirm() {
		return firm;
	}

	public void setFirm(String firm) {
		this.firm = firm;
	}

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public List<CarClass> getClasses() {
		return classes;
	}

	public void setClasses(List<CarClass> classes) {
		this.classes = classes;
	}

	public String getReservationId() {
		return reservationId;
	}

	public void setReservationId(String reservationId) {
		this.reservationId = reservationId;
	}

	public String getBookingId() {
		return bookingId;
	}

	public void setBookingId(String bookingId) {
		this.bookingId = bookingId;
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

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public boolean isPaid() {
		return paid;
	}

	public void setPaid(boolean paid) {
		this.paid = paid;
	}

	public String getTrainNumber() {
		return trainNumber;
	}

	public void setTrainNumber(String trainNumber) {
		this.trainNumber = trainNumber;
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

	public List<Exchange> getExchanges() {
		return exchanges;
	}

	public void setExchanges(List<Exchange> exchanges) {
		this.exchanges = exchanges;
	}

	public List<Document> getDocuments() {
		return documents;
	}

	public void setDocuments(List<Document> documents) {
		this.documents = documents;
	}

}
