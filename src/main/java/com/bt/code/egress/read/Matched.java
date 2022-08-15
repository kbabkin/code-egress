package com.bt.code.egress.read;

import lombok.Value;

@Value
public class Matched {
    LineLocation lineLocation;
    LineToken lineToken;
    String replacement;
    String comment;
}
