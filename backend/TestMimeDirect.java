import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.Properties;
import javax.net.ssl.*;

public class TestMimeDirect {
    public static void main(String[] args) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("jyotilakhidhar96@gmail.com"));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress("jyoti.sonani@qualtechedge.com"));
        msg.setSubject("Test With Excel Attachment via Direct SSL");

        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent("<h2>Automated OCR Report</h2><p>Attached is your report.</p>", "text/html");
        multipart.addBodyPart(textPart);

        MimeBodyPart attachPart = new MimeBodyPart();
        attachPart.setContent("Dummy excel data", "text/plain");
        attachPart.setFileName("test.txt");
        multipart.addBodyPart(attachPart);

        msg.setContent(multipart);
        msg.saveChanges();

        System.out.println("Connecting...");
        Socket socket = new Socket("smtp.gmail.com", 587);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        
        reader.readLine();
        writer.write("EHLO localhost\r\n"); writer.flush();
        String line;
        while ((line = reader.readLine()) != null) { if (line.startsWith("250 ")) break; }
        
        writer.write("STARTTLS\r\n"); writer.flush();
        reader.readLine();
        
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
            }
        }, null);
        SSLSocket ssl = (SSLSocket) sc.getSocketFactory().createSocket(socket, "google.com", 587, true);
        ssl.startHandshake();

        BufferedReader sslReader = new BufferedReader(new InputStreamReader(ssl.getInputStream()));
        BufferedWriter sslWriter = new BufferedWriter(new OutputStreamWriter(ssl.getOutputStream()));

        sslWriter.write("EHLO localhost\r\n"); sslWriter.flush();
        while ((line = sslReader.readLine()) != null) { if (line.startsWith("250 ")) break; }

        sslWriter.write("AUTH LOGIN\r\n"); sslWriter.flush(); sslReader.readLine();
        sslWriter.write(Base64.getEncoder().encodeToString("jyotilakhidhar96@gmail.com".getBytes()) + "\r\n"); sslWriter.flush(); sslReader.readLine();
        sslWriter.write(Base64.getEncoder().encodeToString("niwvyxmtutzxkdsi".getBytes()) + "\r\n"); sslWriter.flush();
        System.out.println("AUTH: " + sslReader.readLine());

        sslWriter.write("MAIL FROM:<jyotilakhidhar96@gmail.com>\r\n"); sslWriter.flush(); sslReader.readLine();
        sslWriter.write("RCPT TO:<jyoti.sonani@qualtechedge.com>\r\n"); sslWriter.flush(); sslReader.readLine();
        sslWriter.write("DATA\r\n"); sslWriter.flush(); sslReader.readLine();

        msg.writeTo(ssl.getOutputStream());
        ssl.getOutputStream().write("\r\n.\r\n".getBytes());
        ssl.getOutputStream().flush();

        System.out.println("DELIVERY: " + sslReader.readLine());
        sslWriter.write("QUIT\r\n"); sslWriter.flush();
        socket.close();
    }
}
