package com.ncs.ratenotif.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ncs.ratenotif.entity.NocRateUploadedStatus;
import com.ncs.ratenotif.entity.PromotionClientEntity;
import com.ncs.ratenotif.repository.NocRateUploadedStatusRepository;
import com.ncs.ratenotif.request.ForwardNocRequest;
import com.ncs.ratenotif.request.PromotionMailRequest;
import com.ncs.ratenotif.request.RateSendRequest;
import com.ncs.ratenotif.request.ReplyMailRequest;
import com.ncs.ratenotif.request.SendMailRequest;
import com.ncs.ratenotif.response.AttachmentResponse;
import com.ncs.ratenotif.response.EmailReceiveResponse;
import com.ncs.ratenotif.response.PromotionMailResponseDTO;
import com.ncs.ratenotif.response.dto.ResponseDTO;
import com.ncs.ratenotif.response.dto.ResponseUtil;
import com.ncs.ratenotif.service.MailService;

import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/mail")
public class MailController {

	private final CommonController commonController;

	@Autowired
	private NocRateUploadedStatusRepository nocRateUploadedStatusRepository;

	@Autowired
	private MailService mailSenderService;

	MailController(CommonController commonController) {
		this.commonController = commonController;
	}

	@PostMapping("/send")
	public ResponseEntity<?> sendMail(@RequestBody SendMailRequest request) {

		String response = mailSenderService.sendEmail(request);

		return ResponseEntity.ok(response);
	}

//	@GetMapping("/received")
//    public ResponseEntity<List<EmailResponse>> getAllReceivedMails() {
//        try {
//            List<EmailResponse> emails = mailSenderService.fetchEmails();
//
//            emails.sort((a, b) -> b.getSentDate().compareTo(a.getSentDate()));
//
//            return ResponseEntity.ok(emails);
//
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError().build();
//        }
//    }

	@GetMapping("/inbox")
	public ResponseEntity<List<EmailReceiveResponse>> getInbox() {
		return ResponseEntity.ok(mailSenderService.getCachedEmails());
	}

	@GetMapping("/view/{uid}/{fileName}")
	public ResponseEntity<byte[]> viewAttachment(@PathVariable String uid, @PathVariable String fileName) {

		AttachmentResponse res = mailSenderService.getAttachment(uid, fileName);

		return ResponseEntity.ok().contentType(res.getMediaType())
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + res.getFileName() + "\"")
				.body(res.getData());
	}

	@GetMapping("/download/{uid}/{fileName}")
	public ResponseEntity<byte[]> downloadAttachment(@PathVariable String uid, @PathVariable String fileName) {

		AttachmentResponse res = mailSenderService.getAttachment(uid, fileName);

		return ResponseEntity.ok().contentType(res.getMediaType())
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + res.getFileName() + "\"")
				.body(res.getData());
	}

	@PostMapping("/syncNow")
	public ResponseEntity<String> triggerSyncNow() {
		try {
			mailSenderService.scheduledSync();
			return ResponseEntity.ok("sync triggered");
		} catch (Exception ex) {
			return ResponseEntity.status(500).body("error: " + ex.getMessage());
		}
	}

	@PostMapping("/sendrate")
	public ResponseEntity<ResponseDTO<Object>> sendRate(@RequestBody RateSendRequest req) {
		try {
//	        String message = mailSenderService.sendRateEmail(req.getClientName(), req.getTrunkName());
			return ResponseUtil.createSuccessMessage(mailSenderService.sendRateEmail(req), "Mail Send successfully",
					HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseUtil.createErrorMessage(null, "Mail not sent", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/preview")
	public ResponseEntity<ResponseDTO<Object>> previewEmail(@RequestBody RateSendRequest req) {
		try {
//	        String message = mailSenderService.sendRateEmail(req.getClientName(), req.getTrunkName());
			return ResponseUtil.createSuccessMessage(mailSenderService.previewRateEmail(req), "Mail Fetch",
					HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseUtil.createErrorMessage(null, "Mail Not fetch", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/forwardMailToNoc")
	public ResponseEntity<ResponseDTO<Object>> forwardRateToNOC(@RequestBody RateSendRequest req) {
		try {
//	        String message = mailSenderService.sendRateEmail(req.getClientName(), req.getTrunkName());
			return ResponseUtil.createSuccessMessage(mailSenderService.forwardRateToNOC(req),
					"Forward Mail Send successfully", HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseUtil.createErrorMessage(null, "Mail not Forwaded", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/uploaded")
	public String rateUploaded(@RequestParam String clientName, @RequestParam String trunkName) {

		mailSenderService.saveStatus(clientName, trunkName, "UPLOADED", "UPLOADED", null);

		return "Rate uploaded successfully. You may close this page.";
	}

	// ❌ NOT UPLOADED (opens remark page)
	@GetMapping("/not-uploaded")
	public String notUploadedPage(@RequestParam String clientName, @RequestParam String trunkName) {

		return """
				    <html>
				    <body>
				        <h3>Reason for not uploading rates</h3>
				        <form method='post' action='/mail/not-uploaded'>
				            <input type='hidden' name='clientName' value='%s'/>
				            <input type='hidden' name='trunkName' value='%s'/>
				            <textarea name='remark' rows='4' cols='40' required></textarea><br/><br/>
				            <button type='submit'>Submit</button>
				        </form>
				    </body>
				    </html>
				""".formatted(clientName, trunkName);
	}

	// ❌ NOT UPLOADED SUBMIT
	@PostMapping("/not-uploaded")
	public String submitNotUploaded(@RequestParam String clientName, @RequestParam String trunkName,
			@RequestParam String remark) {

		mailSenderService.saveStatus(clientName, trunkName, "NOT_UPLOADED", "NOT_UPLOADED", remark);

		return "Status updated successfully. You may close this page.";
	}

	@GetMapping("/routing/uploaded")
	public String routingUploaded(@RequestParam String clientName, @RequestParam String trunkName) {

		mailSenderService.saveStatus(clientName, trunkName, null, "UPLOADED", null);
		return "Routing uploaded successfully";
	}

	@GetMapping("/routing/not-uploaded")
	public String routingNotUploaded(@RequestParam String clientName, @RequestParam String trunkName) {

		return """
				    <form method='post' action='/mail/routing/not-uploaded'>
				        <input type='hidden' name='clientName' value='%s'/>
				        <input type='hidden' name='trunkName' value='%s'/>
				        <textarea name='remark'></textarea>
				        <button type='submit'>Submit</button>
				    </form>
				""".formatted(clientName, trunkName);
	}

	@PostMapping("/routing/not-uploaded")
	public String routingNotUploadedSubmit(@RequestParam String clientName, @RequestParam String trunkName,
			@RequestParam String remark) {

		mailSenderService.saveStatus(clientName, trunkName, null, "NOT_UPLOADED", remark);
		return "Routing status saved";
	}

//	@GetMapping("/accept")
//	public ResponseEntity<String> acceptRate(@RequestParam String clientName, @RequestParam String trunkName) {
//
//		NocRateUploadedStatus status = nocRateUploadedStatusRepository
//				.findByClientNameAndTrunkName(clientName, trunkName)
//				.orElseThrow(() -> new RuntimeException("Record not found"));
//
//		if ("ACCEPTED".equalsIgnoreCase(status.getClientAcceptanceStatus())) {
//			return ResponseEntity.ok(htmlPage("Rate already accepted."));
//		}
//
//		status.setClientAcceptanceStatus("ACCEPTED");
//		status.setRemark("Client accepted rate");
//		nocRateUploadedStatusRepository.save(status);
//
//		return ResponseEntity.ok(htmlPage("✅ Rate accepted successfully"));
//	}

	@GetMapping("/accept")
	public ResponseEntity<String> acceptRate(@RequestParam String clientName, @RequestParam String trunkName) {

		NocRateUploadedStatus status = nocRateUploadedStatusRepository
				.findByClientNameAndTrunkName(clientName, trunkName)
				.orElseThrow(() -> new RuntimeException("Record not found"));

		List<String> toEmails = status.getToEmail();

//		boolean authorized = toEmails.stream().anyMatch(e -> e.equalsIgnoreCase(email));

//		if (!authorized) {
//			return ResponseEntity.ok(htmlPage("Not authorized"));
//		}

		if ("ACCEPTED".equalsIgnoreCase(status.getClientAcceptanceStatus())) {
			return ResponseEntity.ok(htmlPage("Rate already accepted"));
		}

		status.setClientAcceptanceStatus("ACCEPTED");
		status.setRemark("Client accepted via button");
		nocRateUploadedStatusRepository.save(status);

		return ResponseEntity.ok(htmlPage("✅ Rate accepted successfully"));
	}

	private String htmlPage(String msg) {
		return """
				<html>
				<head>
				    <title>Rate Acceptance</title>
				</head>
				<body style="font-family:Arial;text-align:center;margin-top:80px;">
				    <h2>%s</h2>
				    <p>You may close this window.</p>
				</body>
				</html>
				""".formatted(msg);
	}

	@PostMapping("/sendUpdatedRate")
	public ResponseEntity<ResponseDTO<Object>> sendRateV2(@RequestBody RateSendRequest req) {

		try {
			return ResponseUtil.createSuccessMessage(mailSenderService.initiateRateFlow(req), "Rate process initiated",
					HttpStatus.OK);
		} catch (Exception e) {
			return ResponseUtil.createErrorMessage(null, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/approve")
	public ResponseEntity<String> approve(@RequestBody ReplyMailRequest req) throws MessagingException {
		mailSenderService.sendApprovedReply(req);
		return ResponseEntity.ok("Approved mail sent");
	}

	@PostMapping("/reject")
	public ResponseEntity<String> reject(@RequestBody ReplyMailRequest req) throws MessagingException {
		mailSenderService.sendRejectedReply(req);
		return ResponseEntity.ok("Rejected mail sent");
	}

	@PostMapping("/forwardNoc")
	public ResponseEntity<String> forwardNoc(@RequestBody ForwardNocRequest req) throws MessagingException {
		mailSenderService.forwardNoc(req);
		return ResponseEntity.ok("NOC forwarded");
	}

	@PostMapping("/sendPromotionMail")
	public ResponseEntity<ResponseDTO<Object>> sendPromotionEmail(@RequestBody PromotionMailRequest req) {
		PromotionMailResponseDTO result = mailSenderService.sendPromotionMail(req);

		String message;

		if (result.getFailedCount() > 0) {
			message = "Mail sent partially. Some emails failed.";
		} else {
			message = "Mail sent successfully to all clients.";
		}

		return ResponseUtil.createSuccessMessage(result, message, HttpStatus.OK);
	}

	@GetMapping("/getPromotionRecord")
	public ResponseEntity<List<PromotionClientEntity>> getPromotionMailHistory() {
		return ResponseEntity.ok(mailSenderService.getPromotionMailData());
	}

	@PostMapping("/sendBilingIncrementMail")
	public ResponseEntity<ResponseDTO<Object>> sendBillingIncrementEmail() {
		PromotionMailResponseDTO result = mailSenderService.sendBillingIncementMail();

		String message;

		if (result.getFailedCount() > 0) {
			message = "Mail sent partially. Some emails failed.";
		} else {
			message = "Mail sent successfully to all clients.";
		}

		return ResponseUtil.createSuccessMessage(result, message, HttpStatus.OK);
	}
}
