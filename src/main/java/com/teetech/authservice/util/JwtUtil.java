package com.teetech.authservice.util;

import com.teetech.authservice.dto.token.TokenResult;
import com.teetech.authservice.security.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init(){
        String secret = jwtProperties.getSecretKey();

        if (secret == null || secret.isBlank()){
            log.warn("JWT secret key is not provided");
            throw new IllegalArgumentException("JWT secret key must not be blank");
        }

        byte[] decoded;

        try{
            decoded = Decoders.BASE64.decode(secret);
        }catch (Exception e){
            log.warn("JWT secret key is not a valid Base64");
            throw new IllegalArgumentException("JWT secret key is not valid Base64");
        }

        if (decoded.length < 32){
            log.warn("JWT secret key must be at least 32 bytes. Yours got: {}", decoded.length);
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes, got: " + decoded.length);
        }

        if (jwtProperties.getAccessTokenExpiration() <= 0 || jwtProperties.getRefreshTokenExpiration() <= 0){
            log.warn("JWT expiration values should be positive");
            throw new IllegalArgumentException("JWT expiration values must be positive");
        }

        if (jwtProperties.getIssuer() == null || jwtProperties.getIssuer().isBlank()){
            log.warn("JWT issuer is not provided");
            throw new IllegalArgumentException("JWT issuer is not provided");
        }

        this.secretKey = Keys.hmacShaKeyFor(decoded);
    }

    private TokenResult tokenBuilder(UserPrincipal principal, Map<String, Object> claims, long expiration){
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        String token = Jwts.builder()
                .header().add("type", "JWT").and()
                .claims(claims)
                .subject(principal.getUserId().toString())
                .claim("email", principal.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();

        return new TokenResult(token, expiryDate.toInstant());
    }

    public TokenResult generateAccessToken(UserPrincipal principal){
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("token_type", "access");
        claims.put("authorities", principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
        );

        return tokenBuilder(principal, claims, jwtProperties.getAccessTokenExpiration());
    }

    public TokenResult generateRefreshToken(UserPrincipal principal){
        return tokenBuilder(principal, Map.of("token_type", "refresh"),
                jwtProperties.getRefreshTokenExpiration());
    }

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver){
        return resolver.apply(extractAllClaims(token));
    }

    public String extractSubject(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUsername(String token){
        return extractAllClaims(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public Collection<? extends GrantedAuthority> extractAuthorities(String token){
        List<String> authorities = extractAllClaims(token).get("authorities", List.class);

        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    public boolean isAccessTokenValid(String token){
        Claims claims = extractAllClaims(token);
        return "access".equals(claims.get("token_type"));
    }

    public boolean isRefreshTokenValid(String token){
        Claims claims = extractAllClaims(token);
        return "refresh".equals(claims.get("token_type"));
    }
}
