
package gr.hua.td2x.seekkerai.api.security.authentication;

import gr.hua.td2x.seekkerai.api.security.jwt.JwtService;
import gr.hua.td2x.seekkerai.api.user.dto.LoginRequest;
import gr.hua.td2x.seekkerai.api.user.dto.LoginResponse;
import gr.hua.td2x.seekkerai.api.user.model.User;
import gr.hua.td2x.seekkerai.api.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final long expiration;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            @Value("${security.jwt.expiration}") long expiration) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.expiration = expiration;
    }

    public LoginResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getEmail()
        );

        return new LoginResponse(
                accessToken,
                "Bearer",
                expiration / 1000
        );
    }
}
