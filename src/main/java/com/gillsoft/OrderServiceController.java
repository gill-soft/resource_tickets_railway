package com.gillsoft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.gillsoft.abstract_rest_service.AbstractOrderService;
import com.gillsoft.client.CarClass;
import com.gillsoft.client.Document;
import com.gillsoft.client.OrderIdModel;
import com.gillsoft.client.Response;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.ServiceIdModel;
import com.gillsoft.client.Train;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.model.Customer;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Organisation;
import com.gillsoft.model.ResponseError;
import com.gillsoft.model.RestError;
import com.gillsoft.model.Seat;
import com.gillsoft.model.Segment;
import com.gillsoft.model.ServiceItem;
import com.gillsoft.model.Vehicle;
import com.gillsoft.model.request.OrderRequest;
import com.gillsoft.model.response.OrderResponse;
import com.gillsoft.util.StringUtil;

public class OrderServiceController extends AbstractOrderService {
	
	@Autowired
	private RestClient client;
	
	@Autowired
	private SearchServiceController search;

	@Override
	public OrderResponse createResponse(OrderRequest request) {
		
		// формируем ответ
		OrderResponse response = new OrderResponse();
		response.setCustomers(request.getCustomers());
		
		// копия для определения пассажиров
		List<ServiceItem> items = new ArrayList<>();
		items.addAll(request.getServices());
		
		Map<String, Organisation> organisations = new HashMap<>();
		Map<String, Locality> localities = new HashMap<>();
		Map<String, Vehicle> vehicles = new HashMap<>();
		Map<String, Segment> segments = new HashMap<>();
		List<ServiceItem> resultItems = new ArrayList<>();
		
		// список билетов
		OrderIdModel orderId = new OrderIdModel();
		orderId.setIds(new ArrayList<>());
		for (Entry<String, List<ServiceItem>> order : getTripItems(request).entrySet()) {
			String[] params = order.getKey().split(";");
			TripIdModel idModel = new TripIdModel().create(params[0]);
			try {
				Response trainResponse = client.getTrain(idModel.getFrom(), idModel.getTo(), idModel.getDate(), idModel.getTrain());
				
				// удаляем ненужный вагон
				removeNotUsedCars(trainResponse, idModel);
				
				client.getCar(trainResponse.getSession().getId(), idModel.getCarId());
				
				// создаем заказ
				Response reservation = reservation(trainResponse, order.getValue(), request.getCustomers());
				Train train = client.getBooking(reservation.getReservation().getId());
				
				// создаем рейс
				Segment segment = new Segment();
				segment.setId(search.addSegment(vehicles, localities, organisations, segments, trainResponse.getTrain(),
						trainResponse.getTrain().getClasses().get(0), request.getCurrency()));
				Segment full = segments.get(segment.getId());
				
				// устанавливаем данные в сервисы
				for (ServiceItem item : order.getValue()) {
					Customer customer = request.getCustomers().get(item.getCustomer().getId());
					ServiceIdModel serviceId = new ServiceIdModel(reservation.getReservation().getId(), getDocumentId(train, customer));
					orderId.getIds().add(serviceId);
					
					item.setId(serviceId.asString());
					item.setNumber(reservation.getReservation().getId());
					item.setExpire(reservation.getReservation().getExpirationTime());
					
					// рейс
					item.setSegment(segment);
					
					// стоимость
					item.setPrice(full.getPrice());
					
					// устанавливаем место
					item.setSeat(createSeat(train, customer));
					resultItems.add(item);
				}
				full.setPrice(null);
			} catch (ResponseError e) {
				for (ServiceItem item : order.getValue()) {
					item.setError(new RestError(e.getMessage()));
					resultItems.add(item);
				}
			}
		}
		response.setOrderId(orderId.asString());
		response.setLocalities(localities);
		response.setVehicles(vehicles);
		response.setOrganisations(organisations);
		response.setSegments(segments);
		response.setServices(resultItems);
		return response;
	}
	
	private String getDocumentId(Train train, Customer customer) throws ResponseError {
		Optional<Document> o = train.getDocuments().stream().filter(document ->
				Objects.equals(document.getFirstName().toUpperCase(), customer.getName().toUpperCase())
					&& Objects.equals(document.getLastName().toUpperCase(), customer.getSurname().toUpperCase())).findFirst();
		if (o.isPresent()) {
			return o.get().getId();
		}
		throw new ResponseError("Can not find in response passenger " + customer.getName() + " " + customer.getSurname());
	}
	
	private Seat createSeat(Train train, Customer customer) throws ResponseError {
		for (Iterator<Document> iterator = train.getDocuments().iterator(); iterator.hasNext();) {
			Document document = iterator.next();
			if (Objects.equals(document.getFirstName().toUpperCase(), customer.getName().toUpperCase())
					&& Objects.equals(document.getLastName().toUpperCase(), customer.getSurname().toUpperCase())) {
				Seat seat = new Seat();
				seat.setId(document.getSeatNumber());
				seat.setNumber(document.getSeatNumber());
				return seat;
			}
		}
		throw new ResponseError("Can not find in response passenger " + customer.getName() + " " + customer.getSurname());
	}
	
	private Response reservation(Response trainResponse, List<ServiceItem> services, Map<String, Customer> customersMap) throws ResponseError {
		List<Customer> customers = services.stream()
				.map(s -> customersMap.get(s.getCustomer().getId())).collect(Collectors.toList());
		List<Seat> seats = services.stream()
				.map(ServiceItem::getSeat).collect(Collectors.toList());
		return client.reservation(
				trainResponse.getSession().getId(), getOperationType(trainResponse.getTrain()), customers, seats);
	}
	
	private void removeNotUsedCars(Response trainResponse, TripIdModel idModel) {
		for (Iterator<CarClass> iterator = trainResponse.getTrain().getClasses().iterator(); iterator.hasNext();) {
			CarClass clas = iterator.next();
			clas.getCars().removeIf(c -> !Objects.equals(c.getId(), idModel.getCarId()));
			if (clas.getCars().isEmpty()) {
				iterator.remove();
			}
		}
	}
	
	private String getOperationType(Train train) throws ResponseError {
		if (train.getClasses().isEmpty()
				|| train.getClasses().get(0).getCars().isEmpty()) {
			throw new ResponseError("Selected carriage is not present");
		}
		if (!train.getClasses().get(0).getCars().get(0).getOperationTypes().contains("2")
				|| !train.getClasses().get(0).getCars().get(0).getOperationTypes().contains("3")) {
			throw new ResponseError("Selected carriage has not operation type 2 or 3");
		}
		if (train.getClasses().get(0).getCars().get(0).getOperationTypes().contains("3")) {
			return "3";
		} else {
			return "2";
		}
	}
	
	/*
	 * В заказе ресурса можно оформить максимум 4 пассажира в одном заказе.
	 */
	private Map<String, List<ServiceItem>> getTripItems(OrderRequest request) {
		Map<String, List<ServiceItem>> trips = new HashMap<>();
		for (ServiceItem item : request.getServices()) {
			String tripId = item.getSegment().getId();
			List<ServiceItem> items = trips.get(tripId);
			if (items == null) {
				items = new ArrayList<>();
				trips.put(tripId, items);
			}
			if (items.size() == 4) {
				trips.put(String.join(";", tripId, StringUtil.generateUUID()), trips.get(tripId));
				items = new ArrayList<>();
				trips.put(tripId, items);
			}
			items.add(item);
		}
		return trips;
	}

	@Override
	public OrderResponse addServicesResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse removeServicesResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse updateCustomersResponse(OrderRequest request) {
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse getResponse(String orderId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse getServiceResponse(String serviceId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse bookingResponse(String orderId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse confirmResponse(String orderId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse cancelResponse(String orderId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse prepareReturnServicesResponse(OrderRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse returnServicesResponse(OrderRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse getPdfDocumentsResponse(OrderRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

}
