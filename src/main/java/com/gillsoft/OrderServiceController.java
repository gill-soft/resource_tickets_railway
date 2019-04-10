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
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractOrderService;
import com.gillsoft.client.Car;
import com.gillsoft.client.CarClass;
import com.gillsoft.client.Document;
import com.gillsoft.client.OrderIdModel;
import com.gillsoft.client.Refund;
import com.gillsoft.client.Response;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.ServiceIdModel;
import com.gillsoft.client.Train;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.model.Customer;
import com.gillsoft.model.Locality;
import com.gillsoft.model.Organisation;
import com.gillsoft.model.Price;
import com.gillsoft.model.ResponseError;
import com.gillsoft.model.RestError;
import com.gillsoft.model.Seat;
import com.gillsoft.model.Segment;
import com.gillsoft.model.ServiceItem;
import com.gillsoft.model.Tariff;
import com.gillsoft.model.Vehicle;
import com.gillsoft.model.request.OrderRequest;
import com.gillsoft.model.response.OrderResponse;
import com.gillsoft.util.StringUtil;

@RestController
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
		orderId.setOrders(new HashMap<>());
		for (Entry<String, List<ServiceItem>> order : getTripItems(request).entrySet()) {
			String[] params = order.getKey().split(";");
			TripIdModel idModel = new TripIdModel().create(params[0]);
			try {
				Response trainResponse = client.getTrain(idModel.getFrom(), idModel.getTo(), idModel.getDate(), idModel.getTrain());
				
				// удаляем ненужный вагон
				removeNotUsedCars(trainResponse, idModel);
				
				client.getCar(trainResponse.getSession().getId(), idModel.getCarId());
				
				// создаем заказ
				Response reservation = reservation(trainResponse, order.getValue(), request.getCustomers(), idModel.getCarId());
				Train train = client.getBooking(reservation.getReservation().getId());
				
				// создаем рейс
				Segment segment = new Segment();
				segment.setId(search.addSegment(vehicles, localities, organisations, segments, trainResponse.getTrain(),
						trainResponse.getTrain().getClasses().get(0), request.getCurrency()));
				Segment fullSegment = segments.get(segment.getId());
				
				// создаем ид заказов
				String reservationKey = String.join(";", reservation.getReservation().getId(),
						reservation.getReservation().getCost().toString(),
						reservation.getReservation().getCurrency().name());
				orderId.getOrders().put(reservationKey, new ArrayList<>(order.getValue().size()));
				
				// устанавливаем данные в сервисы
				for (ServiceItem item : order.getValue()) {
					try {
						Customer customer = request.getCustomers().get(item.getCustomer().getId());
						ServiceIdModel serviceId = new ServiceIdModel(reservation.getReservation().getId(), getDocumentId(train, customer));
						orderId.getOrders().get(reservationKey).add(serviceId);
						
						item.setId(serviceId.asString());
						item.setNumber(reservation.getReservation().getId());
						item.setExpire(reservation.getReservation().getExpirationTime());
						
						// рейс
						item.setSegment(segment);
						
						// стоимость
						item.setPrice(fullSegment.getPrice());
						
						// устанавливаем место
						item.setSeat(createSeat(train, customer));
						resultItems.add(item);
					} catch (ResponseError e) {
						item.setError(new RestError(e.getMessage()));
						resultItems.add(item);
					}
				}
				fullSegment.setPrice(null);
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
	
	private Response reservation(Response trainResponse, List<ServiceItem> services, Map<String, Customer> customersMap,
			String carId) throws ResponseError {
		List<Customer> customers = services.stream()
				.map(s -> customersMap.get(s.getCustomer().getId())).collect(Collectors.toList());
		List<Seat> seats = services.stream()
				.map(ServiceItem::getSeat).collect(Collectors.toList());
		return client.reservation(
				trainResponse.getSession().getId(), getOperationType(trainResponse.getTrain(), carId), customers, seats);
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
	
	private String getOperationType(Train train, String carId) throws ResponseError {
		Car finded = null;
		for (CarClass clas : train.getClasses()) {
			Optional<Car> o = clas.getCars().stream().filter(car -> Objects.equals(car.getId(), carId)).findFirst();
			if (o.isPresent()) {
				finded = o.get();
				break;
			}
		}
		if (finded == null) {
			throw new ResponseError("Selected carriage is not present");
		}
		if (!finded.getOperationTypes().contains("2")
				&& !finded.getOperationTypes().contains("3")) {
			throw new ResponseError("Selected carriage has not operation type 2 or 3");
		}
		if (finded.getOperationTypes().contains("3")) {
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
		throw RestClient.createUnavailableMethod();
	}

	@Override
	public OrderResponse confirmResponse(String orderId) {
		return confirmOperation(orderId, (id) -> {
			String[] params = id.split(";");
			Train confirm = client.commit(params[0], params[1], params[2]);
			if (!confirm.isPaid()) {
				throw new ResponseError("Can not pay service.");
			}
		}, RestClient.PAIED_STATUS, "Can not pay service. Pay applied only to reserved services.");
	}

	@Override
	public OrderResponse cancelResponse(String orderId) {
		return confirmOperation(orderId, (id) -> {
			String reservationId = id.split(";")[0];
			Train cancel = client.cancelBooking(reservationId);
			if (!cancel.isCancelled()) {
				throw new ResponseError("Can not cancel service.");
			}
		}, RestClient.CANCEL_STATUS, "Can not cancel service. Cancel applied only to reserved services.");
	}
	
	private OrderResponse confirmOperation(String orderId, ConfirmOperation operation, int confirmStatus, String errorMessage) {
		// формируем ответ
		OrderResponse response = new OrderResponse();
		List<ServiceItem> resultItems = new ArrayList<>();
		response.setOrderId(orderId);
		response.setServices(resultItems);
		
		// преобразовываем ид заказа в объкт
		OrderIdModel model = new OrderIdModel().create(orderId);
		
		// отменяем заказы и формируем ответ
		for (Entry<String, List<ServiceIdModel>> entry : model.getOrders().entrySet()) {
			String reservationId = entry.getKey().split(";")[0];
			try {
				// получаем заказ и проверяем статус
				Train booking = client.getBooking(reservationId);
				if (booking.getStatus() == RestClient.RESERVETION_STATUS) {
					
					// выполняем подтверждение
					operation.confirm(entry.getKey());
				} else if (booking.getStatus() != confirmStatus) {
					throw new ResponseError(errorMessage);
				}
				for (ServiceIdModel idModel : entry.getValue()) {
					addServiceItems(resultItems, idModel, true, null);
				}
			} catch (ResponseError e) {
				for (ServiceIdModel idModel : entry.getValue()) {
					addServiceItems(resultItems, idModel, false, new RestError(e.getMessage()));
				}
			}
		}
		return response;
	}
	
	private void addServiceItems(List<ServiceItem> resultItems, ServiceIdModel ticket, boolean confirmed,
			RestError error) {
		ServiceItem serviceItem = new ServiceItem();
		serviceItem.setId(ticket.asString());
		serviceItem.setConfirmed(confirmed);
		serviceItem.setError(error);
		resultItems.add(serviceItem);
	}

	@Override
	public OrderResponse prepareReturnServicesResponse(OrderRequest request) {
		OrderResponse response = new OrderResponse();
		response.setServices(new ArrayList<>(request.getServices().size()));
		for (ServiceItem serviceItem : request.getServices()) {
			ServiceIdModel model = new ServiceIdModel().create(serviceItem.getId());
			try {
				Refund refund = client.getRefundAmount(model.getId(), model.getPassId());
				
				// тариф
				Tariff tariff = new Tariff();
				tariff.setId("0");
				tariff.setValue(refund.getAmount());
				
				// стоимость
				Price price = new Price();
				price.setCurrency(refund.getCurrency());
				price.setAmount(refund.getAmount());
				price.setTariff(tariff);
				
				serviceItem.setPrice(price);
			} catch (ResponseError e) {
				serviceItem.setError(new RestError(e.getMessage()));
			}
			response.getServices().add(serviceItem);
		}
		return response;
	}

	@Override
	public OrderResponse returnServicesResponse(OrderRequest request) {
		OrderResponse response = new OrderResponse();
		response.setServices(new ArrayList<>(request.getServices().size()));
		for (ServiceItem serviceItem : request.getServices()) {
			ServiceIdModel model = new ServiceIdModel().create(serviceItem.getId());
			try {
				// получаем заказ и проверяем статус
				Train booking = client.getBooking(model.getId());
				if (booking.getStatus() == RestClient.PAIED_STATUS) {
					
					// возвращаем заказ
					Refund refund = client.refund(model.getId(), model.getPassId());
					if (!refund.isSuccess()) {
						throw new ResponseError("Can not return service.");
					}
				} else if (booking.getStatus() != RestClient.RETURNED_STATUS) {
					throw new ResponseError("Can not return service. Return applied only to pied services.");
				}
				serviceItem.setConfirmed(true);
			} catch (ResponseError e) {
				serviceItem.setConfirmed(false);
				serviceItem.setError(new RestError(e.getMessage()));
			}
			response.getServices().add(serviceItem);
		}
		return response;
	}

	@Override
	public OrderResponse getPdfDocumentsResponse(OrderRequest request) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private interface ConfirmOperation {
		
		public void confirm(String id) throws ResponseError;
		
	}

}
