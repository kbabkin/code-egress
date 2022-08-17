package com.bt.code.egress.report;

import lombok.Value;

@Value
public class ReportLine {
    Boolean allow;
    String word;
    String context;
    String file;
    Integer line;
    String replacement;
    String comment;
}
