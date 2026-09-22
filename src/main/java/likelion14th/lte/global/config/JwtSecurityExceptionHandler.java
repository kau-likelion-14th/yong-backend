package likelion14th.lte.global.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import likelion14th.lte.global.api.ApiResponse;
import likelion14th.lte.global.api.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class JwtSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeResponse(response, resolveErrorCode(authException));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        writeResponse(response, ErrorCode.TOKEN_NO_AUTH);
    }

    private ErrorCode resolveErrorCode(AuthenticationException exception) {
        if (containsMessage(exception, "expired")) {
            return ErrorCode.TOKEN_EXPIRED;
        }
        if (exception instanceof InsufficientAuthenticationException
                || containsErrorCode(exception, "missing_authority")
                || containsMessage(exception, "missing_authority")) {
            return ErrorCode.TOKEN_NO_AUTH;
        }
        return ErrorCode.TOKEN_INVALID;
    }

    private boolean containsMessage(Throwable throwable, String keyword) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(keyword)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsErrorCode(Throwable throwable, String errorCode) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof JwtValidationException validationException) {
                for (OAuth2Error error : validationException.getErrors()) {
                    if (errorCode.equals(error.getErrorCode())) {
                        return true;
                    }
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private void writeResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.onFailure(errorCode));
    }
}
