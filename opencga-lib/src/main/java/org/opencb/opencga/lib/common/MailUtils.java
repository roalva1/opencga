package org.opencb.opencga.lib.common;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailUtils {

    public static void sendResetPasswordMail(String to, String newPassword) {
//        sendMail("correo.cipf.es", to, "babelomics@cipf.es", "Genomic cloud storage analysis password reset",
//                message.toString());

        Properties accountProperties;
        accountProperties = Config.getAccountProperties();
        String script = accountProperties.getProperty("RESET.PASSWORD.SCRIPT", null);
        if (script != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(script);
            sb.append(" ");
            sb.append(to);
            sb.append(" ");
            sb.append(newPassword);
            sendSystemMail(sb.toString());
        } else {
            System.out.println("RESET.PASSWORD.SCRIPT is not set in account.properties");
        }
    }

    public static void sendMail(String smtpServer, String to, String from, String subject, String body) {
        try {
            Properties props = System.getProperties();
            // -- Attaching to default Session, or we could start a new one --
            props.put("mail.smtp.host", smtpServer);
            Session session = Session.getDefaultInstance(props, null);
            // -- Create a new message --
            // Message msg = new javax.mail.Message(session);
            Message msg = new MimeMessage(session);
            // -- Set the FROM and TO fields --
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            // -- We could include CC recipients too --
            // if (cc != null)
            // msg.setRecipients(Message.RecipientType.CC
            // ,InternetAddress.parse(cc, false));
            // -- Set the subject and body text --
            msg.setSubject(subject);
            msg.setText(body);
            // -- Set some other header information --
            msg.setHeader("X-Mailer", "LOTONtechEmail");
            msg.setSentDate(new Date());
            // -- Send the message --
            Transport.send(msg);
            System.out.println("Message sent OK.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void sendSystemMail(String command) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(command);
//        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
