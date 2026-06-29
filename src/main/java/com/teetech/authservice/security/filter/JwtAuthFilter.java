package com.teetech.authservice.security.filter;

import com.teetech.authservice.entity.Authority;
import com.teetech.authservice.entity.User;
import com.teetech.authservice.security.model.UserPrincipal;
import com.teetech.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.teetech.authservice.util.SecurityConstants.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    private Optional<String> extractToken(HttpServletRequest request){
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)){
            return Optional.empty();
        }

        return Optional.of(authHeader.substring(BEARER_PREFIX.length()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        Optional<String> token = extractToken(request);

        if (token.isEmpty()){
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = token.get();
        String userId;

        try{
            userId = jwtUtil.extractSubject(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null){

                if (!jwtUtil.isAccessTokenValid(jwt)){
                    filterChain.doFilter(request, response);
                    return;
                }

                Set<Authority> authorities = jwtUtil.extractAuthorities(jwt).stream()
                        .map(a -> Authority.builder().name(a.getAuthority()).build())
                        .collect(Collectors.toSet());

                User partialUser = User.builder()
                        .id(UUID.fromString(userId))
                        .username(jwtUtil.extractUsername(jwt))
                        .authorities(authorities)
                        .build();

                UserPrincipal principal = new UserPrincipal(partialUser);

                var authToken = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException e) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
