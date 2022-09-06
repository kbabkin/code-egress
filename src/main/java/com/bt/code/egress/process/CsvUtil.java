package com.bt.code.egress.process;

import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class CsvUtil {
    public List<String> fixNulls(List<String> originalRecord) {
        List<String> fixedRecord = Lists.newArrayList(originalRecord);
        for (int i = 0; i < fixedRecord.size(); i++) {
            if ("null".equalsIgnoreCase(fixedRecord.get(i))) {
                fixedRecord.set(i, "");
            }
        }
        return fixedRecord;
    }
}