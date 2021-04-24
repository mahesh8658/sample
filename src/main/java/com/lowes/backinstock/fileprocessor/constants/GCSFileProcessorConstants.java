package com.lowes.backinstock.fileprocessor.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GCSFileProcessorConstants {

    public static final String SUCCESS_2XX = "2XX_SUCCESS";
    public static final String FAILURES_4XX = "4XX_FAILURES";
    public static final String FAILURES_5XX = "5XX_FAILURES";
    public static final String CSV_FILE_EXT = ".csv";
    public static final String UNDERSCORE_DELIM = "_";
    public static final String STORE_ID_MISSING = "STORE_ID_MISSING";
    public static final String OMNI_ITEM_ID_MISSING = "OMNI_ITEM_ID_MISSING";
    public static final String EMAIL_ID_MISSING = "EMAIL_ID_MISSING";
    public static final String INVALID_RECORDS = "INVALID_RECORDS";
    public static final String VALID_RECORDS = "VALID_RECORDS";
    public static final String BLOB_PATH_TEMPLATE = "gs://%s/%s";
    public static final String LOWES_PREFIX = "LOWES_";
}
