package com.vn.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendResetPassword(String to, String resetUrl) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Đặt lại mật khẩu của bạn");

            String htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <body style="background-color:#f4f6f8;font-family:Arial,sans-serif;">
                      <table width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                          <td align="center" style="padding:40px 0;">
                            <table width="600" style="background:#ffffff;border-radius:8px;padding:32px;">
                              <tr>
                                <td align="center">
                                  <h2 style="color:#333;">Đặt lại mật khẩu</h2>
                                </td>
                              </tr>
                              <tr>
                                <td style="color:#555;font-size:14px;">
                                  <p>Xin chào,</p>
                                  <p>
                                    Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
                                  </p>
                                  <p style="text-align:center;margin:32px 0;">
                                    <a href="%s"
                                       style="background:#4f46e5;color:#fff;
                                              padding:12px 24px;
                                              text-decoration:none;
                                              border-radius:6px;
                                              font-weight:bold;">
                                      Đặt lại mật khẩu
                                    </a>
                                  </p>
                                  <p>Liên kết này sẽ hết hạn sau <strong>10 phút</strong>.</p>
                                  <p>
                                    Nếu bạn không yêu cầu, vui lòng bỏ qua email này.
                                  </p>
                                  <p style="margin-top:32px;">
                                    Trân trọng,<br/>
                                    <strong>BookShop</strong>
                                  </p>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                      </table>
                    </body>
                    </html>
                    """.formatted(resetUrl);

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Gửi email không thành công", e);
        }
    }
}


