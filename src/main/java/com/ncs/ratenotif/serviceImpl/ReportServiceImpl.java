package com.ncs.ratenotif.serviceImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ncs.ratenotif.common.ReportFilterService;
import com.ncs.ratenotif.entity.ClientEntity;
import com.ncs.ratenotif.entity.NocRateUploadedStatus;
import com.ncs.ratenotif.repository.ClientRepository;
import com.ncs.ratenotif.repository.NocRateUploadedStatusRepository;
import com.ncs.ratenotif.request.ReportFilterRequest;
import com.ncs.ratenotif.response.ReportResponse;
import com.ncs.ratenotif.service.ReportService;

@Service
public class ReportServiceImpl implements ReportService {

	@Autowired
	private NocRateUploadedStatusRepository nocRepo;

	@Autowired
	private ClientRepository clientRepo;

	@Override
	public List<ReportResponse> getTodayReport() {

		return nocRepo.findAll(ReportFilterService.todayData()).stream().map(this::mapToResponse).toList();
	}

	@Override
	public List<ReportResponse> getFilteredReport(ReportFilterRequest request) {

		List<NocRateUploadedStatus> data = nocRepo.findAll(ReportFilterService.withFilters(request));

		Map<String, String> clientAmMap =
		        clientRepo.findAll().stream()
		                .collect(Collectors.toMap(
		                        ClientEntity::getName,
		                        ClientEntity::getAmName,
		                        (a, b) -> a
		                ));

		if (request.getAmName() != null && !request.getAmName().isBlank()) {
		    data = data.stream()
		            .filter(d -> request.getAmName()
		                    .equalsIgnoreCase(clientAmMap.get(d.getClientName())))
		            .toList();
		}


		return data.stream().map(this::mapToResponse).toList();
	}

	private ReportResponse mapToResponse(NocRateUploadedStatus entity) {

	    String amName = "NA";

	    ClientEntity client = clientRepo.findByName(entity.getClientName());
	    if (client != null && client.getAmName() != null) {
	        amName = client.getAmName();
	    }

	    return new ReportResponse(
	            entity.getClientName(),
	            entity.getTrunkName(),
	            amName,
	            entity.getNocStatus(),
	            entity.getRoutingStatus(),
	            entity.getClientSendStatus(),
	            entity.getRemark(),
	            entity.getClientAcceptanceStatus(),
	            entity.getCreatedOn()
	    );
	}

}
