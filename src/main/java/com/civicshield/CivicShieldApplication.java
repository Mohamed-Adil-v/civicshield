package com.civicshield;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class CivicShieldApplication {
  public static void main(String[] a) { SpringApplication.run(CivicShieldApplication.class, a); }

  @Bean CommandLineRunner seedAdmin(UserRepo r) {
    return x -> {
      if (r.findByEmail("admin@civicshield.in").isEmpty()) {
        var u = new AppUser();
        u.name = "Admin"; u.email = "admin@civicshield.in"; u.role = "ADMIN";
        u.hash = new BCryptPasswordEncoder().encode(System.getenv().getOrDefault("ADMIN_PASSWORD", "admin123"));
        r.save(u);
      }
    };
  }
}
