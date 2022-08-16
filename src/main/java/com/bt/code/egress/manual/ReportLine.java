package com.bt.code.egress.manual;

import lombok.Value;

@Value
public class ReportLine {
    Boolean correct;
    String word;
    String context;
    String file;
    Integer line;
    String replacement;
    String comment;
}
