import java.io.*;
import java.net.Socket;
import java.util.Base64;
import javax.net.ssl.*;

public class TestSmtpAuth {
    public static void main(String[] args) throws Exception {
        System.out.println("Connecting to smtp.bizmail.yahoo.com:587...");
        Socket socket = new Socket("smtp.bizmail.yahoo.com", 587);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
        System.out.println("S: " + reader.readLine());
        writer.write("EHLO localhost\r\n");
        writer.flush();
        
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("250 ")) break;
        }
        
        writer.write("STARTTLS\r\n");
        writer.flush();
        System.out.println("S: " + reader.readLine());
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            }
        }, null);
        
        SSLSocketFactory factory = sslContext.getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, "smtp.bizmail.yahoo.com", 587, true);
        sslSocket.startHandshake();
        System.out.println("Handshake complete!");
        
        BufferedReader sslReader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
        BufferedWriter sslWriter = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
        
        sslWriter.write("EHLO localhost\r\n");
        sslWriter.flush();
        while ((line = sslReader.readLine()) != null) {
            if (line.startsWith("250 ")) break;
        }
        
        sslWriter.write("AUTH LOGIN\r\n");
        sslWriter.flush();
        sslReader.readLine();
        
        sslWriter.write(Base64.getEncoder().encodeToString("alert@qualtechedge.com".getBytes()) + "\r\n");
        sslWriter.flush();
        sslReader.readLine();
        
        sslWriter.write(Base64.getEncoder().encodeToString("wgcdoupsprrenrjg".getBytes()) + "\r\n");
        sslWriter.flush();
        String authResp = sslReader.readLine();
        System.out.println("PASS: " + authResp);
        
        // Send a test mail
        sslWriter.write("MAIL FROM:<alert@qualtechedge.com>\r\n");
        sslWriter.flush();
        System.out.println("MAIL FROM: " + sslReader.readLine());
        
        // Production:
        String[] recipients = {
            "aksh.sinha@qualtechedge.com", "manish.kankani@qualtechedge.com", "ashish.srivastava@qualtechedge.com",
            "naveen.kumar1@qualtechedge.com", "amit.sethia@qualtechedge.com"
            // Test mode: "jyoti.sonani@qualtechedge.com", "ashutosh.vishwakarma@qualtechedge.com"
        };
        for (String rcpt : recipients) {
            sslWriter.write("RCPT TO:<" + rcpt + ">\r\n");
            sslWriter.flush();
            System.out.println("RCPT TO: " + sslReader.readLine());
        }
        
        sslWriter.write("DATA\r\n");
        sslWriter.flush();
        System.out.println("DATA: " + sslReader.readLine());
        
        sslWriter.write("From: alert@qualtechedge.com\r\n");
        sslWriter.write("To: aksh.sinha@qualtechedge.com, manish.kankani@qualtechedge.com, ashish.srivastava@qualtechedge.com\r\n");
        sslWriter.write("Cc: naveen.kumar1@qualtechedge.com, amit.sethia@qualtechedge.com\r\n");
        sslWriter.write("Subject: GFF OCR Automated Test Email\r\n");
        sslWriter.write("\r\n");
        sslWriter.write("Hello, this is an automated test from GFF backend.\r\n");
        sslWriter.write(".\r\n");
        sslWriter.flush();
        System.out.println("SEND RESULT: " + sslReader.readLine());
        
        sslWriter.write("QUIT\r\n");
        sslWriter.flush();
        socket.close();
    }
}
