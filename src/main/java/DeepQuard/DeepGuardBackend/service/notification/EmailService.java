package DeepQuard.DeepGuardBackend.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public void sendVerificationEmail(String toEmail, String verificationToken) {
        logger.info("Sending verification email to: {}", toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromEmail);
        message.setSubject("Deepguard - Verify Your Email");

        String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;
        String emailBody = String.format(
                "Welcome to Deepguard!\n\n" +
                        "Please click the link below to verify your email address:\n" +
                        "%s\n\n" +
                        "This link will expire in 24 hours.\n\n" +
                        "If you didn't create this account, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "The Deepguard Team",
                verificationUrl
        );

        message.setText(emailBody);

        try {
            mailSender.send(message);
            logger.info("Verification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email");
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        logger.info("Sending password reset email to: {}", toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(fromEmail);
        message.setSubject("Deepguard - Password Reset Request");

        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
        String emailBody = String.format(
                "Hello,\n\n" +
                        "You have requested to reset your password for your Deepguard account.\n\n" +
                        "Please click the link below to reset your password:\n" +
                        "%s\n\n" +
                        "This link will expire in 1 hour.\n\n" +
                        "If you didn't request this password reset, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "The Deepguard Team",
                resetUrl
        );

        message.setText(emailBody);

        try {
            mailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email");
        }
    }
}
