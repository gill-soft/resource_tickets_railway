package com.gillsoft.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.cache.RedisMemoryCache;
import com.gillsoft.logging.SimpleRequestResponseLoggingInterceptor;
import com.gillsoft.model.Customer;
import com.gillsoft.model.ResponseError;
import com.gillsoft.model.Seat;
import com.gillsoft.util.RestTemplateUtil;
import com.gillsoft.util.StringUtil;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class RestClient {
	
	private static Logger LOGGER = LogManager.getLogger(RestClient.class);
	
	public static final String STATIONS_CACHE_KEY = "tickets.stations";
	public static final String TRAINS_CACHE_KEY = "tickets.trains.";
	public static final String TRAIN_CACHE_KEY = "tickets.train.";
	public static final String ROUTE_CACHE_KEY = "tickets.route.";
	
	public static final String DATE_FORMAT = "dd-MM-yyyy";
	public static final String DATE_TIME_FORMAT = "dd-MM-yyyy HH:mm";
	public static final FastDateFormat dateFormat = FastDateFormat.getInstance(DATE_FORMAT);
	public static final FastDateFormat dateTimeFormat = FastDateFormat.getInstance(DATE_TIME_FORMAT);
	
	public static final int RESERVETION_STATUS = 0;
	public static final int PAIED_STATUS = 1; //TODO change
	public static final int RETURNED_STATUS = 2; //TODO change
	public static final int CANCEL_STATUS = 4;
	
	private static final String CONFIRM_CODE = "0";
	private static final String LANG_RU = "ru";
	private static final String SERVICE = "gd";
	
	private static final String STATIONS = "station.json";
	private static final String TRAINS = "search.json";
	private static final String TRAIN = "train.json";
	private static final String CAR = "car.json";
	private static final String ROUTE = "timetable/train_route.json";
	private static final String RESERVATION = "reservation.json";
	private static final String COMMIT = "commit.json";
	private static final String SHOW_BOOKING = "booking_show.json";
	private static final String CANCEL = "cancel.json";
	private static final String GET_REFUND_AMOUNT = "get_refund_amount.json";
	private static final String MAKE_REFUND = "make_refund.json";
	
//	13939702
//	13875070
//	13946362
	
	@Autowired
    @Qualifier("RedisMemoryCache")
	private CacheHandler cache;
	
	private RestTemplate template;
	
	// для запросов поиска с меньшим таймаутом
	private RestTemplate searchTemplate;
	
	public RestClient() {
		template = createNewPoolingTemplate(Config.getRequestTimeout());
		searchTemplate = createNewPoolingTemplate(Config.getSearchRequestTimeout());
	}
	
	public RestTemplate createNewPoolingTemplate(int requestTimeout) {
		RestTemplate template = new RestTemplate(new BufferingClientHttpRequestFactory(
				RestTemplateUtil.createPoolingFactory(Config.getUrl(), 300, requestTimeout, true, true)));
		template.setInterceptors(Collections.singletonList(
				new SimpleRequestResponseLoggingInterceptor()));
		return template;
	}
	
	@SuppressWarnings("unchecked")
	public List<Station> getCachedStations() throws IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, RestClient.STATIONS_CACHE_KEY);
		params.put(RedisMemoryCache.IGNORE_AGE, true);
		params.put(RedisMemoryCache.UPDATE_DELAY, Config.getCacheStationsUpdateDelay());
		params.put(RedisMemoryCache.UPDATE_TASK, new StationsUpdateTask());
		return (List<Station>) cache.read(params);
	}
	
	public List<Station> getStations() {
		Map<String, Station> stations = new HashMap<>();
		for (char i = 'а'; i <= 'ё'; i++) {
			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("key", Config.getKey());
			params.add("lang", LANG_RU);
			params.add("limit", "0");
			params.add("name", Character.toString(i) + "%");
			Response res;
			try {
				res = getResult(searchTemplate, STATIONS, params);
				if (res != null && res.getStations() != null) {
					for (Station station : res.getStations()) {
						stations.put(station.getCode(), station);
						if (station.getSubstations() != null) {
							for (Station substation : station.getSubstations()) {
								stations.put(substation.getCode(), substation);
							}
						}
					}
				}
			} catch (ResponseError e) {
				LOGGER.error("Get stations error", e);
			}
		}
		return new ArrayList<>(stations.values());
	}
	
	public Response getCachedTrains(String from, String to, Date date) throws ResponseError, IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getTrainsCacheKey(date, from, to));
		params.put(RedisMemoryCache.UPDATE_TASK, new TrainsUpdateTask(from, to, date));
		return (Response) checkCache(cache.read(params));
	}
	
	private Object checkCache(Object value) throws ResponseError {
		if (value instanceof ResponseError) {
			throw (ResponseError) value;
		} else {
			return value;
		}
	}
	
	public Response getTrains(String from, String to, Date date) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", Config.getKey());
		params.add("lang", LANG_RU);
		params.add("from", from);
		params.add("to", to);
		params.add("date", dateFormat.format(date));
		return getResult(searchTemplate, TRAINS, params);
	}
	
	public Train getCachedTrain(String sessionId, String trainNumber) throws ResponseError, IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getTrainCacheKey(sessionId, trainNumber));
		params.put(RedisMemoryCache.UPDATE_TASK, new TrainUpdateTask(sessionId, trainNumber));
		return (Train) checkCache(cache.read(params));
	}
	
	public Train getTrain(String sessionId, String trainNumber) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", Config.getKey());
		params.add("lang", LANG_RU);
		params.add("session_id", sessionId);
		params.add("train_number", trainNumber);
		return getResult(searchTemplate, TRAIN, params).getTrain();
	}
	
	public Response getTrain(String from, String to, Date date, String trainNumber) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", Config.getKey());
		params.add("lang", LANG_RU);
		params.add("from", from);
		params.add("to", to);
		params.add("date", dateFormat.format(date));
		params.add("train_number", trainNumber);
		return getResult(searchTemplate, TRAIN, params);
	}
	
	public CarInfo getCar(String sessionId, String carId) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", Config.getKey());
		params.add("lang", LANG_RU);
		params.add("session_id", sessionId);
		params.add("car_id", carId);
		return getResult(searchTemplate, CAR, params).getCar();
	}
	
	public TrainRoute getCachedRoute(String from, String to, Date date, String trainNumber) throws ResponseError, IOCacheException {
		Map<String, Object> params = new HashMap<>();
		params.put(RedisMemoryCache.OBJECT_NAME, getRouteCacheKey(date, trainNumber));
		params.put(RedisMemoryCache.UPDATE_TASK, new RouteUpdateTask(from, to, date, trainNumber));
		return (TrainRoute) checkCache(cache.read(params));
	}
	
	public TrainRoute getRoute(String from, String to, Date date, String trainNumber) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", Config.getKey());
		params.add("lang", LANG_RU);
		params.add("from", from);
		params.add("to", to);
		params.add("date", dateFormat.format(date));
		params.add("train_number", trainNumber);
		return getResult(searchTemplate, ROUTE, params).getRoute();
	}
	
	public Response reservation(String sessionId, String operationType, List<Customer> customers, List<Seat> seats) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", Config.getKey());
		params.add("lang", LANG_RU);
		params.add("session_id", sessionId);
		List<String> passengers = new ArrayList<>(customers.size());
		for (Customer customer : customers) {
			passengers.add(String.join(":", customer.getName(), customer.getSurname(), "", ""));
		}
		params.add("passengers", String.join("|", passengers));
		params.add("auth_key", Config.getAuthKey());
		params.add("no_clothes", "1");
		params.add("operation_type", operationType);
		params.add("range", String.join(",", seats.stream().map(Seat::getId).collect(Collectors.toList())));
		return getResult(template, RESERVATION, params);
	}
	
	public Train commit(String orderId, String amount, String currency) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", Config.getKey());
		params.add("lang", LANG_RU);
		params.add("сommit_auth_key", Config.getAuthKey());
		params.add("signature", getSignature(orderId, amount));
		params.add("service", SERVICE);
		params.add("order_id", orderId);
		params.add("amount", amount);
		params.add("currency", currency);
		return getResult(template, COMMIT, params).getOrder();
	}
	
	private String getSignature(String orderId, String amount) {
		return StringUtil.md5(String.join("",
				Config.getShopApiKey(), SERVICE, orderId, amount, Config.getShopSecretKey()));
	}
	
	public Train getBooking(String reservationId) throws ResponseError {
		return bookingOperation(reservationId, SHOW_BOOKING);
	}
	
	public Train cancelBooking(String reservationId) throws ResponseError {
		return bookingOperation(reservationId, CANCEL);
	}
	
	private Train bookingOperation(String reservationId, String method) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", Config.getKey());
		params.add("lang", LANG_RU);
		params.add("auth_key", Config.getAuthKey());
		params.add("reservation_id", reservationId);
		return getResult(template, method, params).getBooking();
	}
	
	public Refund getRefundAmount(String reservationId, String passengerId) throws ResponseError {
		return refundOperation(reservationId, passengerId, GET_REFUND_AMOUNT);
	}
	
	public Refund refund(String reservationId, String passengerId) throws ResponseError {
		return refundOperation(reservationId, passengerId, MAKE_REFUND);
	}
	
	private Refund refundOperation(String reservationId, String passengerId, String method) throws ResponseError {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("key", Config.getKey());
		params.add("lang", LANG_RU);
		params.add("auth_key", Config.getAuthKey());
		params.add("reservation_id", reservationId);
		params.add("passenger_id", passengerId);
		return getResult(template, method, params).getRefund();
	}
	
	private Response getResult(RestTemplate template, String method, MultiValueMap<String, String> params) throws ResponseError {
		URI uri = UriComponentsBuilder.fromUriString(Config.getUrl() + method).queryParams(params).build().toUri();
		ResponseEntity<ResponseContainer> response = null;
		try {
			response = template.exchange(new RequestEntity<>(HttpMethod.GET, uri), ResponseContainer.class);
		} catch (Exception e) {
			throw new ResponseError(e.getMessage());
		}
		// проверяем ответ на ошибку
		if (!Objects.equals(response.getBody().getResponse().getResult().getCode(), CONFIRM_CODE)) {
			throw new ResponseError(response.getBody().getResponse().getResult().getDescription());
		}
		return response.getBody().getResponse();
	}
	
	public CacheHandler getCache() {
		return cache;
	}
	
	public static RestClientException createUnavailableMethod() {
		return new RestClientException("Method is unavailable");
	}
	
	public static String getTrainsCacheKey(Date date, String from, String to) {
		return TRAINS_CACHE_KEY + String.join(";",
				String.valueOf(DateUtils.truncate(date, Calendar.DATE).getTime()), from, to);
	}
	
	public static String getTrainCacheKey(String sessionId, String trainNumber) {
		return TRAIN_CACHE_KEY + String.join(";", sessionId, trainNumber);
	}
	
	public static String getRouteCacheKey(Date date, String trainNumber) {
		return ROUTE_CACHE_KEY + String.join(";",
				String.valueOf(DateUtils.truncate(date, Calendar.DATE).getTime()), trainNumber);
	}

}
