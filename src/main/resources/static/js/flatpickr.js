let maxDate = new Date();
maxDate.setMonth(maxDate.getMonth() + 3);

flatpickr('#fromCheckinDateToCheckoutDate', {
	mode: "range",
	locale: "ja",
	minDate: "today",
	maxDate: maxDate
});