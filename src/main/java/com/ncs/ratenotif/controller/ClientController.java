package com.ncs.ratenotif.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.ClientTrunk;
import com.ncs.ratenotif.entity.EmailTemplate;
import com.ncs.ratenotif.entity.TemplateType;
import com.ncs.ratenotif.entity.Trunk;
import com.ncs.ratenotif.request.AddTrunkRequest;
import com.ncs.ratenotif.request.ClientCreateRequest;
import com.ncs.ratenotif.response.ClientResponse;
import com.ncs.ratenotif.response.dto.ResponseDTO;
import com.ncs.ratenotif.response.dto.ResponseUtil;
import com.ncs.ratenotif.service.ClientService;

@RestController
@RequestMapping("/client")
@CrossOrigin(origins = "http://localhost:3000")
public class ClientController {

	@Autowired
	private ClientService clientService;

//    @GetMapping
//    public List<ClientEntity> getAllClients() {
//        return clientService.getAllClients();
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ClientEntity> getClientById(@PathVariable Long id) {
//        return clientService.getClientById(id)
//            .map(ResponseEntity::ok)
//            .orElse(ResponseEntity.notFound().build());
//    }

//    @PostMapping
//    public ClientEntity createClient(@RequestBody ClientEntity client) {
//        return clientService.createClient(client);
//    }

//    @PostMapping("/create-with-details")
//    public ResponseEntity<?> createClientWithDetails(@RequestBody ClientCreateRequest request) {
//        try {
//            ClientEntity client = clientService.createClientWithDetails(request);
//            return ResponseEntity.ok(client);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(e.getMessage());
//        }
//    }

	@PostMapping("/trunkToClient/{clientId}")
	public ResponseEntity<?> assignTrunksToClient(@PathVariable Long clientId, @RequestBody List<Long> trunkIds) {
		try {
			List<ClientTrunk> assignedTrunks = clientService.assignTrunksToClient(clientId, trunkIds);
			return ResponseEntity.ok(assignedTrunks);
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("/getClientTrunk/{clientId}")
	public List<Trunk> getClientTrunks(@PathVariable Long clientId) {
		return clientService.getClientTrunks(clientId);
	}

	@GetMapping("/{clientId}/full-details")
	public ResponseEntity<?> getClientFullDetails(@PathVariable Long clientId) {
		try {
			Map<String, Object> clientDetails = clientService.getClientFullDetails(clientId);
			return ResponseEntity.ok(clientDetails);
		} catch (RuntimeException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@GetMapping("/{clientId}/templates")
	public List<EmailTemplate> getClientTemplates(@PathVariable Long clientId) {
		return clientService.getClientTemplates(clientId);
	}

	@GetMapping("/{clientId}/templates/{trunkId}/{templateType}")
	public ResponseEntity<EmailTemplate> getEmailTemplate(@PathVariable Long clientId, @PathVariable Long trunkId,
			@PathVariable TemplateType templateType) {
		try {
			EmailTemplate template = clientService.getEmailTemplate(clientId, trunkId, templateType);
			return ResponseEntity.ok(template);
		} catch (RuntimeException e) {
			return ResponseEntity.notFound().build();
		}
	}

	@PostMapping("/createClient")
	public ResponseEntity<Map<String, Object>> createClient(@RequestBody ClientCreateRequest request) {
		Map<String, Object> response = new HashMap<>();
		try {
			ClientResponse clientResponse = clientService.createClient(request);
			response.put("success", true);
			response.put("message", "Client created successfully with rate sheets");
			response.put("data", clientResponse);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.put("success", false);
			response.put("message", "Error creating client: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/getAllClient/{userName}/{role}")
	public ResponseEntity<Map<String, Object>> getAllClients(@PathVariable String userName, @PathVariable String role) {
		Map<String, Object> response = new HashMap<>();
		try {
			List<ClientResponse> clients = clientService.getAllClients(userName, role);
			response.put("success", true);
			response.put("data", clients);
			response.put("count", clients.size());
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.put("success", false);
			response.put("message", "Error fetching clients: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/getClientById/{id}/{userName}/{role}")
	public ResponseEntity<Map<String, Object>> getClientById(@PathVariable Long id, @PathVariable String userName,
			@PathVariable String role) {
		Map<String, Object> response = new HashMap<>();
		try {
			ClientResponse client = clientService.getClientById(id, userName, role);
			response.put("success", true);
			response.put("data", client);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.put("success", false);
			response.put("message", "Error fetching client: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
		}
	}

	@GetMapping("/getTrunk")
	public List<Trunk> getAllTrunk() {
		return clientService.getAllTrunk();
	}

	@GetMapping("/getClient/{userName}/{role}")
	public ResponseEntity<ResponseDTO<Object>> createData(@PathVariable String userName, @PathVariable String role) {
		try {
			Object savedDeal = clientService.getAllClient(userName, role);

			return ResponseUtil.createSuccessMessage(savedDeal, "Data Found", HttpStatus.FOUND);

		} catch (Exception e) {

			return ResponseUtil.createErrorMessage(null, e.getMessage(), HttpStatus.NOT_FOUND);
		}
	}

	@PostMapping("/addTrunkToClient")
	public ResponseEntity<ResponseDTO<Object>> createTrunkToClient(@RequestBody AddTrunkRequest req) {
		try {
			Object savedDeal = clientService.createTrunkToClient(req);

			return ResponseUtil.createSuccessMessage(savedDeal, "Trunks added to client successfully", HttpStatus.OK);

		} catch (Exception e) {

			return ResponseUtil.createErrorMessage(null, e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@PutMapping("/updateClient")
	public ResponseEntity<ResponseDTO<ClientResponse>> updateClient(@RequestBody ClientCreateRequest request) {

		ClientResponse response;
		try {
			response = clientService.updateClient(request);
			return ResponseUtil.createSuccessMessage(response, "Client updated successfully", HttpStatus.ACCEPTED);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseUtil.createErrorMessage(null, "Client not updated", HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}
}
