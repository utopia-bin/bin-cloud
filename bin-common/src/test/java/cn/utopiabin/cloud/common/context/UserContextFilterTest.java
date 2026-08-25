package cn.utopiabin.cloud.common.context;

import cn.utopiabin.cloud.common.constant.CommonConstants;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class UserContextFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void acceptsSignedGatewayContextAndClearsItAfterRequest() throws Exception {
        var properties = properties();
        var filter = new UserContextFilter(properties);
        var request = signedRequest(System.currentTimeMillis(), SECRET);
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();
        FilterChain chain = (req, res) -> {
            invoked.set(true);
            assertEquals("10", UserContextHolder.getUserId());
            assertEquals("tenant-admin", UserContextHolder.getRoles().getFirst());
            assertEquals("7", UserContextHolder.getTenantId());
        };

        filter.doFilter(request, response, chain);

        assertTrue(invoked.get());
        assertNull(UserContextHolder.get());
    }

    @Test
    void rejectsForgedContext() throws Exception {
        var filter = new UserContextFilter(properties());
        var request = signedRequest(System.currentTimeMillis(), "abcdef0123456789abcdef0123456789");
        var response = new MockHttpServletResponse();
        var invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(401, response.getStatus());
        assertNull(UserContextHolder.get());
    }

    @Test
    void rejectsExpiredContextSignature() throws Exception {
        var filter = new UserContextFilter(properties());
        long expired = System.currentTimeMillis() - Duration.ofMinutes(1).toMillis();
        var request = signedRequest(expired, SECRET);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> fail("expired signature must not reach controller"));

        assertEquals(401, response.getStatus());
    }

    private GatewayContextProperties properties() {
        var properties = new GatewayContextProperties();
        properties.setSigningSecret(SECRET);
        properties.setSignatureMaxAge(Duration.ofSeconds(30));
        return properties;
    }

    private MockHttpServletRequest signedRequest(long timestamp, String secret) {
        var request = new MockHttpServletRequest("GET", "/users/me");
        request.addHeader(CommonConstants.HEADER_USER_ID, "10");
        request.addHeader(CommonConstants.HEADER_USER_NAME, "alice");
        request.addHeader(CommonConstants.HEADER_TENANT_ID, "7");
        request.addHeader(CommonConstants.HEADER_USER_ROLES, "tenant-admin");
        request.addHeader(CommonConstants.HEADER_GATEWAY_TIMESTAMP, String.valueOf(timestamp));
        request.addHeader(CommonConstants.HEADER_GATEWAY_SIGNATURE,
                GatewayContextSigner.sign(secret, timestamp, "10", "alice", "7", "tenant-admin"));
        return request;
    }
}
