package com.lowes.backinstock.fileprocessor.mapper;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.stereotype.Service;

@Service
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UnSubscribeFileToKillSwitchRequest {

    @CsvBindByName(column = "OmniID")
    private String omniItemId;
    @CsvBindByName(column = "StoreNumber")
    private String storeId;
    @CsvBindByName(column = "dataAdded")
    private String dataAdded;
    @CsvBindByName(column = "ItemNumber")
    private String itemNumber;
    @CsvBindByName(column = "email")
    private String emailId;
    @CsvBindByName
    private String errorMessage;
}
