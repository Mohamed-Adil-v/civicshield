package com.civicshield;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepo extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByToken(String t);
  Optional<AppUser> findByEmail(String e);
}
interface HazardRepo extends JpaRepository<Hazard, Long> {}
