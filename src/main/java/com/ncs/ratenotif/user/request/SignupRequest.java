package com.ncs.ratenotif.user.request;

import com.ncs.ratenotif.user.entity.Roles;

import lombok.Data;

@Data
public class SignupRequest {

	private String userName;
	private String fullName;
	private String password;
	private Roles role;
}
