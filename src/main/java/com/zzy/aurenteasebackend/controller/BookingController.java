package com.zzy.aurenteasebackend.controller;

import com.zzy.aurenteasebackend.domain.Booking;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.domain.User;
import com.zzy.aurenteasebackend.repository.BookingRepository;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import com.zzy.aurenteasebackend.repository.UserRepository;
import lombok.Data;
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
//但在真实的商业项目（如 RentEase）中，直接写 * 是大厂安全合规的大忌
//@CrossOrigin(origins = "*") // 确保跨域畅通
public class BookingController {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository ;

    public BookingController(BookingRepository bookingRepository, UserRepository userRepository, PropertyRepository propertyRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
    }

    public BookingRepository getBookingRepository() {
        return bookingRepository;
    }


    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingRequest request) {
        // 🚀 大厂核心：利用你已经在 JwtAuthenticationFilter 里对齐的小账本，直接捞出当前合法的用户名[cite: 7]
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        String currentUserEmail = user.getEmail();


        // 打印测试，看看是不是前端持牌登录的那个用户
        System.out.println("当前收到来自用户 [" + currentUsername + "] 的看房预约申请！");
        System.out.println("房源ID: " + request.getPropertyId() + ", 约看日期: " + request.getBookingDate());

        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        Booking booking = new Booking();
        booking.setProperty(property);
        booking.setBookingDate(request.getBookingDate());
        booking.setUserEmail(currentUserEmail); // 咱们上一步用 SecurityContext 抓到的安全 Email
        booking.setStatus("PENDING");

        // TODO: 在这里调用你的 BookingService.save(...) 落库
        Booking saved = bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Inspection booked successfully for " + currentUsername
        ));
    }

    @Data
    static class BookingRequest {
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
