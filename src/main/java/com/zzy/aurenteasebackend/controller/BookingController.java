package com.zzy.aurenteasebackend.controller;

import com.zzy.aurenteasebackend.domain.Booking;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.domain.User;
import com.zzy.aurenteasebackend.repository.BookingRepository;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import com.zzy.aurenteasebackend.repository.UserRepository;
import com.zzy.aurenteasebackend.service.BookingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
//但在真实的商业项目（如 RentEase）中，直接写 * 是大厂安全合规的大忌
//@CrossOrigin(origins = "*") // 确保跨域畅通
public class BookingController {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;


    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        bookingService.bookProperty(request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Inspection booked successfully for " + currentUsername
        ));
    }

    @Data
    public static class BookingRequest {
        private Long propertyId;
        private LocalDate bookingDate;
    }

    @GetMapping("/my")
    public ResponseEntity<List<Booking>> getMyBookings() {
        // 🔒 核心安全战术：直接从安全上下文中剥离当前经 JWT 校验过的合法 Email
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        String currentUserEmail = user.getEmail();

        System.out.println("🔍 正在为租客捞取看房行程单，当前用户: " + currentUserEmail);

        // 🚀 顺着你之前在 BookingRepository 里写好的战术查询方法直接落库打捞
        List<Booking> myBookings = bookingRepository.findByUserEmail(currentUserEmail);

        return ResponseEntity.ok(myBookings);
    }
}
