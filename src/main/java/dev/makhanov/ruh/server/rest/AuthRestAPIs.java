package dev.makhanov.ruh.server.rest;

import dev.makhanov.ruh.server.message.request.LoginForm;
import dev.makhanov.ruh.server.message.request.SignUpForm;
import dev.makhanov.ruh.server.message.response.JwtResponse;
import dev.makhanov.ruh.server.model.Role;
import dev.makhanov.ruh.server.model.RoleName;
import dev.makhanov.ruh.server.model.User;
import dev.makhanov.ruh.server.repository.RoleRepository;
import dev.makhanov.ruh.server.repository.UserRepository;
import dev.makhanov.ruh.server.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthRestAPIs {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtProvider jwtProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginForm loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateJwtToken(authentication);
        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    @PostMapping("/signup")
    public ResponseEntity<String> registerUser(@Valid @RequestBody SignUpForm signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return new ResponseEntity<String>("Fail -> Email is already in use!",
                    HttpStatus.BAD_REQUEST);
        }

        // Creating user's account
        User user = new User(signUpRequest.getName(), signUpRequest.getSurname(),
                signUpRequest.getEmail(), encoder.encode(signUpRequest.getPassword()));

        Set<Role> roles = new HashSet<>();
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Fail! -> Cause: User Role not find."));
        roles.add(adminRole);
        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok().body("User registered successfully!");
    }
}
