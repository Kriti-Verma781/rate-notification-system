package com.ncs.ratenotif.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.ncs.ratenotif.response.AMResponse;
import com.ncs.ratenotif.response.dto.ResponseUtil;
import com.ncs.ratenotif.user.entity.Roles;
import com.ncs.ratenotif.user.entity.UserEntity;
import com.ncs.ratenotif.user.repository.UserRepository;
import com.ncs.ratenotif.user.request.LoginRequest;
import com.ncs.ratenotif.user.request.SignupRequest;

@Service
public class UserServiceImpl implements UserService{

	    @Autowired
	    private UserRepository userRepository;
	    
	    @Override
	    public Object signup(SignupRequest request) {

	        UserEntity existing =
	                userRepository.findByUsername(request.getUserName());

	        if (existing != null) {
	            throw new RuntimeException("User already exists");
	        }

	        UserEntity user = new UserEntity();
	        user.setUsername(request.getUserName());
	        user.setPassword(request.getPassword()); 
	        user.setRole(request.getRole());         
	        user.setFullName(request.getFullName()); // 🔥 IMPORTANT

	        
	        userRepository.save(user);
	       return ResponseUtil.createSuccessMessage(user, "SignUp Sussessfully", HttpStatus.CREATED);
	    }

	    @Override
	    public UserEntity login(LoginRequest request) {

	        UserEntity user =
	                userRepository.findByUsername(request.getUserName());

	        if (user == null) {
	            throw new RuntimeException("User not found");
	        }

	        if (!user.getPassword().equals(request.getPassword())) {
	            throw new RuntimeException("Invalid password");
	        }

	        return user; 
	    }
	    
	    @Override
		public Object getAllUserList() {
			return userRepository
		            .findAll()
		            .stream()
		            .map(c -> c.getFullName()) 
		            .collect(Collectors.toList());
		}
	    
	    @Override
	    public List<AMResponse> getAmList(String username) {

	        UserEntity loggedInUser = userRepository.findByUsername(username);
	        if (loggedInUser == null) {
	            throw new RuntimeException("User not found");
	        }

	        if (loggedInUser.getRole() == Roles.ADMIN || loggedInUser.getRole() == Roles.SUPER_ADMIN) {

	            return userRepository.findAll()
	                    .stream()
	                    .map(u -> new AMResponse(u.getId(), u.getFullName()))
	                    .toList();
	        }

	        if (loggedInUser.getRole() == Roles.AM) {

	            return List.of(
	                    new AMResponse(loggedInUser.getId(), loggedInUser.getFullName())
	            );
	        }

	        return List.of();
	    }

}
