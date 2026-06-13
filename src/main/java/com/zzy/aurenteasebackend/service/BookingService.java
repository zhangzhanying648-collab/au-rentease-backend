package com.zzy.aurenteasebackend.service;

import com.zzy.aurenteasebackend.controller.BookingController;
import com.zzy.aurenteasebackend.domain.Booking;
import com.zzy.aurenteasebackend.domain.Property;
import com.zzy.aurenteasebackend.domain.User;
import com.zzy.aurenteasebackend.repository.BookingRepository;
import com.zzy.aurenteasebackend.repository.PropertyRepository;
import com.zzy.aurenteasebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final RedissonClient redissonClient;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    private static final String LOCK_PREFIX = "rentease:lock:property:";


    public boolean tryBookingRateLimit() {
        // 1. 获取一个针对具体业务（或特定用户）的限流器 Key
        // 比如限制全网订房总 QPS，或者限制单个用户： "rate:limit:user:" + userId
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("rate:limit:booking:global");

        // 2. 初始化限流规则（仅需初始化一次，后续直接读取）：
        // RateType.OVERALL: 全局所有机器共享令牌
        // 10: 每 1 秒发放 10 个令牌
        rateLimiter.trySetRate(RateType.OVERALL, 10, 1, RateIntervalUnit.SECONDS);

        // 3. 尝试获取 1 个令牌（不等待，拿不到立马返回 false）
        return rateLimiter.tryAcquire(1);
    }

    /**
     * 核心防超卖预订方法
     * @return
     */
    public boolean bookProperty(BookingController.BookingRequest request) {
        // 🚀 大厂核心：利用你已经在 JwtAuthenticationFilter 里对齐的小账本，直接捞出当前合法的用户名[cite: 7]
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + currentUsername));

        String currentUserEmail = user.getEmail();

        Long propertyId = request.getPropertyId();

        String lockKey = LOCK_PREFIX + propertyId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLockAcquired = false;
        try {
            isLockAcquired = lock.tryLock(3, -1, TimeUnit.SECONDS);
            if (!isLockAcquired) {
                log.warn("❌ [分布式锁] 租客 #{} 抢锁失败！说明房源 #{} 正在被其他人疯狂抢订，直接拒绝，防止拥堵。", currentUserEmail, propertyId);
                return false;
            }
            log.info("🎯 [分布式锁] 租客 #{} 成功斩获房源 #{} 的分布式锁，进入独占串行核心业务...", currentUserEmail, propertyId);

            //执行落库
            createBooking(currentUserEmail,request);

            log.info("✅ [业务] 租客 #{} 成功预订房源 #{}！", currentUserEmail, propertyId);
            return true;
        }catch (Exception e){
            log.error("⚠️ 预订抢锁线程被异常中断", e);
            return false;
        }finally {
            if(isLockAcquired&&lock.isHeldByCurrentThread()){
                lock.unlock();
                log.info("🔓 [分布式锁] 房源 #{} 的锁已由租客 #{} 线程安全释放，下一位请进...", propertyId);
            }
        }
    }

    private Booking createBooking(String currentUserEmail,BookingController.BookingRequest request) {


        Long propertyId = request.getPropertyId();


        // 打印测试，看看是不是前端持牌登录的那个用户
        System.out.println("当前收到来自用户 [" + currentUserEmail + "] 的看房预约申请！");
        System.out.println("房源ID: " + propertyId + ", 约看日期: " + request.getBookingDate());
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        Booking booking = new Booking();
        booking.setProperty(property);
        booking.setBookingDate(request.getBookingDate());
        booking.setUserEmail(currentUserEmail); // 咱们上一步用 SecurityContext 抓到的安全 Email
        booking.setStatus("PENDING");

        // TODO: 在这里调用你的 BookingService.save(...) 落库
        Booking saved = bookingRepository.save(booking);
        return saved;
    }

}
