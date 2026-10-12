package com.ncs.ratenotif.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ncs.ratenotif.entity.NocRateUploadedStatus;
import com.ncs.ratenotif.entity.RateFlowType;
import com.ncs.ratenotif.repository.NocRateUploadedStatusRepository;
import com.ncs.ratenotif.request.RateSendRequest;
import com.ncs.ratenotif.service.MailService;

@Component
public class CleintRateAutoScheduler {

	@Autowired
	private NocRateUploadedStatusRepository nocRateUploadedStatusRepository;

	@Autowired
	private MailService emailService;

	@Scheduled(fixedDelay = 5000, initialDelay = 5000)
	public void autoSendToClient() {

	    List<NocRateUploadedStatus> list = nocRateUploadedStatusRepository.findAll();

	    for (NocRateUploadedStatus s : list) {

	        if ("CLIENT_SENT".equalsIgnoreCase(s.getClientSendStatus()))
	            continue;

	        if (!"UPLOADED".equalsIgnoreCase(s.getNocStatus()))
	            continue;

	        if (RateFlowType.NORMAL.name().equals(s.getFlowType())) {
	            if (!"UPLOADED".equalsIgnoreCase(s.getRoutingStatus()))
	                continue;
	        }

	        try {
	            emailService.sendMailToClientByScheduler(
	                    s.getClientName(),
	                    s.getTrunkName());

	            s.setClientSendStatus("CLIENT_SENT");
	            s.setRemark("Client mail sent");

	            nocRateUploadedStatusRepository.save(s);

	            System.out.println("✅ CLIENT MAIL SENT: "
	                    + s.getClientName() + " | " + s.getTrunkName());

	        } catch (Exception e) {

	            s.setClientSendStatus("CLIENT_FAILED");
	            s.setRemark("Client mail failed: " + e.getMessage());
	            nocRateUploadedStatusRepository.save(s);

	            e.printStackTrace();
	        }
	    }
	}

	
	@Scheduled(fixedDelay = 5000, initialDelay = 5000)
	public void autoSendAfterClientAcceptance() {

	    List<NocRateUploadedStatus> list = nocRateUploadedStatusRepository.findAll();

	    for (NocRateUploadedStatus s : list) {

	        if (!"ACCEPTED".equalsIgnoreCase(s.getClientAcceptanceStatus()))
	            continue;

	        try {
	            RateSendRequest req = new RateSendRequest();
	            req.setClientName(s.getClientName());
	            req.setTrunkName(s.getTrunkName());

	            if (!"YES".equalsIgnoreCase(s.getNocForwarded())) {
	                emailService.forwardRateToNOC(req);
	                s.setNocForwarded("YES");
	            }

	            if (RateFlowType.NORMAL.name().equals(s.getFlowType())) {

	                if (!"YES".equalsIgnoreCase(s.getRoutingForwarded())) {
	                    emailService.forwardRateToRouting(req);
	                    s.setRoutingForwarded("YES");
	                }
	            }

	            nocRateUploadedStatusRepository.save(s);

	        } catch (Exception e) {
	            s.setRemark("Auto forward failed: " + e.getMessage());
	            nocRateUploadedStatusRepository.save(s);
	        }
	    }
	}

	
}
