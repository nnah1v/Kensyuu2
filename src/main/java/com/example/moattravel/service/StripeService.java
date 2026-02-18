package com.example.moattravel.service;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.moattravel.form.ReservationRegisterForm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;

@Service
public class StripeService {
	@Value("${stripe.api-key}")
	private String stripeApiKey;

	private final ReservationService reservationService;
	private final ObjectMapper objectMapper;

	public StripeService(ReservationService reservationService, ObjectMapper objectMapper) {
		this.reservationService = reservationService;
		this.objectMapper = objectMapper;
	}

	//セッションを作成し、Stripeに必要な情報を返す
	public String createStripeSession(String houseName, ReservationRegisterForm reservationRegisterForm,
			HttpServletRequest httpServletRequest) {
		Stripe.apiKey = stripeApiKey;
		String requestUrl = new String(httpServletRequest.getRequestURL());

		SessionCreateParams param = SessionCreateParams.builder()
				.addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
				.addLineItem(
						SessionCreateParams.LineItem.builder()
								.setPriceData(
										SessionCreateParams.LineItem.PriceData.builder()
												.setProductData(
														SessionCreateParams.LineItem.PriceData.ProductData.builder()
																.setName(houseName)
																.build())
												.setUnitAmount((long) reservationRegisterForm.getAmount())
												.setCurrency("jpy")
												.build())
								.setQuantity(1L)
								.build())
				.setMode(SessionCreateParams.Mode.PAYMENT)
				.setSuccessUrl(
						requestUrl.replaceAll("/houses/[0-9]+/reservations/confirm", "") + "/reservations?reserved")
				.setCancelUrl(requestUrl.replace("/reservations/confirm", ""))
				.setPaymentIntentData(
						SessionCreateParams.PaymentIntentData.builder()
								.putMetadata("houseId", reservationRegisterForm.getHouseId().toString())
								.putMetadata("userId", reservationRegisterForm.getUserId().toString())
								.putMetadata("checkinDate", reservationRegisterForm.getCheckinDate())
								.putMetadata("checkoutDate", reservationRegisterForm.getCheckoutDate())
								.putMetadata("numberOfPeople", reservationRegisterForm.getNumberOfPeople().toString())
								.putMetadata("amount", reservationRegisterForm.getAmount().toString())
								.build())
				.build();

		try {
			Session session = Session.create(param);
			return session.getId();
		} catch (StripeException e) {
			e.printStackTrace();
			return "";
		}
	}

	// セッションから予約情報を取得し、ReservationServiceクラスを介してデータベースに登録する
	public void processSessionCompleted(Event event, String payload) {
		Stripe.apiKey = stripeApiKey;

		try {
			JsonNode rootNode = objectMapper.readTree(payload);
			String sessionId = rootNode.path("data").path("object").path("id").asText();
			System.out.println("sessionId=" + sessionId);

			SessionRetrieveParams params = SessionRetrieveParams.builder()
					.addExpand("payment_intent")
					.build();

			Session session = Session.retrieve(sessionId, params, null);

			Map<String, String> paymentIntentObject = session.getPaymentIntentObject().getMetadata();
			System.out.println("metadata=" + paymentIntentObject);

			reservationService.create(paymentIntentObject,sessionId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
