const stripe = Stripe('pk_test_51Sxjm5BuvuJ7cbr1uAlhQ9rL6lzMpHzHIfZDbthL2rGn6vIuqn7L5k9txFfIvPtiifnL4Quv28zAfvmdLo3iWYMP00I2GPX0jl');
const paymentButton = document.querySelector('#paymentButton');

paymentButton.addEventListener('click', () => {
	stripe.redirectToCheckout({
		sessionId: sessionId
	})
});