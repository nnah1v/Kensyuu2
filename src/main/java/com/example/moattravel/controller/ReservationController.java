package com.example.moattravel.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.moattravel.entity.House;
import com.example.moattravel.entity.Reservation;
import com.example.moattravel.entity.User;
import com.example.moattravel.form.ReservationInputForm;
import com.example.moattravel.form.ReservationRegisterForm;
import com.example.moattravel.repository.HouseRepository;
import com.example.moattravel.repository.ReservationRepository;
import com.example.moattravel.security.UserDetailsImpl;
import com.example.moattravel.service.ReservationService;

@Controller
public class ReservationController {
	private final ReservationRepository reservationRepository;
	private final HouseRepository houseRepository;
	private final ReservationService reservationService;

	public ReservationController(ReservationRepository reservationRepository, HouseRepository houseRepository,
			ReservationService reservationService) {
		this.reservationRepository = reservationRepository;
		this.houseRepository = houseRepository;
		this.reservationService = reservationService;

	}

	@GetMapping("/reservations")
	public String index(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
			@PageableDefault(page = 0, size = 10, sort = "id", direction = Direction.DESC) Pageable pageable,
			Model model) {

		User user = userDetailsImpl.getUser();

		Page<Reservation> reservationPage = reservationRepository.findByUserOrderByCreatedAtDesc(user, pageable);

		model.addAttribute("reservationPage", reservationPage);

		return "reservations/index";
	}

	@GetMapping("/houses/{id}/reservations/input")
	public String input(@PathVariable(name = "id") Integer id,
			@ModelAttribute @Validated ReservationInputForm reservationInputForm,
			BindingResult bindingResult,
			RedirectAttributes redirectAttributes,
			Model model) {
		House house = houseRepository.getReferenceById(id);
		Integer numberOfPeople = reservationInputForm.getNumberOfPeople();
		Integer capacity = house.getCapacity();

		if (numberOfPeople != null) {
			if (!reservationService.isWithinCapacity(numberOfPeople, capacity)) {

				FieldError fieldError = new FieldError(bindingResult.getObjectName(),
						"numberOfPeople", "宿泊人数が定員を超えています。");

				bindingResult.addError(fieldError);
			}
		}

		if (bindingResult.hasErrors()) {

			model.addAttribute("house", house);
			model.addAttribute("errorMessage", "予約内容に不備があります。");
			return "houses/show";
		}

		redirectAttributes.addFlashAttribute("reservationInputForm", reservationInputForm);

		return "redirect:/houses/{id}/reservations/confirm";
	}

	@GetMapping("/houses/{id}/reservations/confirm")
	public String confirm(
			@PathVariable(name = "id") Integer id,
			@ModelAttribute ReservationInputForm reservationInputForm,
			@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
			Model model) {

		House house = houseRepository.getReferenceById(id);
		User user = userDetailsImpl.getUser();
		
		//チェックイン日とチェックアウト日を取得する
		LocalDate checkinDate = reservationInputForm.getCheckinDate();
		LocalDate checkoutDate = reservationInputForm.getCheckoutDate();
		
		// 宿泊料金を計算する
		Integer price = house.getPrice();
		Integer amount = reservationService.calculateAmount(checkinDate, checkoutDate, price);
		ReservationRegisterForm reservationRegisterForm = new ReservationRegisterForm(
				house.getId(),
				user.getId(),
				checkinDate.toString(),
				checkoutDate.toString(),
				reservationInputForm.getNumberOfPeople(), amount);

		model.addAttribute("house", house);
		model.addAttribute("reservationRegisterForm", reservationRegisterForm);

		return "reservations/confirm";
	}
}