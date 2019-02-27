package com.gillsoft.client;

import java.util.Date;

import com.gillsoft.cache.AbstractUpdateTask;
import com.gillsoft.model.ResponseError;
import com.gillsoft.util.ContextProvider;

public class RouteUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = 5964528266364148528L;
	
	private String from;
	private String to;
	private Date date;
	private String trainNumber;
	
	public RouteUpdateTask(String from, String to, Date date, String trainNumber) {
		this.from = from;
		this.to = to;
		this.date = date;
		this.trainNumber = trainNumber;
	}

	@Override
	public void run() {
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			TrainRoute route = client.getRoute(from, to, date, trainNumber);
			writeObject(client.getCache(), RestClient.getRouteCacheKey(date, trainNumber), route,
					getTimeToLive(), Config.getCacheTripUpdateDelay());
		} catch (ResponseError e) {
			// ошибку тоже кладем в кэш
			writeObject(client.getCache(), RestClient.getRouteCacheKey(date, trainNumber), e,
					Config.getCacheErrorTimeToLive(), Config.getCacheErrorUpdateDelay());
		}
	}
	
	// время жизни до момента отправления
	private long getTimeToLive() {
		if (Config.getCacheTripTimeToLive() != 0) {
			return Config.getCacheTripTimeToLive();
		}
		return date.getTime() - System.currentTimeMillis();
	}

}
