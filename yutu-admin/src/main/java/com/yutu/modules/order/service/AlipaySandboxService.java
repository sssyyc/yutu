package com.yutu.modules.order.service;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.alipay.easysdk.payment.common.models.AlipayTradeRefundResponse;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlipaySandboxService {
    private static final Logger log = LoggerFactory.getLogger(AlipaySandboxService.class);

    private final AlipayProperties alipayProperties;

    public AlipaySandboxService(AlipayProperties alipayProperties) {
        this.alipayProperties = alipayProperties;
    }

    public boolean isConfigured() {
        return missingConfigItems().isEmpty();
    }

    public PrecreateResult preCreate(String subject, String outTradeNo, BigDecimal totalAmount) {
        ensureConfigured();
        try {
            AlipayTradePrecreateResponse response = Factory.Payment.FaceToFace().preCreate(
                    subject,
                    outTradeNo,
                    totalAmount.stripTrailingZeros().toPlainString());
            if (!ResponseChecker.success(response) || !hasText(response.qrCode)) {
                String message = hasText(response.subMsg) ? response.subMsg : response.msg;
                throw new BizException(400, hasText(message) ? message : "Alipay sandbox payment is unavailable");
            }

            PrecreateResult result = new PrecreateResult();
            result.setQrCode(response.qrCode);
            result.setQrCodeImage(toQrCodeImage(response.qrCode));
            result.setRawBody(response.httpBody);
            return result;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("failed to create alipay sandbox precreate order, outTradeNo={}", outTradeNo, ex);
            throw new BizException(500, "Failed to create Alipay sandbox QR code, please try again later");
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
                log.warn("failed to query alipay sandbox trade, outTradeNo={}, attempt={}", outTradeNo, attempt, ex);
                if (attempt < 2) {
                    sleepQuietly(250L);
                }
            }
        }
        throw new BizException(500, "Failed to query Alipay sandbox payment status, please try again later");
    }

    public RefundResult refund(String outTradeNo, BigDecimal refundAmount) {
        ensureConfigured();
        try {
            AlipayTradeRefundResponse response = Factory.Payment.Common().refund(
                    outTradeNo,
                    refundAmount.stripTrailingZeros().toPlainString());
            if (!ResponseChecker.success(response)) {
                String message = hasText(response.subMsg) ? response.subMsg : response.msg;
                throw new BizException(400, hasText(message) ? message : "Alipay sandbox refund failed");
            }

            RefundResult result = new RefundResult();
            result.setTradeNo(response.tradeNo);
            result.setOutTradeNo(response.outTradeNo);
            result.setBuyerLogonId(response.buyerLogonId);
            result.setRefundFee(response.refundFee);
            result.setFundChange(response.fundChange);
            result.setRawBody(response.httpBody);
            return result;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("failed to execute alipay sandbox refund, outTradeNo={}, refundAmount={}", outTradeNo, refundAmount, ex);
            throw new BizException(500, "Failed to execute Alipay sandbox refund, please try again later");
        }
    }

    private void ensureConfigured() {
        List<String> missingItems = missingConfigItems();
        if (!missingItems.isEmpty()) {
            throw new BizException(400, "Alipay sandbox is not configured: " + String.join(", ", missingItems));
        }
        Factory.setOptions(buildConfig());
    }

    private List<String> missingConfigItems() {
        List<String> missingItems = new ArrayList<>();
        if (!alipayProperties.isEnabled()) {
            missingItems.add("ALIPAY_ENABLED=true");
        }
        if (!hasText(alipayProperties.getGatewayHost())) {
            missingItems.add("ALIPAY_GATEWAY_HOST");
        }
        if (!hasText(alipayProperties.getAppId())) {
            missingItems.add("ALIPAY_APP_ID");
        }
        if (!hasText(alipayProperties.getMerchantPrivateKey())) {
            missingItems.add("ALIPAY_MERCHANT_PRIVATE_KEY");
        }
        if (!hasText(alipayProperties.getAlipayPublicKey())) {
            missingItems.add("ALIPAY_PUBLIC_KEY");
        }
        return missingItems;
    }

    private Config buildConfig() {
        Config config = new Config();
        config.protocol = hasText(alipayProperties.getProtocol()) ? alipayProperties.getProtocol() : "https";
        config.gatewayHost = normalizeGatewayHost(alipayProperties.getGatewayHost());
        config.signType = "RSA2";
        config.appId = alipayProperties.getAppId();
        config.merchantPrivateKey = normalizeKey(alipayProperties.getMerchantPrivateKey());
        config.alipayPublicKey = normalizeKey(alipayProperties.getAlipayPublicKey());
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

    private String normalizeKey(String key) {
        return hasText(key) ? key.trim().replace("\\n", "\n") : "";
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private String toQrCodeImage(String qrCode) {
        if (!hasText(qrCode)) {
            return "";
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new MultiFormatWriter().encode(qrCode, BarcodeFormat.QR_CODE, 320, 320, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException ex) {
            throw new BizException(500, "Failed to render Alipay sandbox QR code");
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

    public static class RefundResult {
        private String tradeNo;
        private String outTradeNo;
        private String buyerLogonId;
        private String refundFee;
        private String fundChange;
        private String rawBody;

        public String getTradeNo() {
            return tradeNo;
        }

        public void setTradeNo(String tradeNo) {
            this.tradeNo = tradeNo;
        }

        public String getOutTradeNo() {
            return outTradeNo;
        }

        public void setOutTradeNo(String outTradeNo) {
            this.outTradeNo = outTradeNo;
        }

        public String getBuyerLogonId() {
            return buyerLogonId;
        }

        public void setBuyerLogonId(String buyerLogonId) {
            this.buyerLogonId = buyerLogonId;
        }

        public String getRefundFee() {
            return refundFee;
        }

        public void setRefundFee(String refundFee) {
            this.refundFee = refundFee;
        }

        public String getFundChange() {
            return fundChange;
        }

        public void setFundChange(String fundChange) {
            this.fundChange = fundChange;
        }

        public String getRawBody() {
            return rawBody;
        }

        public void setRawBody(String rawBody) {
            this.rawBody = rawBody;
        }
    }
}
