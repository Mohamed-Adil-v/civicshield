package com.civicshield;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
public class Hazard {
  @Id @GeneratedValue public Long id;
  public Long reporterId;
  public String category, title, status = "REPORTED";
  @Column(length = 2000) public String description;
  public double lat, lng;
  public int severity = 2, confirmations = 0, priority = 0;
  @Column(columnDefinition = "TEXT") public String photo, afterPhoto;
  public Instant createdAt = Instant.now(), resolvedAt;
  @JsonIgnore @ElementCollection(fetch = FetchType.EAGER) public Set<Long> confirmedBy = new HashSet<>();
}
