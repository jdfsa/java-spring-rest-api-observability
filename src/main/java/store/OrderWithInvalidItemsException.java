package store;

import lombok.Getter;

@Getter
class OrderWithInvalidItemsException extends RuntimeException {

	private Order order;

	OrderWithInvalidItemsException(final String message, final Order order) {
		super(message);
		this.order = order;
	}
}
