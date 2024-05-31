package it.gov.pagopa.mbd.util;

import com.azure.cosmos.implementation.guava25.base.Strings;
import it.gov.pagopa.mbd.service.model.csv.RecordAlignEnum;

import static it.gov.pagopa.mbd.service.model.csv.RecordAlignEnum.ALIGN_LEFT;

public class CsvUtils {

    public static String toFixedLength(Object record, Integer length, RecordAlignEnum align, char separator) {
        String recordString;
        if (record instanceof String ) {
            recordString = ((String) record);
        } else if (record instanceof Long ) {
            recordString = ((Long) record).toString();
        } else {
            recordString = record.toString();
        }
        if( align.equals(ALIGN_LEFT) ) {
            return Strings.padEnd(recordString, length, separator).substring(0, length);
        } else {
            String recordPadded = Strings.padStart(recordString, length, separator);
            return recordPadded.substring(recordPadded.length() - length);
        }
    }

}
