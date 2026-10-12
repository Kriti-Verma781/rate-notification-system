package com.ncs.ratenotif.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.ncs.ratenotif.entity.NocRateUploadedStatus;
import com.ncs.ratenotif.request.ReportFilterRequest;

import jakarta.persistence.criteria.Predicate;

public class ReportFilterService {

    public static Specification<NocRateUploadedStatus> withFilters(ReportFilterRequest filter) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getClientName() != null) {
                predicates.add(cb.equal(root.get("clientName"), filter.getClientName()));
            }

            if (filter.getTrunkName() != null) {
                predicates.add(cb.equal(root.get("trunkName"), filter.getTrunkName()));
            }

            if (filter.getFromDate() != null && filter.getToDate() != null) {
                predicates.add(cb.between(
                        root.get("createdOn"),
                        filter.getFromDate().atStartOfDay(),
                        filter.getToDate().atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<NocRateUploadedStatus> todayData() {

        LocalDate today = LocalDate.now();

        return (root, query, cb) ->
                cb.between(
                        root.get("createdOn"),
                        today.atStartOfDay(),
                        today.atTime(LocalTime.MAX));
    }
}

