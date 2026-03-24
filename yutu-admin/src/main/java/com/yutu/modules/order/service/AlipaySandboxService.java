package com.yutu.modules.order.service;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.alipay.easysdk.payment.facetoface.models.AlipayTradePrecreateResponse;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.yutu.common.config.AlipayProperties;
import com.yutu.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class AlipaySandboxService {
    private static final Logger log = LoggerFactory.getLogger(AlipaySandboxService.class);

    private final AlipayProperties alipayProperties;

    public AlipaySandboxService(AlipayProperties alipayProperties) {
        this.alipayProperties = alipayProperties;
    }

    public boolean isConfigured() {
        return alipayProperties.isEnabled()
                && hasText(alipayProperties.getAppId())
                && hasText(alipayProperties.getMerchantPrivateKey())
                && hasText(alipayProperties.getAlipayPublicKey())
                && hasText(alipayProperties.getGatewayHost());
    }

    public PrecreateResult preCreate(String subject, String outTradeNo, BigDecimal totalAmount) {
        ensureConfigured();
        try {
            AlipayTradePrecreateResponse response = Factory.Payment.FaceToFace().preCreate(
                    subject,
                    outTradeNo,
                    totalAmount.stripTrailingZeros().toPlainString());
            if (!ResponseChecker.success(response)) {
                throw new BizException(400, "支付服务暂不可用，请稍后重试");
            }

            PrecreateResult result = new PrecreateResult();
            result.setQrCode(response.qrCode);
            result.setQrCodeImage(toQrCodeImage(response.qrCode));
            result.setRawBody(response.httpBody);
            return result;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("failed to create alipay precreate order, outTradeNo={}", outTradeNo, ex);
            throw new BizException(500, "创建支付二维码失败，请稍后重试");
        }
    }

    public QueryResult query(String outTradeNo) {
        ensureConfigured();
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                AlipayTradeQueryResponse response = Factory.Payment.Common().query(outTradeNo);
                QueryResult result = new QueryResult();
                result.setSuccess(ResponseChecker.success(response));
                result.setTradeStatus(response.tradeStatus);
                result.setTradeNo(response.tradeNo);
                result.setBuyerLogonId(response.buyerLogonId);
                result.setRawBody(response.body);
                return result;
            } catch (Exception ex) {
                log.warn("failed to query alipay trade, outTradeNo={}, attempt={}", outTradeNo, attempt, ex);
                if (attempt < 2) {
                    sleepQuietly(250L);
                }
            }
        }
        throw new BizException(500, "查询支付状态失败，请稍后重试");
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new BizException(400, "支付服务暂不可用，请联系管理员");
        }
        Factory.setOptions(buildConfig());
    }

    private Config buildConfig() {
        Config config = new Config();
        config.protocol = hasText(alipayProperties.getProtocol()) ? alipayProperties.getProtocol() : "https";
        config.gatewayHost = normalizeGatewayHost(alipayProperties.getGatewayHost());
        config.signType = "RSA2";
        config.appId = alipayProperties.getAppId();
        config.merchantPrivateKey = alipayProperties.getMerchantPrivateKey();
        config.alipayPublicKey = alipayProperties.getAlipayPublicKey();
        return config;
    }

    private String normalizeGatewayHost(String gatewayHost) {
        if (!hasText(gatewayHost)) {
            return "";
        }
        String normalized = gatewayHost.trim();
        normalized = normalized.replaceFirst("^https?://", "");
        normalized = normalized.replaceAll("/+$", "");
        normalized = normalized.replaceFirst("/gateway\\.do$", "");
        return normalized;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private String toQrCodeImage(String qrCode) {
        if (!hasText(qrCode)) {
            return "";
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new MultiFormatWriter().encode(qrCode, BarcodeFormat.QR_CODE, 320, 320, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException ex) {
            throw new BizException(500, "生成支付二维码失败，请稍后重试");
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static class PrecreateResult {
        private String qrCode;
        private String qrCodeImage;
        private String rawBody;

        public String getQrCode() {
            return qrCode;
        }

        public void setQrCode(String qrCode) {
            this.qrCode = qrCode;
        }

        public String getQrCodeImage() {
            return qrCodeImage;
        }

        public void setQrCodeImage(String qrCodeImage) {
            this.qrCodeImage = qrCodeImage;
        }

        public String getRawBody() {
            return rawBody;
        }

        public void setRawBody(String rawBody) {
            this.rawBody = rawBody;
        }
    }

    public static class QueryResult {
        private boolean success;
        private String tradeStatus;
        private String tradeNo;
        private String buyerLogonId;
        private String rawBody;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getTradeStatus() {
            return tradeStatus;
        }

        public void setTradeStatus(String tradeStatus) {
            this.tradeStatus = tradeStatus;
        }

        public String getTradeNo() {
            return tradeNo;
        }

        public void setTradeNo(String tradeNo) {
            this.tradeNo = tradeNo;
        }

        public String getBuyerLogonId() {
            return buyerLogonId;
        }

        public void setBuyerLogonId(String buyerLogonId) {
            this.buyerLogonId = buyerLogonId;
        }

        public String getRawBody() {
            return rawBody;
        }

        public void setRawBody(String rawBody) {
            this.rawBody = rawBody;
        }
    }
}
