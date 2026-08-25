package cn.utopiabin.cloud.platform.spi.sms;

/** 厂商短信适配器 SPI；每个厂商实现一个 Sender Bean。 */
public interface SmsSender {
    String provider();
    SmsSendResult send(SmsSendCommand command);
}
