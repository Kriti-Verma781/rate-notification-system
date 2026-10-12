package com.ncs.ratenotif.component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;

public class MailUtils {
	
	 public static String extractBody(Part part) throws MessagingException, IOException {
	        if (part.isMimeType("text/html")) {
	            return (String) part.getContent();
	        } else if (part.isMimeType("text/plain")) {
	            return ((String) part.getContent()).replace("\n", "<br>");
	        } else if (part.isMimeType("multipart/*")) {
	            MimeMultipart mp = (MimeMultipart) part.getContent();
	            String plain = null;
	            for (int i = 0; i < mp.getCount(); i++) {
	                BodyPart bp = mp.getBodyPart(i);
	                if (bp.isMimeType("text/html")) {
	                    return (String) bp.getContent();
	                } else if (bp.isMimeType("text/plain") && plain == null) {
	                    plain = ((String) bp.getContent()).replace("\n", "<br>");
	                } else {
	                    String nested = extractBody(bp);
	                    if (nested != null && !nested.isEmpty()) return nested;
	                }
	            }
	            return plain == null ? "" : plain;
	        }
	        return "";
	    }

	    /**
	     * Collect attachment file names (does not store attachments).
	     */
	    public static List<String> extractAttachmentNames(Message message) throws MessagingException, IOException {
	        List<String> result = new ArrayList<>();
	        if (message.isMimeType("multipart/*")) {
	            MimeMultipart mp = (MimeMultipart) message.getContent();
	            for (int i = 0; i < mp.getCount(); i++) {
	                BodyPart bp = mp.getBodyPart(i);
	                String disp = bp.getDisposition();
	                String fname = bp.getFileName();
	                if (fname != null) {
	                    result.add(fname);
	                } else if (disp != null && Part.ATTACHMENT.equalsIgnoreCase(disp) && fname == null) {
	                    result.add("attachment-" + i);
	                }
	            }
	        }
	        return result;
	    }
	    
	    public static Map<String, byte[]> extractAttachments(Message message) {

	        Map<String, byte[]> attachmentMap = new HashMap<>();

	        try {
	            Object content = message.getContent();

	            if (content instanceof Multipart multipart) {
	                for (int i = 0; i < multipart.getCount(); i++) {
	                    extractFromPart(multipart.getBodyPart(i), attachmentMap);
	                }
	            }
	        } catch (Exception e) {
	            throw new RuntimeException("Failed to read attachments", e);
	        }

	        return attachmentMap;
	    }

	    private static void extractFromPart(Part part, Map<String, byte[]> map)
	            throws Exception {

	        if (part.isMimeType("multipart/*")) {
	            Multipart mp = (Multipart) part.getContent();
	            for (int i = 0; i < mp.getCount(); i++) {
	                extractFromPart(mp.getBodyPart(i), map);
	            }
	            return;
	        }

	        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
	                || part.getFileName() != null) {

	            String fileName = MimeUtility.decodeText(part.getFileName());

	            try (InputStream is = part.getInputStream()) {
	                map.put(fileName, is.readAllBytes());
	            }
	        }
	    }

	    // 👇 keep your existing method
	    public static String extractBody(Message message) throws Exception {
	        Object content = message.getContent();
	        if (content instanceof String) return (String) content;

	        if (content instanceof Multipart mp) {
	            for (int i = 0; i < mp.getCount(); i++) {
	                BodyPart bp = mp.getBodyPart(i);
	                if (bp.isMimeType("text/html")) return (String) bp.getContent();
	                if (bp.isMimeType("text/plain")) return (String) bp.getContent();
	            }
	        }
	        return "";
	    }
	    
	    
}
