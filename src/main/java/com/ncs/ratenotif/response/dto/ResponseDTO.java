package com.ncs.ratenotif.response.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO<T> {

	private String status;
	private String message;
	private T data;
	private Integer statusCode;
	private Object details;
}