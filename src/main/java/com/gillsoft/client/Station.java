package com.gillsoft.client;

import java.io.Serializable;
import java.util.List;

public class Station implements Serializable {

	private static final long serialVersionUID = 4558167384202264639L;

	private String name;
	private String code;
	private String railroad;
	private int popularity;
	private boolean internal;
	private List<Station> substations;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getRailroad() {
		return railroad;
	}

	public void setRailroad(String railroad) {
		this.railroad = railroad;
	}

	public int getPopularity() {
		return popularity;
	}

	public void setPopularity(int popularity) {
		this.popularity = popularity;
	}

	public boolean isInternal() {
		return internal;
	}

	public void setInternal(boolean internal) {
		this.internal = internal;
	}

	public List<Station> getSubstations() {
		return substations;
	}

	public void setSubstations(List<Station> substations) {
		this.substations = substations;
	}

}
