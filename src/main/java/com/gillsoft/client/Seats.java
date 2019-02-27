package com.gillsoft.client;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Seats implements Serializable {

	private static final long serialVersionUID = 407049253856709820L;
	
	private int all;
	private int lower;
	private int upper;
	
	@JsonProperty("side_lower")
	private int sideLower;
	
	@JsonProperty("side_upper")
	private int sideUpper;

	public int getAll() {
		return all;
	}

	public void setAll(int all) {
		this.all = all;
	}

	public int getLower() {
		return lower;
	}

	public void setLower(int lower) {
		this.lower = lower;
	}

	public int getUpper() {
		return upper;
	}

	public void setUpper(int upper) {
		this.upper = upper;
	}

	public int getSideLower() {
		return sideLower;
	}

	public void setSideLower(int sideLower) {
		this.sideLower = sideLower;
	}

	public int getSideUpper() {
		return sideUpper;
	}

	public void setSideUpper(int sideUpper) {
		this.sideUpper = sideUpper;
	}

}
