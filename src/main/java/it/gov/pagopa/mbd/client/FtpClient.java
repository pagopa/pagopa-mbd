package it.gov.pagopa.mbd.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.PrintWriter;

@Data
@RequiredArgsConstructor
public class FtpClient {

    @Value("client.ftp.server")
    private String server;
    @Value("client.ftp.port")
    private int port;
    @Value("client.ftp.user")
    private String user;
    @Value("client.ftp.password")
    private String password;

    private FTPClient ftp;

    // constructor

    public void open() throws IOException {
        ftp = new FTPClient();

        ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

        ftp.connect(server, port);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new IOException("Exception in connecting to FTP Server");
        }

        ftp.login(user, password);
    }

    public void close() throws IOException {
        ftp.disconnect();
    }
}
