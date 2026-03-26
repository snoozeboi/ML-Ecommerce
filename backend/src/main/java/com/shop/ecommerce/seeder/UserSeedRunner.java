package com.shop.ecommerce.seeder;

import com.shop.ecommerce.model.User;
import com.shop.ecommerce.model.UserSegment;
import com.shop.ecommerce.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Profile("seed")
@Component
@RequiredArgsConstructor
@Order(2)
public class UserSeedRunner implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  private static final DateTimeFormatter CSV_DT =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public void run(String... args) throws Exception {
    ClassPathResource resource = new ClassPathResource("seed/users.csv");
    System.out.println(">>> UserSeedRunner starting");
    try (BufferedReader br = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

      String line;
      boolean first = true;
      int count = 0;

      while ((line = br.readLine()) != null) {
        if (first) { first = false; continue; } // skip header
        if (line.isBlank()) continue;

        String[] c = line.split(",", -1);

        // header: id,is_guest,created_at,email,password_hash,user_name
        boolean isGuest = parseBool(c[1]);
        LocalDateTime createdAt = parseDt(c[2]);
        String email = c[3].trim();
        String passwordRaw = c[4].trim();
        String userName = c[5].trim();

        User user = userRepository.findByEmail(email).orElseGet(User::new);

        user.setEmail(email);
        user.setUserName(userName);
        user.setGuest(isGuest);

        // don’t overwrite createdAt if already set
        if (user.getCreatedAt() == null) user.setCreatedAt(createdAt);

        // seed defaults if missing
        if (user.getSegment() == null) user.setSegment(UserSegment.UNCLASSIFIED);
        if (user.getLastActivity() == null) user.setLastActivity(LocalDateTime.now());
        // eventCounter default is 0 already, fine

        // hash if needed
        if (!passwordRaw.startsWith("$2")) {
          user.setPasswordHash(passwordEncoder.encode(passwordRaw));
        } else {
          user.setPasswordHash(passwordRaw);
        }

        userRepository.save(user);
        count++;
      }
      System.out.println("Seeded users: " + count);
    }
  }

  private static boolean parseBool(String s) {
    return "true".equalsIgnoreCase(s.trim());
  }

  private static LocalDateTime parseDt(String s) {
    String v = s.trim();
    if (v.isEmpty()) return null;
    return LocalDateTime.parse(v, CSV_DT);
  }
}