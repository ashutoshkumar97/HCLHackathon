package com.hcl.zbankcard.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String SECRET;

	private SecretKey getKey() {
		return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
	}

	// ✅ Generate token (only username)
	public String generateToken(String username) {
		return Jwts.builder().setSubject(username).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)) // 1 hour
				.signWith(getKey(), SignatureAlgorithm.HS256).compact();
	}

	// ✅ Extract username
	public String extractUsername(String token) {
		return getClaims(token).getSubject();
	}

	// ✅ Validate token
	public boolean validateToken(String token, String username) {
		return username.equals(extractUsername(token)) && !isExpired(token);
	}

	// ✅ Get all claims
	private Claims getClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(getKey()).build().parseClaimsJws(token).getBody();
	}

	// ✅ Check expiration
	private boolean isExpired(String token) {
		return getClaims(token).getExpiration().before(new Date());
	}
}