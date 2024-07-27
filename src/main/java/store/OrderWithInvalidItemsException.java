package store;

class OrderWithInvalidItemsException extends RuntimeException {

	OrderWithInvalidItemsException(final String message, final Order order) {
		super(message + ": " + order.toString());
	}
}
