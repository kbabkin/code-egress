package com.bt.code.egress.report;

import lombok.Value;

import java.util.List;

@Value
public class Report {
    List<ReportLine> reportLines;

    @Value
    public static class ReportLine {
        Boolean allow;
        String word;
        String context;
        String file;
        Integer line;
        String replacement;
        String comment;
    }

    @FunctionalInterface
    public interface Listener {
        void onReport(Report report);
    }

}
