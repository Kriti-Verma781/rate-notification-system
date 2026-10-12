package com.ncs.ratenotif.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.core.io.*;

import com.ncs.ratenotif.component.EmailCache;
import com.ncs.ratenotif.component.MailUtils;
import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.ClientRateSheetEntity;
import com.ncs.ratenotif.entity.CommonTable;
import com.ncs.ratenotif.entity.NocRateUploadedStatus;
import com.ncs.ratenotif.entity.PromotionClientEntity;
import com.ncs.ratenotif.entity.RateFlowType;
import com.ncs.ratenotif.entity.Trunk;
import com.ncs.ratenotif.repository.ClientRateSheetRepository;
import com.ncs.ratenotif.repository.ClientRepository;
import com.ncs.ratenotif.repository.CommonEntityRepository;
import com.ncs.ratenotif.repository.EmailMessageRepository;
import com.ncs.ratenotif.repository.NocRateUploadedStatusRepository;
import com.ncs.ratenotif.repository.PromotionRepository;
import com.ncs.ratenotif.repository.TrunkRepository;
import com.ncs.ratenotif.request.EmailAddresses;
import com.ncs.ratenotif.request.ForwardNocRequest;
import com.ncs.ratenotif.request.PromotionMailRequest;
import com.ncs.ratenotif.request.PromotionRateDTO;
import com.ncs.ratenotif.request.RateMailMatchRowDTO;
import com.ncs.ratenotif.request.RateSendRequest;
import com.ncs.ratenotif.request.ReplyMailRequest;
import com.ncs.ratenotif.request.RoutingRateDTO;
import com.ncs.ratenotif.request.SendMailRequest;
import com.ncs.ratenotif.response.AttachmentResponse;
import com.ncs.ratenotif.response.EmailPreviewResopnse;
import com.ncs.ratenotif.response.EmailReceiveResponse;
import com.ncs.ratenotif.response.PromotionMailResponseDTO;
import com.ncs.ratenotif.response.RateMailRowDTO;
import com.ncs.ratenotif.service.MailService;
import com.ncs.ratenotif.service.RateCreateService;

import jakarta.annotation.PostConstruct;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.*;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.Flags;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.transaction.Transactional;
import org.springframework.http.MediaType;

@Service
public class EmailServiceImpl implements MailService {

	private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private TrunkRepository trunkRepository;

	@Autowired
	private ClientRateSheetRepository rateSheetRepository;

	@Autowired
	private CommonEntityRepository templateRepo;

	@Autowired
	private NocRateUploadedStatusRepository nocRateUploadedStatusRepository;

	@Autowired
	private PromotionRepository promotionRepository;

	@Value("${spring.mail.username}")
	private String username;

	@Value("${spring.mail.password}")
	private String appPassword;

	@Value("${app.imap.host}")
	private String imapHost;

	@Value("${app.imap.port}")
	private String imapPort;

	@Value("${app.imap.protocol}")
	private String protocol;

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private RateCreateService rateCreateService;

	@Value("${spring.mail.from}")
	private String fromEmail;

	@Value("${app.mail.folder:INBOX}")
	private String folderName;

	private final EmailCache cache;

	// track last UID seen (string -> long)
	private volatile long lastUID = -1L;

	private volatile boolean isSyncRunning = false;

	@Value("${noc.base-url}")
	private String baseUrl;

	public EmailServiceImpl(EmailCache cache) {
		this.cache = cache;
	}

	@PostConstruct
	public void init() {
		log.info("EmailService initialized for {} @ {}:{}", username, imapHost, imapPort);
	}

	@Override
	@Scheduled(fixedDelayString = "${app.fetch.interval.ms:5000}")
	public void scheduledSync() {

		if (isSyncRunning) {
			log.info("Previous sync still running — skipping...");
			return;
		}

		try {
			isSyncRunning = true;
			doSync();
		} finally {
			isSyncRunning = false;
		}
	}

	@Override
	public List<EmailReceiveResponse> getCachedEmails() {
		return cache.getAllSortedLatestFirst();
	}

	private void doSync() {
		try {
			List<EmailReceiveResponse> newEmails = fetchNewEmails();
			if (newEmails != null && !newEmails.isEmpty()) {
				newEmails.forEach(cache::addIfNotExists);
				log.info("Fetched {} emails", newEmails.size());
			}
		} catch (Exception ex) {
			log.error("Error during sync", ex);
		}
	}

	private List<EmailReceiveResponse> fetchNewEmails() throws Exception {

	    Properties props = new Properties();
	    props.put("mail.store.protocol", protocol);
	    props.put("mail.imap.host", imapHost);
	    props.put("mail.imap.port", imapPort);
	    props.put("mail.imap.ssl.enable", "true");

	    Session session = Session.getInstance(props);

	    try (Store store = session.getStore(protocol)) {

	        store.connect(imapHost, username, appPassword);

	        try (Folder folder = store.getFolder(folderName)) {

	            System.out.println(folderName);
	            System.out.println(folder.exists());

	            folder.open(Folder.READ_WRITE);

	            List<EmailReceiveResponse> result = new ArrayList<>();

	            Calendar calendar = Calendar.getInstance();
	            calendar.add(Calendar.DAY_OF_MONTH, -9);

	            Date startDate = calendar.getTime();

	            ReceivedDateTerm dateTerm =
	                    new ReceivedDateTerm(ComparisonTerm.GE, startDate);

	            Message[] messages = folder.search(dateTerm);

	            log.info("Last 10 days mails count={}", messages.length);

	            if (folder instanceof UIDFolder) {

	                UIDFolder uf = (UIDFolder) folder;

	                for (Message msg : messages) {

	                    long uid = uf.getUID(msg);

	                    EmailReceiveResponse res =
	                            convertMessageToResponse(uf, msg, uid);

	                    if (res != null) {

	                        cache.addIfNotExists(res);

	                        result.add(res);

	                        processIncomingMail(res);
	                    }

	                    lastUID = uid;
	                }

	            } else {

	                for (Message message : messages) {

	                    EmailReceiveResponse r =
	                            convertMessageToResponse(null, message, null);

	                    if (r != null) {
	                        result.add(r);
	                    }
	                }
	            }

	            result.sort(
	                    Comparator.comparing(
	                            EmailReceiveResponse::getSentDate,
	                            Comparator.nullsLast(Comparator.reverseOrder())
	                    )
	            );

	            return result;
	        }
	    }
	}

	private EmailReceiveResponse convertMessageToResponse(UIDFolder uidFolder, Message m, Long uid) {

		try {
			String uidStr = uid != null ? String.valueOf(uid) : null;

			String from = Arrays.stream(Optional.ofNullable(m.getFrom()).orElse(new Address[0])).map(Address::toString)
					.collect(Collectors.joining(", "));

			List<String> to = parseAddresses(m.getRecipients(Message.RecipientType.TO));
			List<String> cc = parseAddresses(m.getRecipients(Message.RecipientType.CC));

			String subject = Optional.ofNullable(m.getSubject()).orElse("No Subject");
			Date sentDate = m.getSentDate();
			String body = MailUtils.extractBody(m);

			Map<String, byte[]> attachmentData = MailUtils.extractAttachments(m);

			EmailReceiveResponse resp = new EmailReceiveResponse(uidStr, from, to, cc, subject, sentDate, body,
					new ArrayList<>(attachmentData.keySet()), attachmentData, null, null);

			String[] msgId = m.getHeader("Message-ID");
			if (msgId != null && msgId.length > 0)
				resp.setMessageId(msgId[0]);

			String[] irt = m.getHeader("In-Reply-To");
			if (irt != null && irt.length > 0)
				resp.setInReplyTo(irt[0]);

			return resp;

		} catch (Exception ex) {
			log.warn("Failed to convert message", ex);
			return null;
		}
	}

	private List<String> parseAddresses(Address[] addresses) {
		if (addresses == null)
			return Collections.emptyList();
		return Arrays.stream(addresses).map(Address::toString).collect(Collectors.toList());
	}

	private String getTextFromMessage(Message message) throws Exception {

		if (message.isMimeType("text/plain")) {
			return message.getContent().toString();
		}

		if (message.isMimeType("text/html")) {
			return message.getContent().toString();
		}

		if (message.isMimeType("multipart/*")) {
			MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
			return getTextFromMimeMultipart(mimeMultipart);
		}

		return "";
	}

	private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < mimeMultipart.getCount(); i++) {

			BodyPart bp = mimeMultipart.getBodyPart(i);

			if (bp.isMimeType("text/html")) {
				return bp.getContent().toString();
			}

			if (bp.isMimeType("text/plain")) {
				result.append(bp.getContent());
			}

			if (bp.getContent() instanceof MimeMultipart) {
				result.append(getTextFromMimeMultipart((MimeMultipart) bp.getContent()));
			}
		}

		return result.toString();
	}

	@Override
	public String sendEmail(SendMailRequest req) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true);

			helper.setTo(req.getToMail());

			if (req.getCcMail() != null && !req.getCcMail().isEmpty()) {
				helper.setCc(InternetAddress.parse(req.getCcMail()));
			}

			helper.setSubject(req.getSubject());
			helper.setText(req.getBody(), true);

			helper.setFrom(fromEmail);

			mailSender.send(message);

			return "Mail Sent Successfully";

		} catch (MessagingException e) {
			throw new RuntimeException("Failed to send email: " + e.getMessage());
		}
	}

//	@Override
//	public Object sendRateEmail(RateSendRequest req) throws Exception {
//
//		ClientEntity client = clientRepository.findByName(req.getClientName());
//		if (client == null)
//			throw new RuntimeException("Client not found: " + req.getClientName());
//
//		List<String> toEmails = client.getPrimaryEmail();
//		if (toEmails == null || toEmails.isEmpty())
//			throw new RuntimeException("No primary emails configured for client: " + req.getClientName());
//
//		List<String> ccEmails = client.getCcEmails();
//		if (ccEmails == null)
//			ccEmails = new ArrayList<>();
//
//		Set<String> uniqueTo = new LinkedHashSet<>();
//		for (String mail : toEmails) {
//			if (mail != null && !mail.trim().isEmpty())
//				uniqueTo.add(mail.trim());
//		}
//
//		Set<String> uniqueCc = new LinkedHashSet<>();
//		for (String mail : ccEmails) {
//			if (mail != null && !mail.trim().isEmpty() && !uniqueTo.contains(mail.trim())) {
//				uniqueCc.add(mail.trim());
//			}
//		}
//
//		Trunk trunk = trunkRepository.findByName(req.getTrunkName());
//		if (trunk == null)
//			throw new RuntimeException("Trunk not found: " + req.getTrunkName());
//
//		List<ClientRateSheetEntity> rateSheetList = rateSheetRepository.findByClientIdAndTrunkId(client.getId(),
//				trunk.getId());
//		if (rateSheetList == null || rateSheetList.isEmpty())
//			throw new RuntimeException("Rate sheet not found for client '" + req.getClientName() + "' & trunk '"
//					+ req.getTrunkName() + "'");
//
//		ClientRateSheetEntity rateSheet = rateSheetList.get(0);
//
//		CommonTable template = templateRepo.findByType("RATE_TEMPLATE")
//				.orElseThrow(() -> new RuntimeException("RATE_TEMPLATE not found in CommonTable"));
//
//		String subject = template.getValue2().replace("{clientName}", req.getClientName()).replace("{trunk}",
//				req.getTrunkName());
//
//		String body = template.getValue1().replace("{clientName}", req.getClientName()).replace("{trunk}",
//				req.getTrunkName());
//
//		MimeMessage message = mailSender.createMimeMessage();
//		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//		helper.setFrom(fromEmail);
//		helper.setTo(uniqueTo.toArray(new String[0]));
//		if (!uniqueCc.isEmpty())
//			helper.setCc(uniqueCc.toArray(new String[0]));
//
//		helper.setSubject(subject);
//		helper.setText(body, true);
//
//		Resource logo = new ClassPathResource("META-INF/logo-light.png");
//		if (logo.exists())
//			helper.addInline("nt_logo", logo);
//
//		Resource linkedin = new ClassPathResource("META-INF/LinkedIn.png");
//		if (linkedin.exists())
//			helper.addInline("linkedin_logo", linkedin);
//
//		Resource banner = new ClassPathResource("META-INF/signImage.png");
//		if (banner.exists())
//			helper.addInline("banner", banner);
//
//		helper.addAttachment(rateSheet.getFileName(), new ByteArrayResource(rateSheet.getFileContent()));
//
//		mailSender.send(message);
//
//		System.out.println("TO Emails: " + uniqueTo);
//		System.out.println("CC Emails: " + uniqueCc);
//		System.out.println("From: " + fromEmail);
//		System.out.println("Subject: " + subject);
//		System.out.println("Attachment: " + rateSheet.getFileName());
//
//		return "Rate email sent successfully to all recipients for client " + req.getClientName() + " ("
//				+ req.getTrunkName() + ")";
//	}

	@Override
	public Object previewRateEmail(RateSendRequest req) {

		ClientEntity client = clientRepository.findByName(req.getClientName());
		if (client == null)
			throw new RuntimeException("Client not found: " + req.getClientName());

		List<String> toEmails = client.getPrimaryEmail();
		if (toEmails == null || toEmails.isEmpty())
			throw new RuntimeException("No primary emails for client: " + req.getClientName());

		List<String> ccEmails = client.getCcEmails();
		if (ccEmails == null)
			ccEmails = new ArrayList<>();

		// remove duplicates
		Set<String> uniqueTo = new LinkedHashSet<>();
		toEmails.forEach(e -> {
			if (e != null && !e.trim().isEmpty())
				uniqueTo.add(e.trim());
		});

		Set<String> uniqueCc = new LinkedHashSet<>();
		ccEmails.forEach(e -> {
			if (e != null && !e.trim().isEmpty() && !uniqueTo.contains(e.trim()))
				uniqueCc.add(e.trim());
		});

		Trunk trunk = trunkRepository.findByName(req.getTrunkName());
		if (trunk == null)
			throw new RuntimeException("Trunk not found: " + req.getTrunkName());

		List<ClientRateSheetEntity> rateSheetList = rateSheetRepository.findByClientIdAndTrunkId(client.getId(),
				trunk.getId());

		if (rateSheetList == null || rateSheetList.isEmpty())
			throw new RuntimeException("Rate sheet not found");

		ClientRateSheetEntity rateSheet = rateSheetList.get(0);

		CommonTable template = templateRepo.findByType("RATE_TEMPLATE")
				.orElseThrow(() -> new RuntimeException("RATE_TEMPLATE not found"));

		String subject = template.getValue2().replace("{clientName}", req.getClientName()).replace("{trunk}",
				req.getTrunkName());

		String body = template.getValue1().replace("{clientName}", req.getClientName()).replace("{trunk}",
				req.getTrunkName());

		return new EmailPreviewResopnse(fromEmail, new ArrayList<>(uniqueTo), new ArrayList<>(uniqueCc), subject, body,
				rateSheet.getFileName());
	}

//	@Override
//	public Object forwardRateToNOC(RateSendRequest req) throws Exception {
//
//		ClientEntity client = clientRepository.findByName(req.getClientName());
//		if (client == null)
//			throw new RuntimeException("Client not found");
//
//		Trunk trunk = trunkRepository.findByName(req.getTrunkName());
//		if (trunk == null)
//			throw new RuntimeException("Trunk not found");
//
//		ClientRateSheetEntity sheetEntity = rateSheetRepository.findByClientAndTrunk(client, trunk)
//				.orElseThrow(() -> new RuntimeException("Rate sheet not found"));
//
//		// 🔥 Fetch data from Excel
//		List<RateMailRowDTO> rateChanges = rateCreateService.fetchRateChangesFromExcel(sheetEntity.getFileContent());
//
//		if (rateChanges.isEmpty())
//			return "No rate changes found. Email not sent.";
//
//		CommonTable template = templateRepo.findByType("RATE_FORWARD_TEMPLATE")
//				.orElseThrow(() -> new RuntimeException("RATE_TEMPLATE not found"));
//
//		String subject = template.getValue2().replace("{clientName}", req.getClientName()).replace("{trunk}",
//				req.getTrunkName());
//
//		String baseBody = template.getValue1().replace("{clientName}", req.getClientName()).replace("{trunk}",
//				req.getTrunkName());
//
//		String tableHtml = rateCreateService.buildRateChangeTable(rateChanges);
//
////	    String uploadUrl =
////	    		baseUrl + "/mail/uploaded?clientName=" + req.getClientName()
////	                    + "&trunkName=" + req.getTrunkName();
////
////	    String notUploadUrl =
////	    		baseUrl + "/mail/not-uploaded?clientName=" + req.getClientName()
////	                    + "&trunkName=" + req.getTrunkName();
//
//		// 🔘 BUTTON HTML
////	    String actionButtons =
////	            "<br/><br/>" +
////	            "<b>Upload Status:</b><br/><br/>" +
////	            "<a href='" + uploadUrl + "' " +
////	            "style='padding:10px 16px;background:#28a745;color:#ffffff;" +
////	            "text-decoration:none;border-radius:4px;font-weight:bold;'>UPLOAD SUCCESS</a>" +
////	            "&nbsp;&nbsp;&nbsp;" +
////	            "<a href='" + notUploadUrl + "' " +
////	            "style='padding:10px 16px;background:#dc3545;color:#ffffff;" +
////	            "text-decoration:none;border-radius:4px;font-weight:bold;'>NOT UPLOADED</a>";
//
//		// 📧 Final mail body
//		String finalBody = baseBody.replace("Please upload the rates.",
//				"Please upload the rates." + tableHtml + nocButtons(req));
//
//		MimeMessage message = mailSender.createMimeMessage();
//		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//		helper.setFrom(fromEmail);
//		helper.setTo("juleesingh@northcorp.tech");
////	    helper.setCc("juleesingh@northcorp.tech");
//
//		helper.setSubject(subject);
//		helper.setText(finalBody, true);
//
//		helper.addAttachment(sheetEntity.getFileName(), new ByteArrayResource(sheetEntity.getFileContent()));
//
//		mailSender.send(message);
//
//		return "Rate forwarded to NOC with upload / not-upload actions";
//	}

//	@Override
//	public Object forwardRateToNOC(RateSendRequest req) throws Exception {
//
//		ClientEntity client = clientRepository.findByName(req.getClientName());
//		if (client == null)
//			throw new RuntimeException("Client not found");
//
//		Trunk trunk = trunkRepository.findByName(req.getTrunkName());
//		if (trunk == null)
//			throw new RuntimeException("Trunk not found");
//
//		ClientRateSheetEntity sheetEntity = rateSheetRepository.findByClientAndTrunk(client, trunk)
//				.orElseThrow(() -> new RuntimeException("Rate sheet not found"));
//
//		// 🔥 Fetch data from Excel
//		List<RateMailRowDTO> rateChanges = rateCreateService.fetchRateChangesFromExcel(sheetEntity.getFileContent());
//
//		if (rateChanges.isEmpty())
//			return "No rate changes found. Email not sent.";
//
//		CommonTable template = templateRepo.findByType("RATE_FORWARD_TEMPLATE")
//				.orElseThrow(() -> new RuntimeException("RATE_TEMPLATE not found"));
//
//		String subject = template.getValue2().replace("{clientName}", req.getClientName()).replace("{trunk}",
//				req.getTrunkName());
//
//		String baseBody = template.getValue1().replace("{clientName}", req.getClientName()).replace("{trunk}",
//				req.getTrunkName());
//
//		String tableHtml = rateCreateService.buildRateChangeTable(rateChanges);
//
//		// Insert table at correct place
//		String finalBody = baseBody.replace("Please upload the rates.", "Please upload the rates." + tableHtml);
//
//		MimeMessage message = mailSender.createMimeMessage();
//		MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
//
//		helper.setFrom(fromEmail);
////	    helper.setTo(client.getPrimaryEmail().toArray(new String[0]));
//		helper.setCc("vibhav123@nuetechstack.net");
//		helper.setTo("juleesingh@northcorp.tech");
//		helper.setSubject(subject);
//		helper.setText(finalBody, true);
//
//		helper.addAttachment(sheetEntity.getFileName(), new ByteArrayResource(sheetEntity.getFileContent()));
//
//		mailSender.send(message);
//
//		return "Updated rate email sent successfully (Excel based)";
//	}

	private boolean excelHasIncrease(byte[] excelBytes) {
		ZipSecureFile.setMinInflateRatio(0.001);
		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {

			XSSFSheet sheet = workbook.getSheetAt(0);

			for (int i = 11; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null)
					continue;

				String status = getCellValue(row.getCell(3)); // STATUS column

				if ("Increase".equalsIgnoreCase(status)) {
					return true;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Error reading excel for increase check", e);
		}
		return false;
	}

	private String getCellValue(Cell cell) {
		if (cell == null)
			return "";
		return switch (cell.getCellType()) {
		case STRING -> cell.getStringCellValue();
		case NUMERIC -> String.valueOf(cell.getNumericCellValue());
		default -> "";
		};
	}

//	@Override
//	public Object sendRateEmail(RateSendRequest req) throws Exception {
//
// 	    ClientEntity client = clientRepository.findByName(req.getClientName());
//	    if (client == null)
//	        throw new RuntimeException("Client not found");
//
//	    Trunk trunk = trunkRepository.findByName(req.getTrunkName());
//	    if (trunk == null)
//	        throw new RuntimeException("Trunk not found");
//
//	    ClientRateSheetEntity sheetEntity =
//	            rateSheetRepository.findByClientAndTrunk(client, trunk)
//	                    .orElseThrow(() -> new RuntimeException("Rate sheet not found"));
//
////	    List<RateMailRowDTO> rateChanges =
////	            rateCreateService.fetchRateChangesFromExcel(sheetEntity.getFileContent());
//
//	    boolean hasIncrease = excelHasIncrease(sheetEntity.getFileContent());
//
//	    sendMailToClient(req, client, trunk, sheetEntity);
//
//	    if (!hasIncrease) {
//	        forwardRateToNOC(req);
//	        return "Rate sent to client and auto-forwarded to NOC (no increase)";
//	    }
//
//	    return "Rate sent to client only (rate increased, NOC skipped)";
//	}

//	@Override
//	public Object sendRateEmail(RateSendRequest req) throws Exception {
//
//		ClientEntity client = clientRepository.findByName(req.getClientName());
//		Trunk trunk = trunkRepository.findByName(req.getTrunkName());
//
//		ClientRateSheetEntity sheetEntity = rateSheetRepository.findByClientAndTrunk(client, trunk)
//				.orElseThrow(() -> new RuntimeException("Rate sheet not found"));
//
//		boolean hasIncrease = excelHasIncrease(sheetEntity.getFileContent());
//
//		sendMailToClient(req, client, trunk, sheetEntity);
//
//		if (!hasIncrease) {
//			forwardRateToNOC(req);
//			forwardRateToRouting(req);
//			return "Rate sent to NOC + Routing (No Increase)";
//		}
//
//		return "Rate sent to Client only (Increase found)";
//	}

	private void sendMailToClient(RateSendRequest req, ClientEntity client, Trunk trunk,
			ClientRateSheetEntity rateSheet) throws Exception {

		CommonTable template = templateRepo.findByType("RATE_TEMPLATE")
				.orElseThrow(() -> new RuntimeException("RATE_TEMPLATE not found"));

//		String prefix = trunk.getPrefix();
		String prefix = readPrefixFromExcel(rateSheet.getFileContent());

		String rateDetails = buildRateChangeTable(rateSheet.getFileContent()) + "<br/>"
				+ "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:15px;\">\r\n"
				+ "				<tr>\r\n" + "				<td style=\"font-size:14px;color:#333333;\">\r\n"
				+ "				Should you have any questions regarding these changes,\r\n"
				+ "				please feel free to reach out to your Account Manager.\r\n"
				+ "				<br/><br/>\r\n"
				+ "				Kindly confirm receipt of this notification by replying to this email with\r\n"
				+ "				<b>\"ACCEPTED\"</b> or by clicking the button below.\r\n" + "				</td>\r\n"
				+ "				</tr>\r\n" + "				</table>"
				+ clientAcceptButton(req.getClientName(), req.getTrunkName());
		System.out.println("Rates Details" + rateDetails);

//		String subject = template.getValue2().replace("{clientName}", req.getClientName()).replace("{trunk}",
//				req.getTrunkName());

		String subject;

		String clientName = req.getClientName();
		String trunkName = req.getTrunkName();

		if ("Citic tel".equalsIgnoreCase(clientName)) {

			subject = "[NSK][NSK_PRM],[A-Z]";

		} else if ("Airtel".equalsIgnoreCase(clientName)) {

			if ("Premium".equalsIgnoreCase(trunkName)) {
				subject = "KUNUEG";
			} else if ("Standard".equalsIgnoreCase(trunkName)) {
				subject = "KUNUES";
			} else {
				subject = template.getValue2().replace("{clientName}", clientName).replace("{trunk}", trunkName);
			}

		} else if ("CMI".equalsIgnoreCase(clientName)) {
			subject = "[SGP_Nuetech-CHN_CMI][PL][USD][Premium],[Full Country]";

		} else {
			subject = template.getValue2().replace("{clientName}", clientName).replace("{trunk}", trunkName);
		}

		String body = buildRateMailBody().replace("{subject}", subject).replace("{trunk}", req.getTrunkName())
				.replace("{prefix}", prefix).replace("{RATES_DETAILS}", rateDetails);

		List<String> toEmails = client.getPrimaryEmail();
		List<String> ccEmails = new ArrayList<>(client.getCcEmails());

		if (client.getAccountManagerEmail() != null) {
			ccEmails.add(client.getAccountManagerEmail());
		}

		NocRateUploadedStatus statusEntity = nocRateUploadedStatusRepository
				.findByClientNameAndTrunkName(req.getClientName(), req.getTrunkName())
				.orElseGet(NocRateUploadedStatus::new);
		statusEntity.setToEmail(toEmails);
		nocRateUploadedStatusRepository.save(statusEntity);
		sendMail(toEmails, ccEmails, subject, body, rateSheet);
	}

	@Override
	public void saveStatus(String clientName, String trunkName, String nocStatus, String routingStatus, String remark) {

		NocRateUploadedStatus entity = nocRateUploadedStatusRepository
				.findByClientNameAndTrunkName(clientName, trunkName).orElseGet(NocRateUploadedStatus::new);
		entity.setClientName(clientName);
		entity.setTrunkName(trunkName);
		entity.setNocStatus(nocStatus);
		entity.setRoutingStatus(routingStatus);
		entity.setRemark(remark);
		entity.setCreatedOn(LocalDateTime.now());

		nocRateUploadedStatusRepository.save(entity);
	}

	private String nocButtons(RateSendRequest req) {
		return """
				    <br/><br/>
				    <a href='%s/mail/uploaded?clientName=%s&trunkName=%s'>UPLOAD SUCCESS</a>
				    &nbsp;&nbsp;
				    <a href='%s/mail/not-uploaded?clientName=%s&trunkName=%s'>NOT UPLOADED</a>
				""".formatted(baseUrl, req.getClientName(), req.getTrunkName(), baseUrl, req.getClientName(),
				req.getTrunkName());
	}

	private String routingButtons(RateSendRequest req) {
		return """
				    <br/><br/>
				    <a href='%s/mail/routing/uploaded?clientName=%s&trunkName=%s'>UPLOAD SUCCESS</a>
				    &nbsp;&nbsp;
				    <a href='%s/mail/routing/not-uploaded?clientName=%s&trunkName=%s'>NOT UPLOADED</a>
				""".formatted(baseUrl, req.getClientName(), req.getTrunkName(), baseUrl, req.getClientName(),
				req.getTrunkName());
	}

//	@Override
//	public Object forwardRateToRouting(RateSendRequest req) throws Exception {
//
//		ClientRateSheetEntity sheet = getSheet(req.getClientName(), req.getTrunkName());
//
//		String tableHtml = buildRateChangeTable(sheet.getFileContent());
//
//		CommonTable template = templateRepo.findByType("ROUTING_TEMPLATE")
//				.orElseThrow(() -> new RuntimeException("ROUTING_TEMPLATE not found"));
//
//		Map<String, String> values = Map.of(
//		        "{vendorName}", req.getVendorName(),
//		        "{trunkName}", req.getTrunkName(),
//		        "{trunk}", req.getTrunk(),
//		        "{cost}", req.getCost()
//		);
//
//		String body = template.getValue1();
//		for (Map.Entry<String, String> e : values.entrySet()) {
//		    body = body.replace(e.getKey(), e.getValue());
//		}
//
//		body += tableHtml + routingButtons(req);
//
//		sendMail(List.of("juleesingh@northcorp.tech"), List.of("juleesingh@northcorp.tech"), template.getValue2(), body, sheet);
//
//		return "Routing mail sent";
//	}

	private ClientRateSheetEntity getSheet(String clientName, String trunkName) {

		ClientEntity client = clientRepository.findByName(clientName);
		if (client == null)
			throw new RuntimeException("Client not found");

		Trunk trunk = trunkRepository.findByName(trunkName);
		if (trunk == null)
			throw new RuntimeException("Trunk not found");

		return rateSheetRepository.findByClientAndTrunk(client, trunk)
				.orElseThrow(() -> new RuntimeException("Rate sheet not found"));
	}

	private String buildRateChangeTable(byte[] excelBytes) {

		List<RateMailRowDTO> rows = rateCreateService.fetchRateChangesFromExcel(excelBytes);

		if (rows == null || rows.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("""
				<table width="100%" cellpadding="8" cellspacing="0"
				       style="border-collapse:collapse; font-size:14px;">

				<tr style="background:#A9A9A9; color:#000080;">
				    <th style="border:1px solid #ddd; text-align:left;">Destination</th>
				    <th style="border:1px solid #ddd; text-align:center;">Billing Cycle</th>
				    <th style="border:1px solid #ddd; text-align:right;">Rate</th>
				    <th style="border:1px solid #ddd; text-align:center;">Status</th>
				    <th style="border:1px solid #ddd; text-align:center;">Effective Date</th>
				</tr>
				""");

		for (RateMailRowDTO row : rows) {
			sb.append("""
					<tr>
					    <td style="border:1px solid #ddd;">%s</td>
					    <td style="border:1px solid #ddd; text-align:center;">%s</td>
					    <td style="border:1px solid #ddd; text-align:right;">%s</td>
					    <td style="border:1px solid #ddd; text-align:center;">%s</td>
					    <td style="border:1px solid #ddd; text-align:center;">%s</td>
					</tr>
					""".formatted(safe(row.getDestination()), safe(row.getBillingCycle()), safe(row.getRate()),
					safe(row.getStatus()), safe(row.getEffectiveDate())));
		}

		sb.append("</table>");

		return sb.toString();
	}
	
	private String buildNOCRateChangeTable(byte[] excelBytes) {

		List<RateMailRowDTO> rows = rateCreateService.fetchRateChangesFromExcel(excelBytes);

		if (rows == null || rows.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("""
				<table width="100%" cellpadding="8" cellspacing="0"
				       style="border-collapse:collapse; font-size:14px;">

				<tr style="background:#A9A9A9; color:#000080;">
				    <th style="border:1px solid #ddd; text-align:left;">Destination</th>
				    <th style="border:1px solid #ddd; text-align:center;">Billing Cycle</th>
				    <th style="border:1px solid #ddd; text-align:Center;">Rate</th>
				    <th style="border:1px solid #ddd; text-align:center;">Status</th>
				    <th style="border:1px solid #ddd; text-align:center;">Effective Date</th>
				    <th style="border:1px solid #ddd; text-align:left;">Remark</th>
				</tr>
				""");

		for (RateMailRowDTO row : rows) {
			sb.append("""
					<tr>
					    <td style="border:1px solid #ddd;">%s</td>
					    <td style="border:1px solid #ddd; text-align:center;">%s</td>
					    <td style="border:1px solid #ddd; text-align:center;">%s</td>
					    <td style="border:1px solid #ddd; text-align:center;">%s</td>
					    <td style="border:1px solid #ddd; text-align:center;">%s</td>
					     <td style="border:1px solid #ddd; text-align:left;">%s</td>
					</tr>
					""".formatted(safe(row.getDestination()), safe(row.getBillingCycle()), safe(row.getRate()),
					safe(row.getStatus()), safe(row.getEffectiveDate()),  safe(row.getRemark())));
		}

		sb.append("</table>");

		return sb.toString();
	}

	private String safe(String val) {
		if (val == null)
			return "";
		return val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&#39;");
	}

	private void sendMail(List<String> toEmails, List<String> ccEmails, String subject, String body,
			ClientRateSheetEntity sheet) throws Exception {

		MimeMessage msg = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

		helper.setFrom(new InternetAddress("rates@nuetechstack.net", "Nuetech Rates"));
		helper.setTo(toEmails.toArray(new String[0]));

		if (ccEmails != null && !ccEmails.isEmpty()) {
			helper.setCc(ccEmails.toArray(new String[0]));
		}

		helper.setSubject(subject);
		helper.setText(body, true);

		helper.addInline("nt_logo", new ClassPathResource("META-INF/logo-light.png"));

		helper.addInline("linkedin_logo", new ClassPathResource("META-INF/LinkedIn.png"));

		helper.addInline("banner", new ClassPathResource("META-INF/signImage.png"));
		
//		helper.addInline("watermark_logo",
//		        new ClassPathResource("META-INF/logo-light.png"));


		if (sheet != null) {
			helper.addAttachment(sheet.getFileName(), new ByteArrayResource(sheet.getFileContent()));
		}

		mailSender.send(msg);
	}

//	@Scheduled(fixedDelay = 10000)
//	public void autoSendToClient() {
//
//		List<NocRateUploadedStatus> list = nocRateUploadedStatusRepository.findAll();
//
//		for (NocRateUploadedStatus s : list) {
//			if (s.getClientSendStatus() != "CLIENT_SENT") {
//
//				if (!"UPLOADED".equalsIgnoreCase(s.getNocStatus()))
//					continue;
//
//				if (!"UPLOADED".equalsIgnoreCase(s.getRoutingStatus()))
//					continue;
//
//				ClientRateSheetEntity sheet = getSheet(s.getClientName(), s.getTrunkName());
//
//				if (excelHasIncrease(sheet.getFileContent()))
//					continue;
//
//				sendFinalClientMail(s.getClientName(), s.getTrunkName());
//
//				s.setClientSendStatus("CLIENT_SENT");
//				nocRateUploadedStatusRepository.save(s);
//			}
//		}
//	}

	@Transactional
	@Override
	public void sendFinalClientMail(String clientName, String trunkName) {

		try {
			ClientEntity client = clientRepository.findByName(clientName);
			if (client == null)
				throw new RuntimeException("Client not found");

			Trunk trunk = trunkRepository.findByName(trunkName);
			if (trunk == null)
				throw new RuntimeException("Trunk not found");

			ClientRateSheetEntity sheet = rateSheetRepository.findByClientAndTrunk(client, trunk)
					.orElseThrow(() -> new RuntimeException("Rate sheet not found"));

			CommonTable template = templateRepo.findByType("RATE_TEMPLATE")
					.orElseThrow(() -> new RuntimeException("RATE_TEMPLATE not found"));

			String subject = template.getValue2().replace("{clientName}", clientName).replace("{trunk}", trunkName);

			String body = template.getValue1().replace("{clientName}", clientName).replace("{trunk}", trunkName)
					+ buildRateChangeTable(sheet.getFileContent());
			System.out.println("To Email" + client.getPrimaryEmail());
			System.out.println("CC Email" + client.getCcEmails());

			sendMail(client.getPrimaryEmail(), client.getCcEmails(), subject, body, sheet);

		} catch (Exception e) {
			throw new RuntimeException("Failed to send final client mail", e);
		}
	}

	@Override
	public Object sendRateEmail(RateSendRequest req) throws Exception {

		ClientEntity client = clientRepository.findByName(req.getClientName());
		Trunk trunk = trunkRepository.findByName(req.getTrunkName());

		NocRateUploadedStatus statusEntity = nocRateUploadedStatusRepository
				.findByClientNameAndTrunkName(req.getClientName(), req.getTrunkName())
				.orElseGet(NocRateUploadedStatus::new);

		statusEntity.setClientName(req.getClientName());
		statusEntity.setTrunkName(req.getTrunkName());
		statusEntity.setNocStatus("Pending");
		statusEntity.setRoutingStatus("Pending");
		statusEntity.setClientSendStatus("Pending");
		statusEntity.setRemark(null);
		statusEntity.setClientAcceptanceStatus("Pending");
		statusEntity.setCreatedOn(LocalDateTime.now());
		statusEntity.setFlowType(RateFlowType.NORMAL.name());

		nocRateUploadedStatusRepository.save(statusEntity);

		ClientRateSheetEntity sheet = rateSheetRepository.findByClientAndTrunk(client, trunk)
				.orElseThrow(() -> new RuntimeException("Rate sheet not found"));

		boolean hasIncrease = excelHasIncrease(sheet.getFileContent());

		if (hasIncrease) {

			statusEntity.setClientSendStatus("CLIENT_SENT");
			statusEntity.setClientAcceptanceStatus("PENDING");
			statusEntity.setNocForwarded("NO");
			statusEntity.setRoutingForwarded("NO");

			nocRateUploadedStatusRepository.save(statusEntity);
			sendMailToClient(req, client, trunk, sheet);
			return "Rate sent to Client only (Increase found)";
		}

		forwardRateToNOC(req);
		forwardRateToRouting(req);

		return "Rate sent to NOC + Routing (Waiting for upload confirmation)";
	}

	@Override
	public Object forwardRateToNOC(RateSendRequest req) throws Exception {

		ClientRateSheetEntity sheet = getSheet(req.getClientName(), req.getTrunkName());

		EmailAddresses emails = getEmailsFromCommonTable("NOC_EMAIL");

		CommonTable template = templateRepo.findByType("RATE_FORWARD_TEMPLATE")
				.orElseThrow(() -> new RuntimeException("RATE_FORWARD_TEMPLATE not found"));

		String rateDetails = buildNOCRateChangeTable(sheet.getFileContent()) + nocButtons(req);

		String subject = template.getValue2().replace("{clientName}", req.getClientName()).replace("{trunk}",
				req.getTrunkName());

		String body = template.getValue1().replace("{clientName}", req.getClientName())
				.replace("{trunk}", req.getTrunkName()).replace("{RATES_DETAILS}", rateDetails);

		sendMail(emails.getTo(), emails.getCc(), subject, body, sheet);

		return "NOC mail sent";
	}

	@Override
	public Object forwardRateToRouting(RateSendRequest req) throws Exception {

		ClientRateSheetEntity sheet = getSheet(req.getClientName(), req.getTrunkName());

		EmailAddresses emails = getEmailsFromCommonTable("ROUTING_EMAIL");

		CommonTable template = templateRepo.findByType("ROUTING_TEMPLATE")
				.orElseThrow(() -> new RuntimeException("ROUTING_TEMPLATE not found"));

		String body = template.getValue1();

		String routingTable = buildRoutingTable(req.getRoutingRateList());
		System.out.println(
				"Routing list size = " + (req.getRoutingRateList() == null ? "NULL" : req.getRoutingRateList().size()));

		String buttons = routingButtons(req);

		body = body.replace("{ROUTING_TABLE}", routingTable + "<br/>" + buttons);

		String subject = template.getValue2().replace("{trunk}", req.getTrunkName()).replace("{clientName}",
				req.getClientName());

		sendMail(emails.getTo(), emails.getCc(), subject, body, sheet);

		return "Routing mail sent";
	}

	private EmailAddresses getEmailsFromCommonTable(String type) {

		CommonTable ct = templateRepo.findByType(type)
				.orElseThrow(() -> new RuntimeException(type + " not configured"));

		return new EmailAddresses(parseEmails(ct.getValue1()), // TO
				parseEmails(ct.getValue2()) // CC
		);
	}

	private List<String> parseEmails(String emails) {
		if (emails == null || emails.isBlank())
			return List.of();

		return Arrays.stream(emails.split(",")).map(String::trim).filter(e -> !e.isEmpty()).toList();
	}

//	private String buildRoutingTable(List<RoutingRateDTO> list) {
//
//		if (list == null || list.isEmpty()) {
//			return "";
//		}
//
//		StringBuilder sb = new StringBuilder();
//
//		sb.append("""
//				    <br/><br/>
//				    <table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse; width:100%'>
//				        <tr style='background-color:#f2f2f2'>
//				            <th>Country</th>
//				            <th>Vendor</th>
//				            <th>Trunk</th>
//				            <th>Vendor Cost</th>
//				            <th>Remark</th>
//				        </tr>
//				""");
//
//		for (RoutingRateDTO r : list) {
//			sb.append("""
//					    <tr>
//					        <td>%s</td>
//					        <td>%s</td>
//					        <td>%s</td>
//					        <td>%s</td>
//					        <td>%s</td>
//					    </tr>
//					""".formatted(r.getCountry(), r.getVendor(), r.getTrunk(), r.getVendorCost(), r.getRemark()));
//		}
//
//		sb.append("</table>");
//
//		return sb.toString();
//	}

	private String buildRoutingTable(List<RoutingRateDTO> list) {

		if (list == null || list.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("""
				    <table border="1" cellpadding="8" cellspacing="0" width="100%"
				           style="border-collapse:collapse;text-align:center;font-family:Arial;">
				        <tr style="background:#D3D3D3;color:#000080;">
				            <th>Country</th>
				            <th>Vendor</th>
				            <th>Trunk</th>
				            <th>Vendor Cost</th>
				            <th>Remark</th>
				        </tr>
				""");

		for (RoutingRateDTO r : list) {
			sb.append("""
					    <tr>
					        <td>%s</td>
					        <td>%s</td>
					        <td>%s</td>
					        <td>%s</td>
					        <td>%s</td>
					    </tr>
					""".formatted(safe(r.getCountry()), safe(r.getVendor()), safe(r.getTrunk()),
					safe(r.getVendorCost()), safe(r.getRemark())));
		}

		sb.append("</table>");

		return sb.toString();
	}

	private String clientAcceptButton(String clientName, String trunkName) {

		return """
				    <br/><br/>
				    <a href="%s/mail/accept?clientName=%s&trunkName=%s"
				       style="
				         background-color:#28a745;
				         color:#ffffff;
				         padding:12px 22px;
				         text-decoration:none;
				         font-weight:bold;
				         border-radius:5px;
				         display:inline-block;
				       ">
				       ✅ ACCEPT RATE
				    </a>
				""".formatted(baseUrl, clientName, trunkName);
	}

	@Override
	@Transactional
	public void sendMailToClientByScheduler(String clientName, String trunkName) throws Exception {

		ClientEntity client = clientRepository.findByName(clientName);
		if (client == null)
			throw new RuntimeException("Client not found");

		Trunk trunk = trunkRepository.findByName(trunkName);
		if (trunk == null)
			throw new RuntimeException("Trunk not found");

		ClientRateSheetEntity sheet = rateSheetRepository.findByClientAndTrunk(client, trunk)
				.orElseThrow(() -> new RuntimeException("Rate sheet not found"));

		RateSendRequest req = new RateSendRequest();
		req.setClientName(clientName);
		req.setTrunkName(trunkName);

		sendMailToClient(req, client, trunk, sheet);
	}

	public String buildRateMailBody() {
		return """
								<!DOCTYPE html>
								<html>
								<body style="margin:0;padding:0;background:#f4f6f8;font-family:Calibri,Arial,sans-serif;">

								<table width="100%" cellpadding="0" cellspacing="0" style="background:#f4f6f8;">
								<tr>
								<td align="center">

								<!-- MAIN CONTAINER -->
								<table width="900" cellpadding="0" cellspacing="0" style="background:#ffffff;">

								<!-- HEADER -->
								<tr>
								<td style="background:#A9A9A9;color:#000080;text-align:center;padding:15px;">
								  <div style="font-size:24px;font-weight:bold;">{subject}</div>
								</td>
								</tr>

								<!-- BODY -->
								<tr>
								<td style="padding:28px;font-size:15px;color:#333333;line-height:1.6;">

								<p>Dear Partner,</p>

								<p>I hope you are doing well.</p>

								<p>
								Please find attached our latest <b>Rate Notification</b> for your review.<br/>
								The updated rates will apply to traffic sent through the following trunk/prefix:
								</p>

								<table width="100%" cellpadding="10" cellspacing="0" style="background:#ffffff;margin:15px 0;">
								<tr>
								<td style="font-weight:bold;">
								Prefix / Trunk : {prefix} / {trunk}
								</td>
								</tr>
								</table>

								<!-- RATE TABLE -->
								{RATES_DETAILS}

								<br/><br/>

								<table width="100%" cellpadding="15" cellspacing="0"
								       style="margin-top:30px;background:#f8f9fb;">
								<tr>
								<td>

								<div style="font-size:16px;font-weight:bold;color:#1b3f6b;margin-bottom:10px;">
								Important – Billing Increments
								</div>

								<div style="font-size:14px;margin-bottom:10px;">
								All destinations are billed on <b>1/1</b> increment unless stated below:
								</div>

								<table width="100%" cellpadding="4" cellspacing="0"
				       style="font-size:13px;color:#333333;line-height:1.5;">
				       <tr><td style="padding-top:8px;"><b>60/1 Destinations</b></td></tr>
				  <tr><td> Azerbaijan, Ethiopia Mobile Safaricom, Algeria Mobile Ooredoo, Brunei, Thailand, Singapore, South Korea, Vietnam, Gambia, Indonesia, Philippines Fix (PLDT), Philippines Mobile (Smart), Cambodia Cellcard, Cambodia Metfone Breakout, Cambodia Smart Breakout, Cameroon Mobile Orange, Colombia All, Saint Lucia, Sudan North Mobile (Zain), Macau, Philippines All, Ethiopia, Sudan Mobile MTN, Democratic Republic of the Congo All, New Zealand All, Cameroon All, Norway All, Sweden All</td></tr>

				  <tr><td style="padding-top:8px;"><b>60/60 Destinations</b></td></tr>

				  <tr><td> Cook Islands, Fiji, French Polynesia, Haiti, Kiribati, Maldives, Nauru, New Caledonia, Niue, Papua New Guinea, Samoa, Solomon Islands, Surinam, Thuraya, Tokelau, Tonga, Tuvalu, Vanuatu, American Samoa, GMSS, Lesotho, Mexico, Togo, Russia Mobile (Globalstar), India, Sri Lanka, Palau, Inmarsat (All), Micronesia, Comoros, Nepal, UAE, Saudi Arabia Mobile (STC / Al Jawal), Myanmar, Qatar, Saint Helena, Sao Tome Principe, Mali, Diego Garcia, Anguilla, Norfolk Island, Mauritania, Sudan Mobile (Sudatel), Oman All, Wallis, Futuna All, Togo All</td></tr>

				  <tr><td style="padding-top:8px;"><b>Special Billing</b></td></tr>

				  <tr><td>Brazil (55) – 30/6</td></tr>
				  <tr><td>Cambodia (855) – 15/15</td></tr>
				  <tr><td>Canada – 6/6</td></tr>
				</table>
								</td>
								</tr>
								</table>

								</td>
								</tr>

								<!-- FOOTER -->
								<tr>
								<td style="padding:24px;border-top:1px solid #e0e0e0;font-size:12px;color:#5a5a5a;">

								<p>
								Thanks,<br/>
								<b>Rates Manager | Rates Department</b><br/>
								<span style="font-weight:bold;color:#0a3d62;font-size:17px;">Nuetech Stack</span>
								</p>

								<p><b>🌍 Global Presence:</b> USA | UAE | Singapore</p>
								<p><b>📍 HQ:</b> 160, Robinson Road, #14-04 Singapore (068914)</p>

								<a href="https://www.nuetechstack.net" style="color:#0a3d62;text-decoration:none;">
								www.nuetechstack.net
								</a>

								<br/><br/>
								<img src="cid:nt_logo" style="width:90px;"><br/>
								<img src="cid:linkedin_logo" style="width:120px;"><br/><br/>
								<img src="cid:banner" style="width:100%;max-width:600px;">

								<p><b>Please consider the environment before printing my email</b></p>

								 <p style="font-size:11px; line-height:1.4;">
											            Please note that the information and attachments in this email are intended for the exclusive use of the addressee and may contain confidential or privileged information. If you are not the intended recipient, please do not forward, copy or print the message or its attachments. Notify me at the above address, and delete this message and any attachments. Thank you.
											          </p>

								</td>
								</tr>

								</table>
								<!-- END MAIN -->
								</td>
								</tr>
								</table>

								</body>
								</html>
								""";
	}

	@Override
	public String initiateRateFlow(RateSendRequest req) throws Exception {

		ClientEntity client = clientRepository.findByName(req.getClientName());
		Trunk trunk = trunkRepository.findByName(req.getTrunkName());

		ClientRateSheetEntity sheet = rateSheetRepository.findByClientAndTrunk(client, trunk)
				.orElseThrow(() -> new RuntimeException("Rate sheet not found"));

		boolean hasIncrease = excelHasIncrease(sheet.getFileContent());

		NocRateUploadedStatus status = nocRateUploadedStatusRepository
				.findByClientNameAndTrunkName(req.getClientName(), req.getTrunkName())
				.orElseGet(NocRateUploadedStatus::new);

		initStatus(status, req);

		if (hasIncrease) {
			sendMailToClient(req, client, trunk, sheet);

			status.setClientSendStatus("CLIENT_SENT");
			status.setClientAcceptanceStatus("PENDING");
			status.setNocForwarded("NO");

			nocRateUploadedStatusRepository.save(status);
			return "Increase found → Sent to Client";

		} else {
			forwardRateToNOC(req);

			status.setNocForwarded("YES");
			status.setClientSendStatus("NO");

			nocRateUploadedStatusRepository.save(status);
			return "No increase → Sent to NOC only";
		}
	}

	private void initStatus(NocRateUploadedStatus s, RateSendRequest req) {
		s.setClientName(req.getClientName());
		s.setTrunkName(req.getTrunkName());
		s.setCreatedOn(LocalDateTime.now());
		s.setFlowType(RateFlowType.NOC_ONLY.name());

		s.setNocStatus(null);
		s.setRoutingStatus(null);
		s.setClientSendStatus(null);
		s.setClientAcceptanceStatus(null);
		s.setRoutingForwarded("NO");
	}

	private String readPrefixFromExcel(byte[] excelBytes) throws IOException {

		DataFormatter formatter = new DataFormatter();

		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {

			Sheet sheet = workbook.getSheetAt(0);

			for (Row row : sheet) {
				for (Cell cell : row) {

					String cellText = formatter.formatCellValue(cell).trim();

					if ("Prefix".equalsIgnoreCase(cellText)) {

						int startCol = cell.getColumnIndex();

						for (int c = startCol + 1; c < row.getLastCellNum(); c++) {

							Cell valueCell = row.getCell(c);
							String value = formatter.formatCellValue(valueCell).trim();

							if (!value.isEmpty()) {
								return value;
							}
						}

						throw new RuntimeException("Prefix value not found next to Prefix header");
					}
				}
			}
		}

		throw new RuntimeException("Prefix header not found in Excel file");
	}

	@Override
	public void sendApprovedReply(ReplyMailRequest req) throws MessagingException {
		sendReply(req, "APPROVED_TEMPLATE");
	}

	@Override
	public void sendRejectedReply(ReplyMailRequest req) throws MessagingException {
		sendReply(req, "REJECT_TEMPLATE");
	}

	private void sendReply(ReplyMailRequest req, String templateType) {

		EmailReceiveResponse mail = cache.getAllSortedLatestFirst().stream()
				.filter(m -> req.getInboxUid().equals(m.getUid())).findFirst()
				.orElseThrow(() -> new RuntimeException("Mail not found"));

		CommonTable template = templateRepo.findByTypeAndEnableAndDeleted(templateType, "Y", "N");

		List<String> toEmails = List.of(mail.getFrom());

		Set<String> ccSet = new HashSet<>();

		if (mail.getTo() != null) {
			ccSet.addAll(mail.getTo());
		}
		if (mail.getCc() != null) {
			ccSet.addAll(mail.getCc());
		}

		String subject = "Re: " + mail.getSubject();

		String body = template.getValue1();

		if ("REJECT_TEMPLATE".equals(templateType)) {

			String rejectMsg = req.getMsg();

			if (rejectMsg == null || rejectMsg.trim().isEmpty()) {
				rejectMsg = "Rates have been rejected.";
			}

			body = body.replace("{message}", rejectMsg);
		}

		try {
			sendMail(toEmails, new ArrayList<>(ccSet),
//            		List.of("juleesingh6497@gmail.com"),
//                    List.of("juleesingh6497@gmail.com"), 
					subject, body, null);
		} catch (Exception e) {
			throw new RuntimeException("Failed to send reply mail", e);
		}
	}

//    @Override
//    public void forwardNoc(ForwardNocRequest req) {
//
//        EmailReceiveResponse mail =
//                cache.getAllSortedLatestFirst().stream()
//                        .filter(m -> req.getInboxUid().equals(m.getUid()))
//                        .findFirst()
//                        .orElseThrow(() -> new RuntimeException("Mail not found"));
//        
//        EmailAddresses emails = getEmailsFromCommonTable("NOC_EMAIL");
//
//        CommonTable template =
//                templateRepo.findByTypeAndEnableAndDeleted(
//                        "NOC_FORWARD_TEMPLATE", "Y", "N");
//
//        String forwardedBody =
//                template.getValue1()
//                        + "<br/><br/>"
//                        + "<hr/>"
//                        + "<b>From:</b> " + mail.getFrom() + "<br/>"
//                        + "<b>Subject:</b> " + mail.getSubject() + "<br/><br/>"
//                        + mail.getBody();
//
//        try {
//            sendMail(
//                    emails.getTo(),
//                    emails.getCc(),
//                    "Fw: " + mail.getSubject(),
//                    forwardedBody,
//                    null  
//            );
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to forward NOC mail", e);
//        }
//    }

	@Override
	public void forwardNoc(ForwardNocRequest req) {

		EmailReceiveResponse mail = cache.getAllSortedLatestFirst().stream()
				.filter(m -> req.getInboxUid().equals(m.getUid())).findFirst()
				.orElseThrow(() -> new RuntimeException("Mail not found"));

		EmailAddresses emails = getEmailsFromCommonTable("NOC_EMAIL");

		CommonTable template = templateRepo.findByTypeAndEnableAndDeleted("NOC_FORWARD_TEMPLATE", "Y", "N");

		String forwardedBody = template.getValue1() + "<br/><br/><hr/>" + "<b>From:</b> " + mail.getFrom() + "<br/>"
				+ "<b>Subject:</b> " + mail.getSubject() + "<br/><br/>" + mail.getBody();

		try {
			sendMailWithAttachments(emails.getTo(), emails.getCc(), "Fw: " + mail.getSubject(), forwardedBody,
					mail.getAttachmentData());
		} catch (Exception e) {
			throw new RuntimeException("Failed to forward NOC mail", e);
		}
	}

	public void sendMailWithAttachments(List<String> to, List<String> cc, String subject, String body,
			Map<String, byte[]> attachments) throws Exception {

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message, true);

		helper.setTo(to.toArray(new String[0]));
		if (cc != null)
			helper.setCc(cc.toArray(new String[0]));
		helper.setSubject(subject);
		helper.setText(body, true);

		if (attachments != null) {
			for (Map.Entry<String, byte[]> e : attachments.entrySet()) {
				helper.addAttachment(e.getKey(), new ByteArrayResource(e.getValue()));
			}
		}

		mailSender.send(message);
	}

	public void processIncomingMail(EmailReceiveResponse mail) {

		if (!containsAcceptKeyword(mail.getBody()))
			return;

		Optional<NocRateUploadedStatus> opt = mail.getInReplyTo() != null
				? nocRateUploadedStatusRepository.findByMailMessageId(mail.getInReplyTo())
				: nocRateUploadedStatusRepository.findByMailMessageId(mail.getFrom());

		if (opt.isEmpty())
			return;

		NocRateUploadedStatus status = opt.get();

		if (!"PENDING".equalsIgnoreCase(status.getClientAcceptanceStatus()))
			return;

		if (status.getToEmail().stream().noneMatch(e -> e.equalsIgnoreCase(mail.getFrom()))) {
			return;
		}

		status.setClientAcceptanceStatus("ACCEPTED");
		status.setRemark("Client accepted via manual mail reply");
		nocRateUploadedStatusRepository.save(status);
	}

	private static final List<Pattern> ACCEPT_PATTERNS = List.of(
			Pattern.compile("\\b(rate|rates)\\b.*\\b(approved|accepted)\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\ball\\s+rates\\s+(received|uploaded)\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\bupdated\\b.*\\brates\\b.*\\bsuccessfully\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\bwe\\s+have\\s+updated\\b.*\\brates\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\baccepted\\b", Pattern.CASE_INSENSITIVE));

	private boolean containsAcceptKeyword(String body) {
		if (body == null)
			return false;

		String clean = stripQuotedText(body.toLowerCase());

		return ACCEPT_PATTERNS.stream().anyMatch(p -> p.matcher(clean).find());
	}

	private String stripQuotedText(String text) {

		int idx = text.indexOf("on ");
		if (idx > 0 && text.contains("wrote")) {
			return text.substring(0, idx);
		}

		idx = text.indexOf("-----original message-----");
		if (idx > 0)
			return text.substring(0, idx);

		return text;
	}

	@Override
	public PromotionMailResponseDTO sendPromotionMail(PromotionMailRequest request) {

		int success = 0;
		int failed = 0;

		CommonTable template = templateRepo.findByTypeAndEnableAndDeleted("PROMOTION_TEMPLATE", "Y", "N");

		if (template == null) {
			throw new RuntimeException("Promotion template not found");
		}

		String rateTable = buildRateTable(request.getRateList());
		String mailBody = template.getValue1().replace("{RATE_TABLE}", rateTable);
		String subject = template.getValue2();

		List<PromotionClientEntity> clients = promotionRepository.findByEmailIsNotNull();

		for (PromotionClientEntity client : clients) {
			try {
				sendMail(List.of(client.getEmail()), null, subject, mailBody, null);

				client.setStatus("MAIL_SENT");
				client.setSendDate(LocalDate.now());
				success++;

			} catch (Exception ex) {
				client.setStatus("FAILED");
				client.setSendDate(LocalDate.now());
				failed++;
			}

			promotionRepository.save(client);
		}

		return new PromotionMailResponseDTO(clients.size(), success, failed);
	}

	public static String buildRateTable(List<PromotionRateDTO> list) {

	    StringBuilder rows = new StringBuilder();

	    for (PromotionRateDTO r : list) {
	        rows.append("""
	            <tr>
	                <td style="padding:10px; border:1px solid #dcdcdc;">%s</td>
	                <td style="padding:10px; border:1px solid #dcdcdc;">%s</td>
	                <td style="padding:10px; border:1px solid #dcdcdc; font-weight:bold; color:#2f5597;">%s</td>
	                <td style="padding:10px; border:1px solid #dcdcdc;">%s</td>
	            </tr>
	        """.formatted(
	                safe1(r.getDestination()),
	                safe1(r.getQuality()),
	                safe1(r.getOffer()),
	                safe1(r.getRemark())
	        ));
	    }

	    return """
	        <table width="100%%" cellpadding="0" cellspacing="0"
	               style="border-collapse:collapse; margin-top:20px; font-size:14px;">
	            <tr style="background:#2f5597; color:#ffffff;">
	                <th style="padding:12px; border:1px solid #2f5597;">Destination</th>
	                <th style="padding:12px; border:1px solid #2f5597;">Quality</th>
	                <th style="padding:12px; border:1px solid #2f5597;">Offer</th>
	                <th style="padding:12px; border:1px solid #2f5597;">Remark</th>
	            </tr>
	            %s
	        </table>
	    """.formatted(rows.toString());
	}

	private static String safe1(String value){
	    return value == null ? "" : value;
	}


	@Override
	public List<PromotionClientEntity> getPromotionMailData() {
		return promotionRepository.findAllByOrderBySendDateDesc();
	}

	@Override
	public PromotionMailResponseDTO sendBillingIncementMail() {

		int success = 0;
		int failed = 0;

		CommonTable template = templateRepo.findByTypeAndEnableAndDeleted("BILLINGINCREMENT_TEMPLATE", "Y", "N");

		if (template == null) {
			throw new RuntimeException("Billing template not found");
		}

//	    String rateTable = buildRateTable(request.getRateList());
		String mailBody = template.getValue1();
		String subject = template.getValue2();

		List<ClientEntity> clients = clientRepository.findAll();

		for (ClientEntity client : clients) {
			try {
				List<String> toMails = client.getPrimaryEmail();

				if (toMails == null || toMails.isEmpty()) {
					continue;
				}

				List<String> ccMails = new ArrayList<>();

				if (client.getCcEmails() != null) {
					ccMails.addAll(client.getCcEmails());
				}

				if (client.getAccountManagerEmail() != null && !client.getAccountManagerEmail().isBlank()) {
					ccMails.add(client.getAccountManagerEmail());
				}

				sendMail(toMails, ccMails.isEmpty() ? null : ccMails, subject, mailBody, null);

				success++;

			} catch (Exception ex) {
				failed++;
			}
		}

		return new PromotionMailResponseDTO(clients.size(), success, failed);
	}

	@Override
	public AttachmentResponse getAttachment(String uid, String fileName) {

		EmailReceiveResponse mail = cache.getAllSortedLatestFirst().stream().filter(m -> uid.equals(m.getUid()))
				.findFirst().orElseThrow(() -> new RuntimeException("Mail not found"));

		byte[] data = mail.getAttachmentData().get(fileName);

		if (data == null) {
			throw new RuntimeException("Attachment not found");
		}

		MediaType mediaType = detectMediaType(fileName);

		return new AttachmentResponse(fileName, data, mediaType);
	}

	private MediaType detectMediaType(String fileName) {

		String f = fileName.toLowerCase();

		if (f.endsWith(".pdf"))
			return MediaType.APPLICATION_PDF;
		if (f.endsWith(".png"))
			return MediaType.IMAGE_PNG;
		if (f.endsWith(".jpg") || f.endsWith(".jpeg"))
			return MediaType.IMAGE_JPEG;
		if (f.endsWith(".xlsx"))
			return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		if (f.endsWith(".csv"))
			return MediaType.TEXT_PLAIN;

		return MediaType.APPLICATION_OCTET_STREAM;
	}
}
