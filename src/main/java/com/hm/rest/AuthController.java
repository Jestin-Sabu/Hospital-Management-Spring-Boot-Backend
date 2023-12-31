package com.hm.rest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hm.entity.ERole;
import com.hm.entity.Role;
import com.hm.entity.User;
import com.hm.payload.request.LoginRequest;
import com.hm.payload.request.MessageRequest;
import com.hm.payload.request.SignupRequest;
import com.hm.payload.response.JwtResponse;
import com.hm.payload.response.MessageResponse;
import com.hm.repo.RoleRepository;
import com.hm.repo.UserRepository;
import com.hm.security.jwt.JwtUtils;
import com.hm.service.UserDetailsImpl;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
				.collect(Collectors.toList());
		return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(),
				userDetails.getEmail(), roles, getUser(userDetails.getUsername())));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
		}

		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
		}

		// Create new user's account
		User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(),
				encoder.encode(signUpRequest.getPassword()), signUpRequest.getFirstName(), signUpRequest.getLastName(),
				signUpRequest.getMobileNumber(), signUpRequest.getAddress(), signUpRequest.getPin());

		Set<String> strRoles = signUpRequest.getRole();
		Set<Role> roles = new HashSet<>();

		if (strRoles == null) {
			Role userRole = roleRepository.findByName(ERole.ROLE_PATIENT)
					.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
			roles.add(userRole);
		} else {
			strRoles.forEach(role -> {
				switch (role) {
				case "admin":
					Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(adminRole);

					break;
				case "doctor":
					Role modRole = roleRepository.findByName(ERole.ROLE_DOCTOR)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(modRole);

					break;
				default:
					Role userRole = roleRepository.findByName(ERole.ROLE_PATIENT)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(userRole);
				}
			});
		}

		user.setRoles(roles);
		userRepository.save(user);

		return ResponseEntity
				.ok(new MessageResponse("User registered successfully! " + "Username : " + user.getUsername()));
	}

	@PostMapping("/search")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> searchUser(@Valid @RequestBody MessageRequest msg) {
		User user = userRepository.findByUsername(msg.getMessage())
				.orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + msg.getMessage()));
		return ResponseEntity.ok(user);

	}

	@PostMapping("/patient/search")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> searchPatient(@Valid @RequestBody MessageRequest msg) {
		User user = userRepository.findByUsername(msg.getMessage()).orElseThrow(
				() -> new UsernameNotFoundException("Patient Not Found with username: " + msg.getMessage()));
		Boolean flag = false;
		for (Role r : user.getRoles()) {
			if (r.getName() != ERole.ROLE_PATIENT) {
				flag = true;
			}
		}
		if (flag) {
			throw new UsernameNotFoundException("Patient Not Found with username: " + msg.getMessage());
		}
		return ResponseEntity.ok(user);

	}

	private User getUser(String userName) {
		User user = userRepository.findByUsername(userName)
				.orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + userName));
		return user;
	}

	@PostMapping("/doctor/search")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> searchDoctor(@Valid @RequestBody MessageRequest msg) {
		User user = userRepository.findByUsername(msg.getMessage()).orElseThrow(
				() -> new UsernameNotFoundException("Doctor Not Found with username: " + msg.getMessage()));
		Boolean flag = false;
		for (Role r : user.getRoles()) {
			if (r.getName() != ERole.ROLE_DOCTOR) {
				flag = true;
			}
		}
		if (flag) {
			throw new UsernameNotFoundException("Doctor Not Found with username: " + msg.getMessage());
		}
		return ResponseEntity.ok(user);

	}
}
