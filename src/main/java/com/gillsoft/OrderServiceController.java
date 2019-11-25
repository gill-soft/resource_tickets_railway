package com.gillsoft;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.gillsoft.abstract_rest_service.AbstractOrderService;
import com.gillsoft.client.Car;
import com.gillsoft.client.CarClass;
import com.gillsoft.client.Document;
import com.gillsoft.client.Exchange;
import com.gillsoft.client.OrderIdModel;
import com.gillsoft.client.Refund;
import com.gillsoft.client.Response;
import com.gillsoft.client.RestClient;
import com.gillsoft.client.ServiceIdModel;
import com.gillsoft.client.Train;
import com.gillsoft.client.TripIdModel;
import com.gillsoft.model.Currency;
import com.gillsoft.model.Customer;
import com.gillsoft.model.DocumentType;
import com.gillsoft.model.Lang;
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
		orderId.setOrders(new ArrayList<>());
		for (ServiceItem item : request.getServices()) {
			TripIdModel idModel = new TripIdModel().create(item.getCarriage().getId());
			try {
				Response trainResponse = client.getTrain(idModel.getFrom(), idModel.getTo(), idModel.getDate(), idModel.getTrain());
				
				// удаляем ненужный вагон
				removeNotUsedCars(trainResponse, idModel);
				
				client.getCar(trainResponse.getSession().getId(), idModel.getCarId());
				
				// создаем рейс
				Segment segment = new Segment();
				segment.setId(search.addSegment(vehicles, localities, organisations, segments, trainResponse.getTrain(),
						trainResponse.getTrain().getClasses().get(0), request.getCurrency()));
				segments.get(segment.getId()).setPrice(null);
			
				// создаем заказ
				Response reservation = client.reservation(
						trainResponse.getSession().getId(), getOperationType(trainResponse.getTrain(), idModel.getCarId()),
						request.getCustomers().get(item.getCustomer().getId()), item.getSeat());
				Train booking = client.getBooking(reservation.getReservation().getId());
				
				// устанавливаем данные в сервисы
				Customer customer = request.getCustomers().get(item.getCustomer().getId());
				
				// создаем ид заказа
				ServiceIdModel serviceId = new ServiceIdModel(reservation.getReservation().getId(),
						getDocumentId(booking, customer),
						reservation.getReservation().getCost(),
						Currency.valueOf(reservation.getReservation().getCurrency()));
				orderId.getOrders().add(serviceId);
				
				item.setId(serviceId.asString());
				item.setNumber(reservation.getReservation().getId());
				item.setExpire(reservation.getReservation().getExpirationTime());
				
				// рейс
				item.setSegment(segment);
				
				// стоимость
				item.setPrice(createPrice(booking, request.getCurrency()));
				
				// устанавливаем место
				item.setSeat(createSeat(booking, customer));
				resultItems.add(item);
			} catch (ResponseError e) {
				item.setError(new RestError(e.getMessage()));
				resultItems.add(item);
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
	
	private Price createPrice(Train booking, Currency currency) {
		BigDecimal amount = null;
		if (currency != null) {
			for (Exchange exchange : booking.getExchanges()) {
				if (currency.name().equals(exchange.getCurrency())) {
					amount = exchange.getCost();
					break;
				}
			}
		} else {
			amount = booking.getCost();
			currency = Currency.UAH;
		}
		// тариф
		Tariff tariff = new Tariff();
		tariff.setId(booking.getDocuments().get(0).getAdult());
		if ("1".equals(tariff.getId())) {
			tariff.setName(Lang.RU, "Взрослый");
			tariff.setName(Lang.UA, "Дорослий");
			tariff.setName(Lang.EN, "Adult");
		} else {
			tariff.setName(Lang.RU, "Детский");
			tariff.setName(Lang.UA, "Дитячий");
			tariff.setName(Lang.EN, "Child");
		}
		tariff.setValue(amount);
		
		// стоимость
		Price price = new Price();
		price.setCurrency(currency);
		price.setAmount(amount);
		price.setTariff(tariff);
		return price;
	}
	
	private String getDocumentId(Train train, Customer customer) throws ResponseError {
		if (train.getDocuments() == null
				|| train.getDocuments().isEmpty()) {
			throw new ResponseError("Can not find in response passenger " + customer.getName() + " " + customer.getSurname());
		} else {
			return train.getDocuments().get(0).getId();
		}
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
			Train confirm = client.commit(id.getId(), id.getCost(), id.getCurrency().name());
			if (!confirm.isPaid()) {
				throw new ResponseError("Can not pay service.");
			}
		}, RestClient.PAIED_STATUS, "Can not pay service. Pay applied only to reserved services.");
	}

	@Override
	public OrderResponse cancelResponse(String orderId) {
		return confirmOperation(orderId, (id) -> {
			Train cancel = client.cancelBooking(id.getId());
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
		for (ServiceIdModel idModel : model.getOrders()) {
			try {
				// получаем заказ и проверяем статус
				Train booking = client.getBooking(idModel.getId());
				if (booking.getStatus() == RestClient.RESERVETION_STATUS) {
					
					// выполняем подтверждение
					operation.confirm(idModel);
				} else if (booking.getStatus() != confirmStatus) {
					throw new ResponseError(errorMessage);
				}
				addServiceItems(resultItems, idModel, true, null);
			} catch (ResponseError e) {
				addServiceItems(resultItems, idModel, false, new RestError(e.getMessage()));
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
				price.setCurrency(Currency.valueOf(refund.getCurrency()));
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
		OrderResponse response = new OrderResponse();
		response.setServices(new ArrayList<>());
		
		// преобразовываем ид заказа в объкт
		OrderIdModel model = new OrderIdModel().create(request.getOrderId());
		
		// отменяем заказы и формируем ответ
		for (ServiceIdModel idModel : model.getOrders()) {
			try {
				String base64 = client.getBase64Ticket(idModel.getId());
				if (base64 != null) {
					List<com.gillsoft.model.Document> documents = new ArrayList<>();
					com.gillsoft.model.Document document = new com.gillsoft.model.Document();
					document.setType(DocumentType.TICKET);
					document.setBase64(base64);
					documents.add(document);
					ServiceItem item = new ServiceItem();
					item.setId(idModel.asString());
					item.setDocuments(documents);
					response.getServices().add(item);
				}
			} catch (ResponseError e) {
				addServiceItems(response.getServices(), idModel, false, new RestError(e.getMessage()));
			}
		}
		return response;
	}
	
	private interface ConfirmOperation {
		
		public void confirm(ServiceIdModel id) throws ResponseError;
		
	}

}
