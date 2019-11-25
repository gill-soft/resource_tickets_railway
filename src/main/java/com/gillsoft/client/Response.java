package com.gillsoft.client;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Response implements Serializable {

	private static final long serialVersionUID = 4468159924545386067L;
	
	private Result result;
	
	private Session session;
	
	private List<Station> stations;
	
	private List<Train> trains;
	
	private Train train;
	
	private CarInfo car;
	
	@JsonProperty("train_route")
	private TrainRoute route;
	
	private Reservation reservation;
	
	private Card card;
	
	private Train booking;
	
	private Train order;
	
	private Refund refund;
	
	private Map<String, String> pdf;

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public List<Station> getStations() {
		return stations;
	}

	public void setStations(List<Station> stations) {
		this.stations = stations;
	}

	public List<Train> getTrains() {
		return trains;
	}

	public void setTrains(List<Train> trains) {
		this.trains = trains;
	}

	public Train getTrain() {
		return train;
	}

	public void setTrain(Train train) {
		this.train = train;
	}

	public CarInfo getCar() {
		return car;
	}

	public void setCar(CarInfo car) {
		this.car = car;
	}

	public TrainRoute getRoute() {
		return route;
	}

	public void setRoute(TrainRoute route) {
		this.route = route;
	}

	public Reservation getReservation() {
		return reservation;
	}

	public void setReservation(Reservation reservation) {
		this.reservation = reservation;
	}

	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}

	public Train getBooking() {
		return booking;
	}

	public void setBooking(Train booking) {
		this.booking = booking;
	}

	public Train getOrder() {
		return order;
	}

	public void setOrder(Train order) {
		this.order = order;
	}

	public Refund getRefund() {
		return refund;
	}

	public void setRefund(Refund refund) {
		this.refund = refund;
	}

	public Map<String, String> getPdf() {
		return pdf;
	}

	public void setPdf(Map<String, String> pdf) {
		this.pdf = pdf;
	}

}
