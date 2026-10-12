package com.ncs.ratenotif.service;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.ncs.ratenotif.entity.PromotionClientEntity;
import com.ncs.ratenotif.request.ForwardNocRequest;
import com.ncs.ratenotif.request.PromotionMailRequest;
import com.ncs.ratenotif.request.RateSendRequest;
import com.ncs.ratenotif.request.ReplyMailRequest;
import com.ncs.ratenotif.request.SendMailRequest;
import com.ncs.ratenotif.response.AttachmentResponse;
import com.ncs.ratenotif.response.EmailReceiveResponse;
import com.ncs.ratenotif.response.PromotionMailResponseDTO;

import jakarta.mail.MessagingException;

public interface MailService {

	String sendEmail(SendMailRequest req);

//	List<EmailReceiveResponse> fetchEmails() throws Exception;

	Object sendRateEmail(RateSendRequest req) throws Exception;

	List<EmailReceiveResponse> getCachedEmails();

	void scheduledSync();

	Object previewRateEmail(RateSendRequest req);

	Object forwardRateToNOC(RateSendRequest req) throws Exception;

	void saveStatus(String clientName, String trunkName, String nocStatus,String routingStatus, String remark);

	Object forwardRateToRouting(RateSendRequest req) throws Exception;

	void sendFinalClientMail(String clientName, String trunkName);

	void sendMailToClientByScheduler(String clientName, String trunkName) throws Exception;

	String initiateRateFlow(RateSendRequest req) throws Exception;

	void sendApprovedReply(ReplyMailRequest req) throws MessagingException;

	void sendRejectedReply(ReplyMailRequest req) throws MessagingException;;

	void forwardNoc(ForwardNocRequest req) throws MessagingException;

	PromotionMailResponseDTO sendPromotionMail(PromotionMailRequest request);

	List<PromotionClientEntity> getPromotionMailData();

	PromotionMailResponseDTO sendBillingIncementMail();

	AttachmentResponse getAttachment(String uid, String fileName);

}
