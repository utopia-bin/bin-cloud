package cn.utopiabin.cloud.api.open.controller;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.rest.RestResult;
import cn.utopiabin.cloud.api.open.model.WorkbenchProfileVO;
import cn.utopiabin.cloud.platform.api.application.SsoApi;
import cn.utopiabin.cloud.platform.model.dto.application.SsoExchangeDTO;
import cn.utopiabin.cloud.platform.model.dto.application.SsoRefreshDTO;
import cn.utopiabin.cloud.platform.model.vo.application.SsoTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Local single-instance BFF example. Browser cookies carry only an opaque server session ID. */
@RestController
@Tag(name="学习工作台单点登录示例")
@RequestMapping("/workbench")
public class WorkbenchController {
    @DubboReference private SsoApi sso;
    @Value("${workbench.public-base-url:http://localhost:5173}") private String baseUrl;
    @Value("${workbench.client-secret:}") private String secret;
    private static final String TOKENS="workbenchTokens", EXPIRES="workbenchExpires", CSRF="workbenchCsrf";
    private static final SecureRandom RANDOM=new SecureRandom();

    @GetMapping("/start")
    @Operation(summary="应用后端生成state与PKCE后跳转平台登录壳")
    public ResponseEntity<Void> start(@RequestParam long tenantApplicationId,HttpServletRequest request) {
        if(tenantApplicationId<=0) throw new BizException(400,"开通实例ID无效");
        if(secret.isBlank()) throw new BizException(503,"请先在应用管理轮换open-api客户端凭证，并配置open-api的WORKBENCH_SSO_SECRET");
        HttpSession session=request.getSession(); String state=random(),verifier=random();
        synchronized(session) { session.setAttribute("ssoState",state);session.setAttribute("ssoVerifier",verifier);session.setAttribute("ssoStarted",System.currentTimeMillis()); }
        String location=UriComponentsBuilder.fromUriString(baseUrl).path("/sso/launch")
                .queryParam("tenantApplicationId",tenantApplicationId).queryParam("redirectUri",callback())
                .queryParam("state",state).queryParam("codeChallenge",challenge(verifier)).build().encode().toUriString();
        return redirect(location);
    }

    @GetMapping("/callback")
    @Operation(summary="校验state并在后端兑换授权码，浏览器不接触Token")
    public ResponseEntity<Void> callback(@RequestParam String code,@RequestParam String state,HttpServletRequest request) {
        HttpSession session=request.getSession(false);
        if(session==null) throw new BizException(400,"登录发起会话已丢失，请从我的应用重新进入");
        synchronized(session) {
            String saved=(String)session.getAttribute("ssoState"),verifier=(String)session.getAttribute("ssoVerifier");
            Long started=(Long)session.getAttribute("ssoStarted");
            session.removeAttribute("ssoState");session.removeAttribute("ssoVerifier");session.removeAttribute("ssoStarted");
            if(!equal(saved,state) || verifier==null || started==null || System.currentTimeMillis()-started>300_000) throw new BizException(400,"登录状态校验失败或已超时，请重新发起");
            var dto=new SsoExchangeDTO();dto.setClientId("open-api");dto.setClientSecret(secret);dto.setCode(code);dto.setCodeVerifier(verifier);dto.setRedirectUri(callback());
            var tokens=sso.exchange(dto);
            request.changeSessionId();
            save(session,tokens);session.setAttribute(CSRF,random());
        }
        return redirect(baseUrl+"/applications/workbench");
    }

    @GetMapping("/profile")
    @Operation(summary="查询本应用身份、角色、菜单和CSRF令牌")
    public RestResult<WorkbenchProfileVO> profile(HttpServletRequest request) {
        HttpSession session=requireSession(request);
        synchronized(session) {
            var result=new WorkbenchProfileVO(); result.setProfile(sso.profile(access(session),"learning-workbench")); result.setCsrfToken((String)session.getAttribute(CSRF));
            return RestResult.ok(result);
        }
    }

    @PostMapping("/execute")
    @Operation(summary="执行需要workbench:execute权限的学习示例")
    public RestResult<String> execute(@RequestHeader("X-CSRF-Token") String csrf,HttpServletRequest request) {
        var session=requireSession(request);
        synchronized(session) {
            csrf(session,csrf);
            var profile=sso.profile(access(session),"learning-workbench");
            if(!profile.getPermissionCodes().contains("workbench:execute") && !profile.getPermissionCodes().contains("*")) throw new BizException(403,"当前应用角色没有示例执行权限");
            return RestResult.ok("已在租户应用实例 "+profile.getTenantApplicationId()+" 中完成权限校验与示例执行");
        }
    }

    @PostMapping("/logout")
    @Operation(summary="仅退出学习工作台，不影响平台登录")
    public RestResult<Void> logout(@RequestHeader("X-CSRF-Token") String csrf,HttpServletRequest request) {
        var session=requireSession(request);
        synchronized(session) {
            csrf(session,csrf);
            try { sso.logout(access(session),false); }
            finally { session.invalidate(); }
        }
        return RestResult.ok();
    }

    private HttpSession requireSession(HttpServletRequest request) {
        var session=request.getSession(false);
        if(session==null || session.getAttribute(TOKENS)==null) throw new BizException(401,"尚未进入学习工作台，请从我的应用重新登录");
        return session;
    }
    private String access(HttpSession session) {
        var tokens=(SsoTokenVO)session.getAttribute(TOKENS);
        Long expires=(Long)session.getAttribute(EXPIRES);
        if(expires==null || expires<=System.currentTimeMillis()+10_000) {
            var dto=new SsoRefreshDTO();dto.setClientId("open-api");dto.setClientSecret(secret);dto.setRefreshToken(tokens.getRefreshToken());
            tokens=sso.refresh(dto);save(session,tokens);
        }
        return tokens.getAccessToken();
    }
    private void save(HttpSession session,SsoTokenVO tokens) {session.setAttribute(TOKENS,tokens);session.setAttribute(EXPIRES,System.currentTimeMillis()+tokens.getExpiresIn()*1000);}
    private void csrf(HttpSession session,String actual) {if(!equal((String)session.getAttribute(CSRF),actual)) throw new BizException(403,"CSRF校验失败，请刷新工作台");}
    private String callback() { return baseUrl+"/api/open/workbench/callback"; }
    private ResponseEntity<Void> redirect(String uri) {return ResponseEntity.status(302).header("Cache-Control","no-store").header("Referrer-Policy","no-referrer").location(URI.create(uri)).build();}
    private static String random() {byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private static String challenge(String value) {try{return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean equal(String a,String b) {return a!=null && b!=null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
}
