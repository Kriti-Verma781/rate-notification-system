package com.ncs.ratenotif.component;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

import com.ncs.ratenotif.response.EmailReceiveResponse;

@Component
public class EmailCache {

	 private final CopyOnWriteArrayList<EmailReceiveResponse> list = new CopyOnWriteArrayList<>();
	 
	 private final ConcurrentSkipListMap<Long, EmailReceiveResponse> map = new ConcurrentSkipListMap<>();

	    public void replaceAll(List<EmailReceiveResponse> newList) {
	        list.clear();
	        if (newList != null && !newList.isEmpty()) list.addAll(newList);
	    }

//	    public List<EmailReceiveResponse> getAllSortedLatestFirst() {
//	        return list.stream()
//	                .sorted(Comparator.comparing(EmailReceiveResponse::getSentDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
//	                .collect(Collectors.toList());
//	    }
	    
	    public void addIfNotExists(EmailReceiveResponse r) {
	        long uid = Long.parseLong(r.getUid());
	        map.putIfAbsent(uid, r);
	    }

	    public List<EmailReceiveResponse> getAllSortedLatestFirst() {
	        return map.descendingMap().values().stream().toList();
	    }

//	    public void addIfNotExists(EmailReceiveResponse r) {
//	        boolean exists = list.stream().anyMatch(e -> e.getUid() != null && e.getUid().equals(r.getUid()));
//	        if (!exists) list.add(0, r);
//	    }

	    public void clear() { list.clear(); }
}
