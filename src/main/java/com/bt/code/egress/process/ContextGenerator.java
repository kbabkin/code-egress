package com.bt.code.egress.process;

import com.bt.code.egress.read.LineToken;

@FunctionalInterface
public interface ContextGenerator {
    String getContext(LineToken lineToken);
}
