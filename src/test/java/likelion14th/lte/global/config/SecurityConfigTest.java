package likelion14th.lte.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {
    @Test
    void allowsConfiguredFrontendOriginsAndCredentialsOnly() {
        SecurityConfig security = new SecurityConfig();
        ReflectionTestUtils.setField(security, "allowedOrigins", List.of(
                "http://localhost:3000", "https://lteproject.netlify.app"));
        var request = new MockHttpServletRequest("OPTIONS", "/api/auth/kakao");
        var cors = security.corsConfigurationSource().getCorsConfiguration(request);

        assertNotNull(cors);
        assertEquals("https://lteproject.netlify.app", cors.checkOrigin("https://lteproject.netlify.app"));
        assertEquals("http://localhost:3000", cors.checkOrigin("http://localhost:3000"));
        assertNull(cors.checkOrigin("https://unregistered.example"));
        assertEquals(Boolean.TRUE, cors.getAllowCredentials());
        assertNotNull(cors.checkHttpMethod(org.springframework.http.HttpMethod.POST));
        assertNotNull(cors.checkHttpMethod(org.springframework.http.HttpMethod.PATCH));
        assertNotNull(cors.checkHttpMethod(org.springframework.http.HttpMethod.DELETE));
    }
}
