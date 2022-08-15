package com.bt.code.egress.process;

import com.bt.code.egress.read.Matched;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Picker {
    public void pick(Matched matched) {
        log.info("Matched: {}", matched);
    }
}
