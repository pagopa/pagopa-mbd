package it.gov.pagopa.mbd.config.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;

@Configuration
@ConditionalOnProperty(prefix = "client.ftp", name = "enabled")
public class FTPConfig {

    @Value("${client.ftp.host}")
    private String host;

    @Value("${client.ftp.port}")
    private Integer port;

    @Value("${client.ftp.username}")
    private String username;

    @Value("${client.ftp.password}")
    private String password;

    @Bean
    public DefaultFtpSessionFactory sf() {
        DefaultFtpSessionFactory sf = new DefaultFtpSessionFactory();
        sf.setHost(host);
        sf.setPort(port);
        sf.setUsername(username);
        sf.setPassword(password);
        return sf;
    }
}
