package store;

class OrderItemProductNotFoundException extends RuntimeException {
	OrderItemProductNotFoundException(Long id) {
		super("Could not find product " + id);
	}
}
