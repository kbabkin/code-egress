package com.bt.code.egress.report;

import com.bt.code.egress.process.TextMatched;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor
@Slf4j
public class ReportCollector implements TextMatched.Listener {
    private final ReportHelper reportHelper;
    private final Collection<Report.ReportLine> reportLines = new ConcurrentLinkedQueue<>();

    @Override
    public void onMatched(TextMatched textMatched) {
        log.info("Matched: {}", textMatched);
        reportLines.add(new Report.ReportLine(
                textMatched.getAllowed(),
                textMatched.getLineToken().getWordLowerCase(),
                reportHelper.getContext(textMatched.getLineToken()),
                textMatched.getLineLocation().getFile(),
                textMatched.getLineLocation().getLineNum(),
                textMatched.getReplacement(),
                textMatched.getComment()
        ));
    }

    public Report toReport() {
        return new Report(new ArrayList<>(reportLines));
    }
}
