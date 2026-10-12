package com.ncs.ratenotif.serviceImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.ClientRateSheetEntity;
import com.ncs.ratenotif.entity.ClientTrunk;
import com.ncs.ratenotif.entity.EmailTemplate;
import com.ncs.ratenotif.entity.TemplateType;
import com.ncs.ratenotif.entity.Trunk;
import com.ncs.ratenotif.repository.*;
import com.ncs.ratenotif.request.AddTrunkRequest;
import com.ncs.ratenotif.request.ClientCreateRequest;
import com.ncs.ratenotif.response.ClientResponse;
import com.ncs.ratenotif.response.RateSheetResponse;
import com.ncs.ratenotif.response.TrunkResponse;
import com.ncs.ratenotif.service.ClientService;
import com.ncs.ratenotif.service.ExcelGenerationService;
import com.ncs.ratenotif.user.entity.UserEntity;
import com.ncs.ratenotif.user.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class ClientServiceImpl implements ClientService{

    private final ClientRateSheetRepository clientRateSheetRepository;
	
	@Autowired
	private ClientRepository clientRepository;
	
    @Autowired
    private TrunkRepository trunkRepository;
    
    @Autowired
    private ClientTrunkRepository clientTrunkRepository;
    
    @Autowired
    private EmailTemplateRepository emailTemplateRepository;
    
    @Autowired
    private ExcelGenerationService excelGenerationService;
    
    @Autowired
    private UserRepository userRepository;

    ClientServiceImpl(ClientRateSheetRepository clientRateSheetRepository) {
        this.clientRateSheetRepository = clientRateSheetRepository;
    }

//    @Override
//    public List<ClientEntity> getAllClients() {
//        return clientRepository.findAll();
//    }
//
//    @Override
//    public Optional<ClientEntity> getClientById(Long id) {
//        return clientRepository.findById(id);
//    }

//    @Override
//    @Transactional
//    public Object createClient(ClientEntity client) {
//        return clientRepository.save(client);
//    }

//    @Override
//    @Transactional
//    public ClientEntity createClientWithDetails(ClientCreateRequest request) {
//        ClientEntity client = new ClientEntity();
//        client.setName(request.getName());
//        client.setPrimaryEmail(request.getPrimaryEmail());
//        client.setCcEmails(request.getCcEmails());
//        client.setAccountManagerEmail(request.getAccountManagerEmail());
//        client.setCompanyAddress(request.getCompanyAddress());
//        client.setContactPerson(request.getContactPerson());
//        client.setPhoneNumber(request.getPhoneNumber());
//        
//        ClientEntity savedClient = clientRepository.save(client);
//        
//        if (request.getTrunkIds() != null && !request.getTrunkIds().isEmpty()) {
//            assignTrunksToClient(savedClient.getId(), request.getTrunkIds());
//        }
//        
//        return savedClient;
//    }

    @Override
    @Transactional
    public List<ClientTrunk> assignTrunksToClient(Long clientId, List<Long> trunkIds) {
        ClientEntity client = clientRepository.findById(clientId)
            .orElseThrow(() -> new RuntimeException("Client not found"));
        
        List<ClientTrunk> assignedTrunks = new ArrayList<>();
        
        for (Long trunkId : trunkIds) {
            Trunk trunk = trunkRepository.findById(trunkId)
                .orElseThrow(() -> new RuntimeException("Trunk not found: " + trunkId));
            
            if (!clientTrunkRepository.existsByClientIdAndTrunkId(clientId, trunkId)) {

                ClientTrunk clientTrunk = new ClientTrunk(client, trunk);
                clientTrunk.setIsActive(true);
                assignedTrunks.add(clientTrunkRepository.save(clientTrunk));

                generateAndSaveRateSheet(client, trunk, new ArrayList<>());

                createTemplatesForClientTrunk(client, trunk);
            }
        }
        
        return assignedTrunks;
    }


    private void createTemplatesForClientTrunk(ClientEntity client, Trunk trunk) {
        for (TemplateType templateType : TemplateType.values()) {
            EmailTemplate template = new EmailTemplate();
            template.setClient(client);
            template.setTrunk(trunk);
            template.setTemplateType(templateType);
            template.setSubjectPattern(generateDefaultSubject(templateType, client.getName(), trunk.getName()));
            template.setBody(generateDefaultBody(templateType, client.getName(), trunk.getName()));
            template.setIsActive(true);
            
            emailTemplateRepository.save(template);
        }
    }

    private String generateDefaultSubject(TemplateType templateType, String clientName, String trunkName) {
        switch (templateType) {
            case RATE_SEND: return "NUETECH - " + trunkName + " Rate Notification to " + clientName;
            case RATE_ACCEPT: return "RE: NUETECH - " + trunkName + " Rate Notification to " + clientName + " - Accepted";
            case RATE_REJECT: return "RE: NUETECH - " + trunkName + " Rate Notification to " + clientName + " - Rejected";
            case RATE_APPROVE: return "Approval Required: " + trunkName + " Rates for " + clientName;
            case RATE_FORWARD: return "FWD: " + trunkName + " Rates for " + clientName;
            default: return "NUETECH Rate Notification";
        }
    }

    private String generateDefaultBody(TemplateType templateType, String clientName, String trunkName) {
        String baseBody = "Dear " + clientName + " Team,\n\n";
        
        switch (templateType) {
            case RATE_SEND: return baseBody + "Please find attached our latest " + trunkName + " rates.\n\nBest regards,\nNUETECH Team";
            case RATE_ACCEPT: return baseBody + "We accept the proposed " + trunkName + " rates. Please proceed with implementation.\n\nBest regards,\nNUETECH Team";
            case RATE_REJECT: return baseBody + "We need to discuss the proposed " + trunkName + " rates. Please contact us.\n\nBest regards,\nNUETECH Team";
            case RATE_APPROVE: return "Hello Account Manager,\n\nPlease review and approve the " + trunkName + " rates for " + clientName + ".\n\nThank you.";
            case RATE_FORWARD: return baseBody + "Forwarding the rate information as requested.\n\nBest regards,\nNUETECH Team";
            default: return baseBody + "Thank you.\n\nNUETECH Team";
        }
    }

    @Override
    public List<Trunk> getClientTrunks(Long clientId) {
        return clientTrunkRepository.findTrunksByClientId(clientId);
    }

    @Override
    public Map<String, Object> getClientFullDetails(Long clientId) {
        ClientEntity client = clientRepository.findById(clientId)
            .orElseThrow(() -> new RuntimeException("Client not found"));
        
        List<Trunk> trunks = getClientTrunks(clientId);
        List<EmailTemplate> templates = emailTemplateRepository.findByClientId(clientId);
        
        Map<String, Object> details = new HashMap<>();
        details.put("client", client);
        details.put("trunks", trunks);
        details.put("templates", templates);
        
        return details;
    }

    @Override
    public EmailTemplate getEmailTemplate(Long clientId, Long trunkId, TemplateType templateType) {
        return emailTemplateRepository.findByClientIdAndTrunkIdAndTemplateType(clientId, trunkId, templateType)
            .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    @Override
    public List<EmailTemplate> getClientTemplates(Long clientId) {
        return emailTemplateRepository.findByClientId(clientId);
    }
    
    @Override
    @Transactional
    public ClientResponse createClient(ClientCreateRequest request) {
    	ClientEntity existingClient = clientRepository.findByName(request.getName());
	    if (existingClient != null) {
	        throw new RuntimeException("Client already exists");
	    }
        try {
            ClientEntity client = new ClientEntity();
            client.setName(request.getName());
            client.setPrimaryEmail(request.getPrimaryEmail());
            client.setCcEmails(request.getCcEmails() != null ? request.getCcEmails() : new ArrayList<>());
            client.setAccountManagerEmail(request.getAccountManagerEmail());
            client.setAmName(request.getAmName());

            ClientEntity savedClient = clientRepository.save(client);
            
            clientRepository.flush();

            List<TrunkResponse> trunkResponses = new ArrayList<>();
            List<RateSheetResponse> rateSheetResponses = new ArrayList<>();

            if (request.getTrunkIds() != null && !request.getTrunkIds().isEmpty()) {
                for (Long trunkId : request.getTrunkIds()) {
                    Trunk trunk = trunkRepository.findById(trunkId)
                        .orElseThrow(() -> new RuntimeException("Trunk not found with id: " + trunkId));

                    ClientTrunk clientTrunk = new ClientTrunk(savedClient, trunk);
                    clientTrunkRepository.save(clientTrunk);

                    generateAndSaveRateSheet(savedClient, trunk, rateSheetResponses);

                    trunkResponses.add(new TrunkResponse(trunk.getId(), trunk.getName(), trunk.getPrefix(), LocalDateTime.now()));
                }
            }

            return buildClientResponse(savedClient, trunkResponses, rateSheetResponses);
            
        } catch (Exception e) {
            throw new RuntimeException("Error creating client: " + e.getMessage(), e);
        }
    }

    private void generateAndSaveRateSheet(ClientEntity client, Trunk trunk, List<RateSheetResponse> rateSheetResponses) {
        try {
            byte[] excelContent = excelGenerationService.generateRateSheetExcel(trunk);
            String fileName = "Nuetech- " + trunk.getName() + " Rate Notification to " + client.getName() + ".xlsx";
            
            ClientRateSheetEntity rateSheet = new ClientRateSheetEntity();
            rateSheet.setClient(client);
            rateSheet.setTrunk(trunk);
            rateSheet.setFileName(fileName);
            rateSheet.setFileContent(excelContent);
            rateSheet.setCreatedDate(LocalDateTime.now());
            
            ClientRateSheetEntity savedRateSheet = clientRateSheetRepository.save(rateSheet);
            clientRateSheetRepository.flush(); // Force flush to avoid transaction conflicts

            rateSheetResponses.add(new RateSheetResponse(
                savedRateSheet.getId(), 
                fileName, 
                trunk.getName(), 
                savedRateSheet.getCreatedDate()
            ));
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel for trunk: " + trunk.getName(), e);
        }
    }

    private ClientResponse buildClientResponse(ClientEntity client, 
                                             List<TrunkResponse> trunkResponses, 
                                             List<RateSheetResponse> rateSheetResponses) {
        ClientResponse response = new ClientResponse();
        response.setId(client.getId());
        response.setName(client.getName());
        response.setPrimaryEmail(client.getPrimaryEmail());
        response.setCcEmails(client.getCcEmails());
        response.setAccountManagerEmail(client.getAccountManagerEmail());
        response.setTrunks(trunkResponses);
        response.setRateSheets(rateSheetResponses);
        response.setAmName(client.getAmName());
        return response;
    }

//    @Transactional()
//    public List<ClientResponse> getAllClients() {
//        List<ClientEntity> clients = clientRepository.findAll();
//        return clients.stream().map(this::convertToResponse).collect(Collectors.toList());
//    }
    
    @Transactional
    public List<ClientResponse> getAllClients(String username, String role) {

        List<ClientEntity> clients;

        if ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role)) {
            clients = clientRepository.findAllByOrderByNameDesc();
        } 
        else if ("AM".equalsIgnoreCase(role)) {

            UserEntity user = userRepository.findByUsername(username);
            if (user == null) {
//                throw new UsernameNotFoundException("User not found");
            }

            String accountManagerName = user.getFullName();
            clients = clientRepository.findByAmNameOrderByIdDesc(accountManagerName);
        } 
        else {
            throw new RuntimeException("Unauthorized role");
        }

        return clients.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }


    @Transactional
    public ClientResponse getClientById(Long id, String username, String role) {

        ClientEntity client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + id));

        if ("AM".equalsIgnoreCase(role)) {
            UserEntity user = userRepository.findByUsername(username);
            if (user == null) {
//                throw new UsernameNotFoundException("User not found");
            }

            if (!user.getFullName().equalsIgnoreCase(client.getAmName())) {
                throw new RuntimeException("Access denied for this client");
            }
        }

        return convertToResponse(client);
    }


    private ClientResponse convertToResponse(ClientEntity client) {
        ClientResponse response = new ClientResponse();
        response.setId(client.getId());
        response.setName(client.getName());
        response.setPrimaryEmail(client.getPrimaryEmail());
        response.setCcEmails(client.getCcEmails());
        response.setAccountManagerEmail(client.getAccountManagerEmail());
        response.setAmName(client.getAmName());

        List<TrunkResponse> trunkResponses = client.getClientTrunks().stream()
            .map(ct -> new TrunkResponse(ct.getTrunk().getId(), ct.getTrunk().getName(), ct.getTrunk().getPrefix(), LocalDateTime.now()))
            .collect(Collectors.toList());
        response.setTrunks(trunkResponses);

        List<RateSheetResponse> rateSheetResponses = clientRateSheetRepository.findByClientId(client.getId()).stream()
            .map(rs -> new RateSheetResponse(rs.getId(), rs.getFileName(), rs.getTrunk().getName(), rs.getCreatedDate()))
            .collect(Collectors.toList());
        response.setRateSheets(rateSheetResponses);

        return response;
    }
    
    @Override
    public List<Trunk> getAllTrunk(){
    	return trunkRepository.findAll();
    }
    
    @Override
    public List<Map<String, Object>> getAllClient(String username, String role) {

    	 if ("ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role)) {
    	        return clientRepository.findAllByOrderByNameDesc()
    	                .stream()
    	                .map(c -> {
    	                    Map<String, Object> map = new HashMap<>();
    	                    map.put("id", c.getId());
    	                    map.put("name", c.getName());
    	                    return map;
    	                })
    	                .collect(Collectors.toList());
    	    }

        UserEntity user = userRepository.findByUsername(username);
        if (user == null) {
//            throw new UsernameNotFoundException("User not found");
        }

        String accountManagerName = user.getFullName();

        return clientRepository.findByAmNameOrderByNameDesc(accountManagerName)
                .stream()
                .map(c -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", c.getId());
                    map.put("name", c.getName());
                    return map;
                })
                .collect(Collectors.toList());
    }
    
    
    @Override
    @Transactional
    public Object createTrunkToClient(AddTrunkRequest request) {

        ClientEntity client = null;

        if (request.getClientName() != null) {
            client = clientRepository.findByName(request.getClientName());
            if (client == null)
                throw new RuntimeException("Client not found with name: " + request.getClientName());
        } 
        else {
            throw new RuntimeException("Please pass either clientId or clientName");
        }

        List<TrunkResponse> trunkResponses = new ArrayList<>();
        List<RateSheetResponse> rateSheetResponses = new ArrayList<>();

        for (Long trunkId : request.getTrunkIds()) {

            Trunk trunk = trunkRepository.findById(trunkId)
                    .orElseThrow(() -> new RuntimeException("Trunk not found with id: " + trunkId));

            boolean alreadyExist = clientTrunkRepository.existsByClientIdAndTrunkId(client.getId(), trunkId);
            if (alreadyExist) {
                continue; 
            }

            ClientTrunk clientTrunk = new ClientTrunk(client, trunk);
            clientTrunkRepository.save(clientTrunk);

            generateAndSaveRateSheet(client, trunk, rateSheetResponses);

            trunkResponses.add(new TrunkResponse(
                    trunk.getId(),
                    trunk.getName(),
                    trunk.getPrefix(),
                    LocalDateTime.now()
            ));
        }

        return buildClientResponse(client, trunkResponses, rateSheetResponses);
    }
    
    @Override
    public ClientResponse updateClient(ClientCreateRequest request) throws Exception {

        ClientEntity client = clientRepository.findByName(request.getName());
        if (client == null) {
            throw new Exception(
                "Client not found with name: " + request.getName()
            );
        }

        applyUpdates(client, request);

        ClientEntity updatedClient = clientRepository.save(client);

        return buildClientResponse(updatedClient, List.of(), List.of());
    }

    private void applyUpdates(ClientEntity client, ClientCreateRequest request) {

    	if (request.getName() != null) {
            client.setName(request.getName());
        }
    	
        if (request.getPrimaryEmail() != null) {
            client.setPrimaryEmail(request.getPrimaryEmail());
        }

        if (request.getCcEmails() != null) {
            client.setCcEmails(request.getCcEmails());
        }

        if (request.getAmName() != null) {
            client.setAmName(request.getAmName());
        }

        if (request.getAccountManagerEmail() != null) {
            client.setAccountManagerEmail(request.getAccountManagerEmail());
        }
    }
    
}