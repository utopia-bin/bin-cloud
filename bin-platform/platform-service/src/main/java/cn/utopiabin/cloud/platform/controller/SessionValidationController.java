package cn.utopiabin.cloud.platform.controller;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.platform.service.application.SsoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Internal gateway check: validates the signed token itself; never trusts claimed identity headers. */
@RestController
@RequiredArgsConstructor
public class SessionValidationController {
    private final SsoService service;
    @GetMapping("/internal/sso/validate")
    public ResponseEntity<Void> validate(@RequestHeader("Authorization") String authorization,
                                        @RequestHeader("X-Expected-Audience") String audience) {
        if(!authorization.startsWith("Bearer ")) return ResponseEntity.status(401).build();
        try { service.active(authorization.substring(7),audience); return ResponseEntity.noContent().build(); }
        catch (BizException e) { return ResponseEntity.status(401).build(); }
    }
}
