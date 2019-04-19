package com.gillsoft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.gillsoft.abstract_rest_service.SimpleAbstractTripSearchService;
import com.gillsoft.cache.CacheHandler;
import com.gillsoft.cache.IOCacheException;
import com.gillsoft.client.Car;
import com.gillsoft.client.CarClass;
import com.gillsoft.client.CarInfo;
import com.gillsoft.client.Response;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.RouteStation;
import com.gillsoft.client.Train;
import com.gillsoft.client.TrainRoute;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.model.Carriage;
import com.gillsoft.model.CarriageClass;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Document;
import com.gillsoft.model.Lang;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Organisation;
import com.gillsoft.model.Price;
import com.gillsoft.model.RequiredField;
import com.gillsoft.model.ResponseError;
import com.gillsoft.model.RestError;
import com.gillsoft.model.ReturnCondition;
import com.gillsoft.model.Route;
import com.gillsoft.model.RoutePoint;
import com.gillsoft.model.Seat;
import com.gillsoft.model.SeatStatus;
import com.gillsoft.model.SeatType;
import com.gillsoft.model.SeatsScheme;
import com.gillsoft.model.Segment;
import com.gillsoft.model.SimpleTripSearchPackage;
import com.gillsoft.model.Tariff;
import com.gillsoft.model.Trip;
import com.gillsoft.model.TripContainer;
import com.gillsoft.model.Vehicle;
import com.gillsoft.model.request.TripSearchRequest;
import com.gillsoft.model.response.TripSearchResponse;
import com.gillsoft.util.StringUtil;

@RestController
public class SearchServiceController extends SimpleAbstractTripSearchService<SimpleTripSearchPackage<Map<String, Train>>> {
	
	@Autowired
	private RestClient client;
	
	@Autowired
	@Qualifier("MemoryCacheHandler")
	private CacheHandler cache;
	
	@Autowired
	private SeatsSchemeController schemaController;

	@Override
	public TripSearchResponse initSearchResponse(TripSearchRequest request) {
		return simpleInitSearchResponse(cache, request);
	}
	
	@Override
	public void addInitSearchCallables(List<Callable<SimpleTripSearchPackage<Map<String, Train>>>> callables,
			TripSearchRequest request) {
		callables.add(() -> {
			SimpleTripSearchPackage<Map<String, Train>> searchPackage = new SimpleTripSearchPackage<>();
			searchPackage.setSearchResult(new HashMap<>());
			searchPackage.setRequest(request);
			searchTrips(searchPackage);
			return searchPackage;
		});
	}
	
	private void searchTrips(SimpleTripSearchPackage<Map<String, Train>> searchPackage) {
		searchPackage.setInProgress(false);
		try {
			TripSearchRequest request = searchPackage.getRequest();
			Response response = client.getCachedTrains(request.getLocalityPairs().get(0)[0], request.getLocalityPairs().get(0)[1],
					request.getDates().get(0));
			List<Train> trains = response.getTrains();
			for (Train train : trains) {
				try {
					// получаем каждый поезд по отдельности со стоимостью и вагонами
					if (!searchPackage.getSearchResult().containsKey(train.getNumber())) {
						
						// запускаем формаирование маршрута
						try {
							client.getCachedRoute(train.getDepartureCode(), train.getArrivalCode(), train.getDeparture(), train.getNumber());
						} catch (Exception e) {
						}
						Train details = client.getCachedTrain(response.getSession().getId(), train.getNumber());
						searchPackage.getSearchResult().put(details.getNumber(), details);
					}
				} catch (IOCacheException e) {
					searchPackage.setInProgress(true);
				} catch (ResponseError e) {
				}
			}
		} catch (IOCacheException e) {
			searchPackage.setInProgress(true);
		} catch (ResponseError e) {
			searchPackage.setException(e);
		}
	}

	@Override
	public TripSearchResponse getSearchResultResponse(String searchId) {
		return simpleGetSearchResponse(cache, searchId);
	}
	
	@Override
	public void addNextGetSearchCallablesAndResult(List<Callable<SimpleTripSearchPackage<Map<String, Train>>>> callables,
			Map<String, Vehicle> vehicles, Map<String, Locality> localities, Map<String, Organisation> organisations,
			Map<String, Segment> segments, List<TripContainer> containers,
			SimpleTripSearchPackage<Map<String, Train>> result) {
		
		// добавляем уже найденные поезда
		addResult(vehicles, localities, organisations, segments, containers, result);
		
		// продолжаем поиск, если он еще не окончен
		if (result.isInProgress()) {
			callables.add(() -> {
				searchTrips(result);
				return result;
			});
		}
	}
	
	private void addResult(Map<String, Vehicle> vehicles, Map<String, Locality> localities,
			Map<String, Organisation> organisations, Map<String, Segment> segments, List<TripContainer> containers,
			SimpleTripSearchPackage<Map<String, Train>> result) {
		TripContainer container = new TripContainer();
		container.setRequest(result.getRequest());
		if (result.getSearchResult() != null) {
			List<Trip> trips = new ArrayList<>();
			for (Train train : result.getSearchResult().values()) {
				
				// проверяем возвращен уже этот поезд или нет 
				if (!train.isAdded()) {
					
					// классы вагонов обьединяем в отдельные рейсы
					for (CarClass clas : train.getClasses()) {
						String segmentId = addSegment(vehicles, localities, organisations, segments, train, clas, result.getRequest().getCurrency());
						if (segmentId != null) {
							Trip resTrip = new Trip();
							resTrip.setId(segmentId);
							trips.add(resTrip);
						}
					}
					train.setAdded(true);
				}
			}
			container.setTrips(trips);
		}
		if (result.getException() != null) {
			container.setError(new RestError(result.getException().getMessage()));
		}
		containers.add(container);
	}
	
	public String addSegment(Map<String, Vehicle> vehicles, Map<String, Locality> localities,
			Map<String, Organisation> organisations, Map<String, Segment> segments, Train train, CarClass clas,
			Currency currency) {
		Segment segment = new Segment();
		segment.setNumber(train.getNumber());
		segment.setDepartureDate(train.getDeparture());
		segment.setArrivalDate(train.getArrival());
		segment.setFreeSeatsCount(clas.getSeats().getAll());
		
		segment.setDeparture(createLocality(localities, train.getPassengerDepartureCode(), train.getPassengerDepartureName()));
		segment.setArrival(createLocality(localities, train.getPassengerArrivalCode(), train.getPassengerArrivalName()));
		
		segment.setCarrier(addOrganisation(organisations, train.getProviderName()));
		segment.setVehicle(addVehicle(vehicles, train.getNumber()));
		
		segment.setPrice(createPrice(clas, currency));
		
		TripIdModel id = new TripIdModel(clas.getCars().get(0).getId(), clas.getName(), clas.getSubclass(), clas.getType(),
				train.getNumber(), train.getPassengerDepartureCode(), train.getPassengerArrivalCode(), train.getDeparture());
		
		segment.setCarriages(new ArrayList<>(clas.getCars().size()));
		for (Car car : clas.getCars()) {
			
			// продавать можно только 2 и 3
			if (car.getOperationTypes().contains("2")
					|| car.getOperationTypes().contains("3")) {
				Carriage carriage = new Carriage();
				id.setCarId(car.getId());
				carriage.setId(id.asString());
				carriage.setNumber(car.getNumber());
				carriage.setClas(getCarClass(clas.getName(), clas.getSubclass()));
				carriage.setFreeLowerPlaces(car.getSeats().getLower());
				carriage.setFreeLowerSidePlaces(car.getSeats().getSideLower());
				carriage.setFreeTopPlaces(car.getSeats().getUpper());
				carriage.setFreeTopSidePlaces(car.getSeats().getSideUpper());
				segment.getCarriages().add(carriage);
			}
		}
		if (!segment.getCarriages().isEmpty()) {
			String key = segment.getCarriages().get(0).getId();
			segments.put(key, segment);
			
			// получаем маршрут
			try {
				TrainRoute route = client.getCachedRoute(train.getDepartureCode(), train.getArrivalCode(), train.getDeparture(), train.getNumber());
				segment.setRoute(createRoute(route, localities));
			} catch (Exception e) {
			}
			return key;
		}
		return null;
	}
	
	private CarriageClass getCarClass(String className, String subClass) {
		switch (className) {
		case "first":
			return CarriageClass.FIRST;
		case "second":
			return CarriageClass.SECOND;
		case "third":
			return CarriageClass.THIRD;
		case "reserved":
			switch (subClass) {
			case "1":
				return CarriageClass.RESERVED_FIRST;
			case "2":
				return CarriageClass.RESERVED_SECOND;
			case "3":
				return CarriageClass.RESERVED_THIRD;
			}
		case "non_reserved":
			return CarriageClass.NON_RESERVED;
		case "comfortable":
			return CarriageClass.COMFORTABLE;
		default:
			return null;
		}
	}
	
	public Locality createLocality(Map<String, Locality> localities, String id, String name) {
		String key = String.valueOf(id);
		Locality station = new Locality();
		station.setName(Lang.RU, name);
		if (localities == null) {
			station.setId(id);
			return station;
		}
		Locality locality = localities.get(key);
		if (locality == null) {
			localities.put(key, station);
		}
		return new Locality(key);
	}
	
	public Organisation addOrganisation(Map<String, Organisation> organisations, String name) {
		if (name == null) {
			return null;
		}
		String key = StringUtil.md5(name);
		Organisation organisation = organisations.get(key);
		if (organisation == null) {
			organisation = new Organisation();
			organisation.setName(name);
			organisations.put(key, organisation);
		}
		return new Organisation(key);
	}
	
	public Vehicle addVehicle(Map<String, Vehicle> vehicles, String number) {
		if (number == null) {
			return null;
		}
		String key = StringUtil.md5(number);
		Vehicle vehicle = vehicles.get(key);
		if (vehicle == null) {
			vehicle = new Vehicle();
			vehicle.setNumber(number);
			vehicles.put(key, vehicle);
		}
		return new Vehicle(key);
	}
	
	private Price createPrice(CarClass clas, Currency currency) {
		BigDecimal amount = null;
		if (currency != null
				&& clas.getExchanges().containsKey(currency)) {
			amount = clas.getExchanges().get(currency);
		} else {
			amount = clas.getCost();
			currency = Currency.UAH;
		}
		// тариф
		Tariff tariff = new Tariff();
		tariff.setId("0");
		tariff.setValue(amount);
		
		// стоимость
		Price price = new Price();
		price.setCurrency(currency);
		price.setAmount(amount);
		price.setTariff(tariff);
		return price;
	}

	@Override
	public Route getRouteResponse(String tripId) {
		TripIdModel idModel = new TripIdModel().create(tripId);
		try {
			TrainRoute route = client.getCachedRoute(idModel.getFrom(), idModel.getTo(), idModel.getDate(), idModel.getTrain());
			return createRoute(route, null);
		} catch (IOCacheException | ResponseError e) {
			throw new RestClientException(e.getMessage());
		}
	}
	
	private Route createRoute(TrainRoute trainRoute, Map<String, Locality> localities) {
		Route route = new Route();
		route.setNumber(trainRoute.getNumber());
		route.setName(Lang.RU, trainRoute.getFirstStation() + " - " + trainRoute.getLastStation());
		List<RoutePoint> path = new ArrayList<>(trainRoute.getStations().size());
		for (RouteStation station : trainRoute.getStations()) {
			RoutePoint point = new RoutePoint();
			point.setDepartureTime(station.getDeparture());
			point.setArrivalTime(station.getArrival());
			point.setLocality(createLocality(localities, station.getCode(), station.getName()));
			path.add(point);
		}
		route.setPath(path);
		return route;
	}

	@Override
	public SeatsScheme getSeatsSchemeResponse(String tripId) {
		CarInfo car = getCarInfo(tripId);
		return schemaController.getScheme(car.getSchema(), car.getClas().getName().equals("reserved") ?
				car.getClas().getType() : null);
	}
	
	private CarInfo getCarInfo(String tripId) {
		try {
			TripIdModel idModel = new TripIdModel().create(tripId);
			Response response = client.getTrain(idModel.getFrom(), idModel.getTo(), idModel.getDate(), idModel.getTrain());
			return client.getCar(response.getSession().getId(), idModel.getCarId());
		} catch (ResponseError e) {
			throw new RestClientException(e.getMessage());
		}
	}

	@Override
	public List<Seat> getSeatsResponse(String tripId) {
		//TODO
		CarInfo car = getCarInfo(tripId);
		Map<String, SeatType> types = schemaController.getCarriageSeats(
				car.getSchema(), car.getClas().getName().equals("reserved") ?
				car.getClas().getType() : null);
		
		List<Seat> newSeats = new ArrayList<>();
		for (Entry<String, Map<String, Integer>> seats : car.getSeats().entrySet()) {
			if (types != null) {
				for (Entry<String, SeatType> entry : types.entrySet()) {
					Seat newSeat = new Seat();
					newSeat.setType(entry.getValue());
					newSeat.setId(entry.getKey());
					newSeat.setNumber(entry.getKey());
					if (SeatType.SEAT == entry.getValue()) {
						newSeat.setStatus(getSeatStatus(seats.getValue().get(entry.getKey())));
					}
					newSeats.add(newSeat);
				}
			} else {
				for (Entry<String, Integer> seat : seats.getValue().entrySet()) {
					Seat newSeat = new Seat();
					newSeat.setType(SeatType.SEAT);
					newSeat.setId(seat.getKey());
					newSeat.setNumber(seat.getKey());
					newSeat.setStatus(getSeatStatus(seat.getValue()));
					newSeats.add(newSeat);
				}
			}
		}
		return newSeats;
	}
	
	private SeatStatus getSeatStatus(Integer status) {
		if (status == null) {
			return SeatStatus.EMPTY;
		}
		// от 1 до 5 - свободные места
		if (status <= 5) {
			return SeatStatus.FREE;
		} else {
			return SeatStatus.SALED;
		}
	}

	@Override
	public List<Tariff> getTariffsResponse(String tripId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<RequiredField> getRequiredFieldsResponse(String tripId) {
		List<RequiredField> required = new ArrayList<>(4);
		required.add(RequiredField.NAME);
		required.add(RequiredField.SURNAME);
		required.add(RequiredField.PHONE);
		required.add(RequiredField.EMAIL);
		return required;
	}

	@Override
	public List<Seat> updateSeatsResponse(String tripId, List<Seat> seats) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<ReturnCondition> getConditionsResponse(String tripId, String tariffId) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public List<Document> getDocumentsResponse(String tripId) {
		throw RestClient.createUnavailableMethod();
	}

}
