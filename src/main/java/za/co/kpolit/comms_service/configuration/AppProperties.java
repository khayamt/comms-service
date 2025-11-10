package za.co.kpolit.comms_service.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String secret;
    private String zapierUrl;
    private boolean verifySignature = true;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public String getZapierUrl() { return zapierUrl; }
    public void setZapierUrl(String zapierUrl) { this.zapierUrl = zapierUrl; }

    public boolean isVerifySignature() { return verifySignature; }
    public void setVerifySignature(boolean verifySignature) { this.verifySignature = verifySignature; }
}