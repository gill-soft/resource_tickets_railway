package com.gillsoft.client;

import com.gillsoft.cache.AbstractUpdateTask;
import com.gillsoft.model.ResponseError;
import com.gillsoft.util.ContextProvider;

public class TrainUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = -3353809778569437497L;
	
	private String sessionId;
	private String trainNumber;
	
	public TrainUpdateTask(String sessionId, String trainNumber) {
		this.sessionId = sessionId;
		this.trainNumber = trainNumber;
	}

	@Override
	public void run() {
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			Train train = client.getTrain(sessionId, trainNumber);
			writeObject(client.getCache(), RestClient.getTrainCacheKey(sessionId, trainNumber), train,
					getTimeToLive(train), Config.getCacheTripUpdateDelay());
		} catch (ResponseError e) {
			// ошибку тоже кладем в кэш
			writeObject(client.getCache(), RestClient.getTrainCacheKey(sessionId, trainNumber), e,
					Config.getCacheErrorTimeToLive(), Config.getCacheErrorUpdateDelay());
		}
	}
	
	// время жизни до момента отправления
	private long getTimeToLive(Train train) {
		if (Config.getCacheTripTimeToLive() != 0) {
			return Config.getCacheTripTimeToLive();
		}
		if (train.getDeparture() != null) {
			return train.getDeparture().getTime() - System.currentTimeMillis();
		} else {
			return Config.getCacheTripTimeToLive();
		}
	}

}
