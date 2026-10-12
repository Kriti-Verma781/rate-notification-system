package com.ncs.ratenotif.user.service;

import java.util.List;

import com.ncs.ratenotif.response.AMResponse;
import com.ncs.ratenotif.user.entity.UserEntity;
import com.ncs.ratenotif.user.request.LoginRequest;
import com.ncs.ratenotif.user.request.SignupRequest;

public interface UserService {

	public Object signup(SignupRequest request);
	
	public UserEntity login(LoginRequest request);

	Object getAllUserList();

	List<AMResponse> getAmList(String username);
}
