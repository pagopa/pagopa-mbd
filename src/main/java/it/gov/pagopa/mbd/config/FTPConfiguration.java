package it.gov.pagopa.mbd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;

@Configuration
public class FTPConfiguration {

    @Bean
    public DefaultFtpSessionFactory sf() {
        DefaultFtpSessionFactory sf = new DefaultFtpSessionFactory();
        sf.setHost("");
        sf.setPort(1000);
        sf.setUsername("");
        sf.setPassword("");
        return sf;
    }
}
