package com.ncs.ratenotif.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentResponse {

	private String fileName;
    private byte[] data;
    private MediaType mediaType;

}
