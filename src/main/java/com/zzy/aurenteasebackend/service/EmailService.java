package com.zzy.aurenteasebackend.service;

import com.zzy.aurenteasebackend.domain.Application;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Async
    public void sendApplicationResultEmail(
            String toEmail, String propertyTitle, String status){
        try{
            log.info("🧵 异步邮件线程启动！正在为租客 [" + toEmail + "] 拼装通知信... 当前线程: " + Thread.currentThread().getName());
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true,"UTF-8");
            helper.setFrom("zhangzhanying648@gmail.com");
            helper.setTo(toEmail);

            boolean isApproved="APPROVED".equals(status);
            String subject = isApproved ? "🎉 Congratulations! Your Rental Application Approved" : "Notice regarding your rental application";
            helper.setSubject("[AU RentEase] " + subject);

            // 🎨 纯正澳洲现代地产风格的 HTML 邮件排版
            String htmlContent = """
                <div style="font-family: 'Segoe UI', Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
                    <div style="background: %s; padding: 32px; text-align: center; color: white;">
                        <span style="font-weight: 900; font-size: 24px; letter-spacing: -0.5px;">AU RentEase Ecosystem</span>
                    </div>
                    <div style="padding: 32px; background: white; color: #1e293b; line-height: 1.6;">
                        <h2 style="margin-top: 0; font-size: 20px; font-weight: 800;">Dear Applicant,</h2>
                        <p style="font-size: 15px;">Thank you for your patience during our premium vetting process.</p>
                        
                        <div style="background: #f8fafc; border-left: 4px solid %s; padding: 20px; border-radius: 8px; margin: 24px 0;">
                            <p style="margin: 0; font-size: 14px; color: #64748b;">Property Asset</p>
                            <p style="margin: 4px 0 12px 0; font-weight: bold; font-size: 16px; color: #0f172a;">%s</p>
                            <p style="margin: 0; font-size: 14px; color: #64748b;">Application Status</p>
                            <p style="margin: 4px 0 0 0; font-weight: 900; font-size: 18px; color: %s;">%s</p>
                        </div>

                        %s
                        
                        <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 32px 0;" />
                        <p style="font-size: 12px; color: #94a3b8; text-align: center; margin: 0;">This is an automated system dispatch from AU RentEase Portals. Please do not reply directly.</p>
                    </div>
                </div>
                """
                    .formatted(
                            isApproved ? "#10b981" : "#0f172a", // Header 背景色：绿 / 深蓝
                            isApproved ? "#10b981" : "#f43f5e", // 边框高亮色
                            propertyTitle,
                            isApproved ? "#10b981" : "#f43f5e", // 状态文本颜色
                            status,
                            isApproved ?
                                    "<p style='font-size: 15px;'><b>Next Steps:</b> Our property manager will contact you within 24 hours to arrange the <b>Tenancy Agreement</b> signing and bond payment secure lodgement.</p>" :
                                    "<p style='font-size: 15px;'>Unfortunately, due to the high volume of competitive applicants for this prime asset, the landlord has proceeded with another offer on this occasion. We wish you all the best in your property hunt.</p>"
                    );

            helper.setText(htmlContent, true); // 第二个参数设为 true 才会渲染 HTML
            mailSender.send(message);

            System.out.println("✨ 异步通知邮件发送成功！目的地: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("💥 异步解构邮件载荷战败: " + e.getMessage());
        }
    }

}
