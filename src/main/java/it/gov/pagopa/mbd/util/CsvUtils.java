package it.gov.pagopa.mbd.util;

import com.azure.cosmos.implementation.guava25.base.Strings;
import it.gov.pagopa.mbd.service.model.csv.RecordAlignEnum;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static it.gov.pagopa.mbd.service.model.csv.RecordAlignEnum.ALIGN_LEFT;

public class CsvUtils {

    public static String toFixedLength(Object record, Integer length, RecordAlignEnum align, char separator) {
        String recordString;
        if(record == null) {
            recordString = "";
        } else {
            if (record instanceof String ) {
                recordString = ((String) record);
            } else if (record instanceof Long ) {
                recordString = ((Long) record).toString();
            } else {
                recordString = record.toString();
            }
        }

        if( align.equals(ALIGN_LEFT) ) {
            return Strings.padEnd(recordString, length, separator).substring(0, length);
        } else {
            String recordPadded = Strings.padStart(recordString, length, separator);
            return recordPadded.substring(recordPadded.length() - length);
        }
    }

    public static void writeFile(String filename, byte[] s) {
        try {
            File file = new File(filename);
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(file));
            bw.write(s);
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
