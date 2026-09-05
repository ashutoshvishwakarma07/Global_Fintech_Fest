import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class MailTest {
    public static void main(String[] args) {
        String username = "nonisonani16@gmail.com";
        String password = "Jyotilsonani16@";
        
        System.out.println("Testing Port 587 STARTTLS with TLS 1.2 / 1.3...");
        testPort587(username, password);
        
        System.out.println("\nTesting Port 465 SSL...");
        testPort465(username, password);
    }
    
    static void testPort587(String user, String pass) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        
        trySession(props, user, pass);
    }
    
    static void testPort465(String user, String pass) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        
        trySession(props, user, pass);
    }
    
    static void trySession(Properties props, String user, String pass) {
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
        session.setDebug(true);
        try {
            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", user, pass);
            System.out.println(">>> SUCCESSFUL CONNECT! <<<");
            transport.close();
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
