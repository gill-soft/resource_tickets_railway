package com.gillsoft.client;

import java.util.Date;
import java.util.List;

import com.gillsoft.cache.AbstractUpdateTask;
import com.gillsoft.concurrent.SerializablePoolType;
import com.gillsoft.model.ResponseError;
import com.gillsoft.util.ContextProvider;

public class TrainsUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = -5103341288539272796L;
	private static final long THIRTY_SIX_HOURS = 129600000l;
	private static final String POOL_NAME = "RAILWAY_TRAINS_POOL";
	private static final int POOL_SIZE = 100;
	private SerializablePoolType poolType = new SerializablePoolType(POOL_SIZE, POOL_NAME);
	
	private String from;
	private String to;
	private Date date;
	
	public TrainsUpdateTask(String from, String to, Date date) {
		this.from = from;
		this.to = to;
		this.date = date;
	}

	@Override
	public void run() {
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			Response response = client.getTrains(from, to, date);
			writeObject(client.getCache(), RestClient.getTrainsCacheKey(date, from, to), response,
					getTimeToLive(response.getTrains()), getHalfPartOfDepartureTime(date), false, true, poolType);
		} catch (ResponseError e) {
			// ошибку тоже кладем в кэш
			writeObject(client.getCache(), RestClient.getTrainsCacheKey(date, from, to), e,
					getHalfPartOfDepartureTime(date), 0, false, true, poolType);
		}
	}
	
	// если до даты отправления больше полторы суток, то время обновления
	// устанавливаем меньше в два раза, чем время от текущего до желаемого
	public static long getHalfPartOfDepartureTime(Date departure) {
		long updateDelay = departure.getTime() - System.currentTimeMillis();
		if (updateDelay > THIRTY_SIX_HOURS) {
			updateDelay = updateDelay / 2;
		}
		return updateDelay;
	}
	
	// время жизни до момента самого позднего отправления
	private long getTimeToLive(List<Train> trains) {
		if (Config.getCacheTripTimeToLive() != 0) {
			return Config.getCacheTripTimeToLive();
		}
		long max = 0;
		for (Train train : trains) {
			Date date = train.getDeparture();
			if (date != null && date.getTime() > max) {
				max = date.getTime();
			}
		}
		if (max == 0
				|| max < System.currentTimeMillis()) {
			return Config.getCacheErrorTimeToLive();
		}
		return max - System.currentTimeMillis();
	}

}
