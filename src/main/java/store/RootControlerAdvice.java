package store;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class RootControlerAdvice {

	@ExceptionHandler(CustomerNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	ResponseData customerNotFoundHandler(CustomerNotFoundException e) {
		return ResponseData.builder().message(e.getMessage()).build();
	}

	@ExceptionHandler(OrderItemProductNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
	ResponseData orderItemProductNotFoundHandler(OrderItemProductNotFoundException e) {
		return ResponseData.builder().message(e.getMessage()).build();
	}

	@Data
	@Builder
	static class ResponseData {
		String message;
	}
}
