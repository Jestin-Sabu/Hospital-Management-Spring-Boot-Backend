package com.hm.payload.request;

import jakarta.validation.constraints.NotBlank;

public class MessageRequest {

	@NotBlank
	private String message;

	public MessageRequest() {

	}

	public MessageRequest(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
