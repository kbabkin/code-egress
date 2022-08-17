package com.bt.code.egress.report;

import com.bt.code.egress.process.Matched;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class ReportCollector implements Matched.Listener {
    private final ReportHelper reportHelper;
    private final List<Report.ReportLine> reportLines = new ArrayList<>();

    @Override
    public void onMatched(Matched matched) {
        log.info("Matched: {}", matched);
        reportLines.add(new Report.ReportLine(
                matched.getAllowed(),
                matched.getLineToken().getWordLowerCase(),
                reportHelper.getContext(matched.getLineToken()),
                matched.getLineLocation().getFile(),
                matched.getLineLocation().getLineNum(),
                matched.getReplacement(),
                matched.getComment()
        ));
    }

    public Report toReport() {
        return new Report(Collections.unmodifiableList(reportLines));
    }
}
