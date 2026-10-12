package com.ncs.ratenotif.component;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.ClientTrunk;
import com.ncs.ratenotif.entity.EmailTemplate;
import com.ncs.ratenotif.entity.TemplateType;
import com.ncs.ratenotif.entity.Trunk;
import com.ncs.ratenotif.repository.ClientRepository;
import com.ncs.ratenotif.repository.ClientTrunkRepository;
import com.ncs.ratenotif.repository.EmailTemplateRepository;
import com.ncs.ratenotif.repository.TrunkRepository;

@Component
public class DataInitializer implements CommandLineRunner{

	@Autowired
	private ClientRepository clientRepository;
	
	@Autowired
	private TrunkRepository trunkRepository;
	
	@Autowired
	private EmailTemplateRepository emailTemplateRepository;
	
	@Autowired
	private ClientTrunkRepository clientTrunkRepository;

	    @Override
	    public void run(String... args) throws Exception {
	        if (trunkRepository.count() == 0) {
	            initializeTrunks();
	        }
	        if (emailTemplateRepository.count() == 0) {
	            initializeGlobalTemplates();
	        }
	        if (clientRepository.count() == 0) {
//	            initializeSampleClients();
	        }
	    }

	    private void initializeTrunks() {
	        trunkRepository.save(new Trunk("Premium", "9999"));
	        trunkRepository.save(new Trunk("CC", "1111"));
	        trunkRepository.save(new Trunk("Standard", "8888"));
	        trunkRepository.save(new Trunk("TDM", "7777"));
	        trunkRepository.save(new Trunk("ORTP", "6666"));
	    }

	    private void initializeGlobalTemplates() {
	        emailTemplateRepository.deleteAll();

	        for (Trunk trunk : trunkRepository.findAll()) {
	            EmailTemplate sendTemplate = new EmailTemplate();
	            sendTemplate.setTrunk(trunk);
	            sendTemplate.setTemplateType(TemplateType.RATE_SEND);
	            sendTemplate.setSubjectPattern("NUETECH - " + trunk.getName() + " Rate Notification to {clientName}");
	            sendTemplate.setBody(getCommonRateSendBody(trunk.getName()));
	            sendTemplate.setIsActive(true);
	            emailTemplateRepository.save(sendTemplate);
	        }

	        initializeCommonActionTemplates();
	    }

	    private String getCommonRateSendBody(String trunkName) {
	        return "Dear Partner,\n\n" +
	               "Please find attached herewith the new rate notification for the product: " + trunkName + "\n" +
	               "Rates are effective as indicated on the attached spreadsheet.\n" +
	               "Attachments: Rate Sheet Attached.\n\n\n" +
	               "Thanks,\n" +
	               "Rates Manager || Rates Department\n" +
	               "Nuetech Stack \n" +
	               "📍 Global Presence:  USA || UAE || Singapore\n" +
	               "🏛 HQ: 160, Robinson Road, #14-04 Singapore (068914)\n" +
	               "🌐 www.nuetechstack.net\n\n\n" +
	               "Please consider the environment before printing my email\n\n" +
	               "Please note that the information and attachments in this email are intended for the exclusive use of the addressee and may contain confidential or privileged information. If you are not the intended recipient, please do not forward, copy or print the message or its attachments. Notify me at the above address, and delete this message and any attachments. Thank you.";
	    }

	    private void initializeCommonActionTemplates() {
	        String commonFooter = "\n\nThanks,\nRates Manager || Rates Department\nNuetech Stack \n📍 Global Presence:  USA || UAE || Singapore\n🏛 HQ: 160, Robinson Road, #14-04 Singapore (068914)\n🌐 www.nuetechstack.net\n\n\nPlease consider the environment before printing my email\n\nPlease note that the information and attachments in this email are intended for the exclusive use of the addressee and may contain confidential or privileged information. If you are not the intended recipient, please do not forward, copy or print the message or its attachments. Notify me at the above address, and delete this message and any attachments. Thank you.";

	        EmailTemplate acceptTemplate = new EmailTemplate();
	        acceptTemplate.setTemplateType(TemplateType.RATE_ACCEPT);
	        acceptTemplate.setSubjectPattern("RE: {originalSubject}");
	        acceptTemplate.setBody("Dear partner,\n\nAll rates have been accepted and updated." + commonFooter);
	        acceptTemplate.setIsActive(true);
	        emailTemplateRepository.save(acceptTemplate);

	        EmailTemplate forwardTemplate = new EmailTemplate();
	        forwardTemplate.setTemplateType(TemplateType.RATE_FORWARD);
	        forwardTemplate.setSubjectPattern("FW: {originalSubject}");
	        forwardTemplate.setBody("Please upload the rates." + commonFooter);
	        forwardTemplate.setIsActive(true);
	        emailTemplateRepository.save(forwardTemplate);

	        EmailTemplate approveTemplate = new EmailTemplate();
	        approveTemplate.setTemplateType(TemplateType.RATE_APPROVE);
	        approveTemplate.setSubjectPattern("RE: {originalSubject}");
	        approveTemplate.setBody("The rates have been approved. Please proceed with implementation." + commonFooter);
	        approveTemplate.setIsActive(true);
	        emailTemplateRepository.save(approveTemplate);

	        EmailTemplate rejectTemplate = new EmailTemplate();
	        rejectTemplate.setTemplateType(TemplateType.RATE_REJECT);
	        rejectTemplate.setSubjectPattern("RE: {originalSubject}");
	        rejectTemplate.setBody("The rates have been rejected. Please review and resubmit." + commonFooter);
	        rejectTemplate.setIsActive(true);
	        emailTemplateRepository.save(rejectTemplate);
	    }

//	    private void initializeSampleClients() {
//	        ClientEntity client1 = new ClientEntity();
//	        client1.setName("chatlink");
//	        client1.setPrimaryEmail("rates@chatlink.net");
//	        client1.setCcEmails(Arrays.asList("sarthak@chatlink.net", "mohit@chatlink.net", "vinit@gmail.com"));
//	        client1.setAccountManagerEmail("am@nuetechstack.net");
//	        client1.setCompanyAddress("Chatlink Office, Mumbai, India");
//	        client1.setContactPerson("Sarthak Manager");
//	        client1.setPhoneNumber("+91-9876543210");
//	        ClientEntity savedClient1 = clientRepository.save(client1);
//
//	        // Assign trunks to clients
//	        List<Trunk> allTrunks = trunkRepository.findAll();
//	        for (Trunk trunk : allTrunks) {
//	            clientTrunkRepository.save(new ClientTrunk(savedClient1, trunk));
//	        }
//	    }
}
