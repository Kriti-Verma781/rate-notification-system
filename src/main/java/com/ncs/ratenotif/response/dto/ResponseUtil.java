package com.ncs.ratenotif.response.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ResponseUtil {

	public static <T> ResponseEntity<ResponseDTO<T>> createSuccessMessage(T data, String message, HttpStatus status){
		ResponseDTO<T> response = new ResponseDTO<>(
				"Success",
				message,
				data,
				status.value(),
				null
				);
		
		return new ResponseEntity<>(response, status);
	}
	
	public static <T> ResponseEntity<ResponseDTO<T>> createErrorMessage(T data, String message, HttpStatus status){
		ResponseDTO<T> response = new ResponseDTO<>(
				"Error",
				message,
				null,
				status.value(),
				data
				);
		return new ResponseEntity<>(response, status);
	}
}