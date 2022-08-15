package com.bt.code.egress.manual;

import lombok.Value;

@Value
public class ReportLine {
    String word;
    String file;
    Integer line;
    Boolean correct;
    String context;
    String lineHash;
    String comment;
}
