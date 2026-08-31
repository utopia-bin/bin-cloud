package cn.utopiabin.cloud.api.open.config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component
public class WorkbenchCacheFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/workbench/")) {
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Referrer-Policy", "no-referrer");
        }
        chain.doFilter(request, response);
    }
}
