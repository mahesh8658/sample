package com.lowes.backinstock.fileprocessor.dto.request;

import lombok.Data;

@Data
public class KillSwitchRequest {
	private String emailId;
	private String omniItemId;
	private String storeId;
	private String dataAdded;
	private String itemNumber;
}
