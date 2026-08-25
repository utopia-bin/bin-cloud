package cn.utopiabin.cloud.common.utils;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * TI签名工具
 *
 * @author Bin
 * @version 1.0
 * @date 2025/6/9 09:28
 * @since 1.0
 */
@Slf4j
@Getter
@Setter
public class TiSignUtil {
    private static final DateTimeFormatter UTC_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);

    /**
     * 请求header的host字段
     */
    private String host;
    /**
     * 请求接口action
     */
    private String xtcAction;
    /**
     * 请求接口版本
     */
    private String xtcVersion;
    /**
     * 请求接口服务名
     */
    private String xtcService;
    /**
     * 请求unix时间搓，精确到秒
     */
    private String xtcTimestamp;
    /**
     * http请求Header的Content-type值，当前网关只支持: application/json  multipart/form-data
     */
    private String contentType;

    /**
     * http请求方法，只能为 POST 或者 GET
     */
    private String httpMethod;

    /**
     * Ti平台获取的签名密钥(通过 管理中心-个人中心-密钥管理 获取)，非常重要，请妥善保管
     */
    private String secretId;
    private String secretKey;

    public TiSignUtil(String host, String action, String version, String service, String contentType, String httpMethod,
                      String secretId, String secretKey) {
        this.host = host;
        this.xtcAction = action;
        this.xtcVersion = version;
        this.xtcService = service;
        this.contentType = contentType;
        this.httpMethod = httpMethod;

        this.secretId = secretId;
        this.secretKey = secretKey;
    }

    public Map<String, String> buildHeader(Map<String, Object> params) throws Exception {
        // 1. 构造canonical request 字符串
        if (httpMethod == null) {
            throw new Exception("Request method should not be null, can only be GET or POST");
        }

        // 生成签名有效期的时间为60分钟
        var timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        this.xtcTimestamp = timestamp;

        var requestPayload = "POST".equals(httpMethod) && params != null && !params.isEmpty()
                ? JSONObject.toJSONString(params) : "";
        var hashedRequestPayload = sha256Hex(requestPayload);

        // 1.6 按照固定格式拼接所有请求信息
        var canonicalRequest = """
                %s
                /
                
                content-type:%s
                host:%s
                
                content-type;host
                %s""".formatted(httpMethod, contentType, host, hashedRequestPayload);

        // 2. 构造用于计算签名的字符串
        var date = UTC_DATE.format(Instant.ofEpochSecond(Long.parseLong(timestamp)));
        var credentialScope = "%s/%s/tc3_request".formatted(date, xtcService);
        var hashedCanonicalRequest = sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        var stringToSign = """
                TC3-HMAC-SHA256
                %s
                %s
                %s""".formatted(timestamp, credentialScope, hashedCanonicalRequest);

        // 3. 对第2步构造的字符串进行签名
        var secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        var secretService = hmac256(secretDate, xtcService);
        var secretSigning = hmac256(secretService, "tc3_request");
        var signature = HexFormat.of().formatHex(hmac256(secretSigning, stringToSign)).toLowerCase();

        // 4. 构造http请求头的authorization字段
        var authorization = "TC3-HMAC-SHA256 Credential=%s/%s, SignedHeaders=content-type;host, Signature=%s"
                .formatted(secretId, credentialScope, signature);

        var header = new HashMap<String, String>(8);
        header.put("Host", host);
        header.put("X-TC-Action", xtcAction);
        header.put("X-TC-Version", xtcVersion);
        header.put("X-TC-Service", xtcService);
        header.put("X-TC-Timestamp", this.xtcTimestamp);
        header.put("Content-Type", contentType);
        header.put("Authorization", authorization);

        log.info("============= Ti签名字符串 Authorization =============");
        log.info("Authorization: {} Header: {}", authorization, JSONObject.toJSONString(header));

        return header;
    }

    private String sha256Hex(String s) throws Exception {
        var md = MessageDigest.getInstance("SHA-256");
        var d = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(d).toLowerCase();
    }

    public String sha256Hex(byte[] b) throws Exception {
        var md = MessageDigest.getInstance("SHA-256");
        var d = md.digest(b);
        return HexFormat.of().formatHex(d).toLowerCase();
    }

    public byte[] hmac256(byte[] key, String msg) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, mac.getAlgorithm()));
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }
}
