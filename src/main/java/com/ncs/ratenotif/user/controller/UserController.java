package com.ncs.ratenotif.user.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ncs.ratenotif.response.AMResponse;
import com.ncs.ratenotif.response.dto.ResponseDTO;
import com.ncs.ratenotif.response.dto.ResponseUtil;
import com.ncs.ratenotif.user.entity.Roles;
import com.ncs.ratenotif.user.entity.UserEntity;
import com.ncs.ratenotif.user.request.LoginRequest;
import com.ncs.ratenotif.user.request.SignupRequest;
import com.ncs.ratenotif.user.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userService;
	
	@PostMapping("/signup")
	public ResponseEntity<ResponseDTO<Object>> userSignup(@RequestBody SignupRequest req){
		return ResponseUtil.createSuccessMessage(userService.signup(req), "Signup successfully", HttpStatus.CREATED);
	}
	
	@PostMapping("/login")
	public ResponseEntity<ResponseDTO<UserEntity>> userLogin(
	        @RequestBody LoginRequest req) {

	    UserEntity user = userService.login(req);

	    return ResponseUtil.createSuccessMessage(
	            user,
	            "Login successfully",
	            HttpStatus.OK
	    );
	}

	@GetMapping("/getRoles")
    public ResponseEntity<List<String>> getRoles() {

        List<String> roles = Arrays.stream(Roles.values())
                                   .map(Enum::name)
                                   .toList();

        return ResponseEntity.ok(roles);
    }
	
	@GetMapping("/getUserList")
	public ResponseEntity<ResponseDTO<Object>> getUserList(){
		return ResponseUtil.createSuccessMessage(userService.getAllUserList(), "Data Found", HttpStatus.FOUND);
	}
	
	@GetMapping("/getAmList")
	public ResponseEntity<List<AMResponse>> getAmList(
	        @RequestParam String username) {

	    return ResponseEntity.ok(userService.getAmList(username));
	}
}
