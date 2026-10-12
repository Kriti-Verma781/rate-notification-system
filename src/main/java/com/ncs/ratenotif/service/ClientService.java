package com.ncs.ratenotif.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.ClientTrunk;
import com.ncs.ratenotif.entity.EmailTemplate;
import com.ncs.ratenotif.entity.TemplateType;
import com.ncs.ratenotif.entity.Trunk;
import com.ncs.ratenotif.request.AddTrunkRequest;
import com.ncs.ratenotif.request.ClientCreateRequest;
import com.ncs.ratenotif.response.ClientResponse;

public interface ClientService {

	List<ClientResponse> getAllClients(String username, String role);

	ClientResponse getClientById(Long id, String username, String role);

//	ClientEntity createClientWithDetails(ClientCreateRequest request);

	List<ClientTrunk> assignTrunksToClient(Long clientId, List<Long> trunkIds);

	List<Trunk> getClientTrunks(Long clientId);

	Map<String, Object> getClientFullDetails(Long clientId);

	EmailTemplate getEmailTemplate(Long clientId, Long trunkId, TemplateType templateType);

	List<EmailTemplate> getClientTemplates(Long clientId);

	ClientResponse createClient(ClientCreateRequest request);

	List<Trunk> getAllTrunk();

	Object getAllClient(String username, String role);

	Object createTrunkToClient(AddTrunkRequest request);

	ClientResponse updateClient(ClientCreateRequest request) throws Exception;



}
