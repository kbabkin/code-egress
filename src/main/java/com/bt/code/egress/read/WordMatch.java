package com.bt.code.egress.read;

import lombok.Value;

@Value
public class WordMatch {
    LineToken lineToken;
    String reason;
}
