package com.civicshield;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api")
public class ApiController {
  final UserRepo users; final HazardRepo hz; final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
  ApiController(UserRepo u, HazardRepo h) { users = u; hz = h; }

  static ResponseStatusException err(int c, String m) { return new ResponseStatusException(HttpStatus.valueOf(c), m); }
  static boolean open(Hazard h) { return !List.of("RESOLVED", "REJECTED").contains(h.status); }
  static void score(Hazard h) { h.priority = h.severity * 10 + h.confirmations * 3 + (int) ChronoUnit.DAYS.between(h.createdAt, Instant.now()); }
  static double dist(double a, double b, double c, double d) {  // metres (haversine)
    double p = Math.PI / 180, x = Math.sin((c - a) * p / 2), y = Math.sin((d - b) * p / 2);
    return 12742000 * Math.asin(Math.sqrt(x * x + Math.cos(a * p) * Math.cos(c * p) * y * y));
  }
  AppUser need(String t) {
    return (t == null || t.isBlank() ? Optional.<AppUser>empty() : users.findByToken(t)).orElseThrow(() -> err(401, "Please log in first"));
  }
  Map<String, Object> session(AppUser u) {
    u.token = UUID.randomUUID().toString(); users.save(u);
    return Map.of("token", u.token, "role", u.role, "name", u.name);
  }

  @PostMapping("/register") Map<String, Object> register(@RequestBody Map<String, String> b) {
    String e = b.getOrDefault("email", "").trim().toLowerCase(), p = b.getOrDefault("password", "");
    if (!e.contains("@") || p.length() < 6) throw err(400, "Enter a valid email and a password of 6+ characters");
    if (users.findByEmail(e).isPresent()) throw err(400, "This email is already registered");
    var u = new AppUser(); u.name = b.getOrDefault("name", "Citizen"); u.email = e; u.hash = enc.encode(p); u.role = "CITIZEN";
    return session(u);
  }
  @PostMapping("/login") Map<String, Object> login(@RequestBody Map<String, String> b) {
    var u = users.findByEmail(b.getOrDefault("email", "").trim().toLowerCase())
        .filter(x -> enc.matches(b.getOrDefault("password", ""), x.hash)).orElseThrow(() -> err(401, "Wrong email or password"));
    return session(u);
  }

  @GetMapping("/hazards") List<Hazard> list() { return hz.findAll(); }

  @PostMapping("/hazards") ResponseEntity<?> add(@RequestHeader(value = "X-Token", required = false) String t,
      @RequestBody Hazard h, @RequestParam(defaultValue = "false") boolean force) {
    var u = need(t);
    if (h.title == null || h.title.isBlank()) throw err(400, "Add a short title");
    if (h.category == null) h.category = "Pothole";
    if (!force) for (var o : hz.findAll())  // duplicate detection: same category within 50 m
      if (open(o) && o.category.equals(h.category) && dist(o.lat, o.lng, h.lat, h.lng) < 50) return ResponseEntity.status(409).body(o);
    h.id = null; h.reporterId = u.id; h.status = "REPORTED"; h.confirmations = 0; h.resolvedAt = null; h.afterPhoto = null;
    h.createdAt = Instant.now(); h.confirmedBy = new HashSet<>(); score(h);
    return ResponseEntity.ok(hz.save(h));
  }

  @PostMapping("/hazards/{id}/confirm") Hazard confirm(@RequestHeader(value = "X-Token", required = false) String t, @PathVariable Long id) {
    var u = need(t); var h = hz.findById(id).orElseThrow(() -> err(404, "Not found"));
    if (h.confirmedBy.add(u.id)) { h.confirmations++; score(h); hz.save(h); }
    return h;
  }

  @PatchMapping("/hazards/{id}") Hazard update(@RequestHeader(value = "X-Token", required = false) String t,
      @PathVariable Long id, @RequestBody Map<String, String> b) {
    if (!"ADMIN".equals(need(t).role)) throw err(403, "Admins only");
    var h = hz.findById(id).orElseThrow(() -> err(404, "Not found"));
    if (b.get("status") != null) { h.status = b.get("status"); if (h.status.equals("RESOLVED")) h.resolvedAt = Instant.now(); }
    if (b.get("afterPhoto") != null) h.afterPhoto = b.get("afterPhoto");
    score(h); return hz.save(h);
  }

  @GetMapping("/analytics") Map<String, Object> analytics() {
    var all = hz.findAll(); var r = new LinkedHashMap<String, Object>();
    r.put("total", all.size());
    r.put("byCategory", all.stream().collect(Collectors.groupingBy(x -> x.category, TreeMap::new, Collectors.counting())));
    r.put("byStatus", all.stream().collect(Collectors.groupingBy(x -> x.status, TreeMap::new, Collectors.counting())));
    r.put("avgResolveHours", all.stream().filter(x -> x.resolvedAt != null)
        .mapToDouble(x -> Duration.between(x.createdAt, x.resolvedAt).toMinutes() / 60.0).average().orElse(0));
    r.put("overdue", all.stream().filter(x -> open(x) && x.createdAt.isBefore(Instant.now().minus(7, ChronoUnit.DAYS))).count());
    return r;
  }
}
