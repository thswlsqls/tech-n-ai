package com.tech.n.ai.client.mail.domain.mail.template;

import lombok.RequiredArgsConstructor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RequiredArgsConstructor
public class ThymeleafEmailTemplateService implements EmailTemplateService {
    
    private final TemplateEngine templateEngine;
    
    @Override
    public String renderVerificationEmail(String email, String token, String verifyUrl) {
        Context context = createContext(email, token, verifyUrl);
        return templateEngine.process("email/verification", context);
    }
    
    @Override
    public String renderPasswordResetEmail(String email, String token, String resetUrl) {
        Context context = createContext(email, token, resetUrl);
        return templateEngine.process("email/password-reset", context);
    }
    
    private Context createContext(String email, String token, String url) {
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("token", token);
        context.setVariable("verifyUrl", url);
        context.setVariable("resetUrl", url);
        return context;
    }
}
