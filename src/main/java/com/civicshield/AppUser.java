package com.civicshield;
import jakarta.persistence.*;

@Entity
public class AppUser {
  @Id @GeneratedValue public Long id;
  public String name;
  @Column(unique = true) public String email;
  public String hash, role, token;
}
