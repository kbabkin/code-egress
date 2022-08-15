package com.bt.code.egress.read;

import lombok.Value;

@Value
public class LineLocation {
    String file;
    Integer lineNum;
}
