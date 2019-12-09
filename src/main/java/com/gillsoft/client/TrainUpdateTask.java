package com.gillsoft.client;

import java.util.Date;

import com.gillsoft.cache.AbstractUpdateTask;
import com.gillsoft.concurrent.SerializablePoolType;
import com.gillsoft.model.ResponseError;
import com.gillsoft.util.ContextProvider;

public class TrainUpdateTask extends AbstractUpdateTask {

	private static final long serialVersionUID = -3353809778569437497L;
	
	private static final String POOL_NAME = "RAILWAY_TRAIN_POOL";
	private static final int POOL_SIZE = 200;
	private static final SerializablePoolType poolType = new SerializablePoolType(POOL_SIZE, POOL_NAME);
	
	private String from;
	private String to;
	private Date date;
	private String trainNumber;

	public TrainUpdateTask(String from, String to, Date date, String trainNumber) {
		this.from = from;
		this.to = to;
		this.date = date;
		this.trainNumber = trainNumber;
	}
	
	@Override
	public void run() {
		RestClient client = ContextProvider.getBean(RestClient.class);
		try {
			Train train = client.getTrain(from, to, date, trainNumber).getTrain();
			writeObject(client.getCache(), RestClient.getTrainCacheKey(date, from, to, trainNumber), train,
					getTimeToLive(train), TrainsUpdateTask.getHalfPartOfDepartureTime(date), false, false, poolType);
		} catch (ResponseError e) {
			// ошибку тоже кладем в кэш
			writeObject(client.getCache(), RestClient.getTrainCacheKey(date, from, to, trainNumber), e,
					Config.getCacheErrorTimeToLive(), 0, false, true, poolType);
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
