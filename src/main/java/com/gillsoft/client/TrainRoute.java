package com.gillsoft.client;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrainRoute implements Serializable {

	private static final long serialVersionUID = -8420403218346356020L;
	
	private List<RouteStation> stations;
	
	private String name;
	private String number;
	
	@JsonProperty("first_station")
	private String firstStation;
	
	@JsonProperty("last_station")
	private String lastStation;

	public List<RouteStation> getStations() {
		return stations;
	}

	public void setStations(List<RouteStation> stations) {
		this.stations = stations;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getFirstStation() {
		return firstStation;
	}

	public void setFirstStation(String firstStation) {
		this.firstStation = firstStation;
	}

	public String getLastStation() {
		return lastStation;
	}

	public void setLastStation(String lastStation) {
		this.lastStation = lastStation;
	}

}
