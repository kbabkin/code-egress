package com.bt.code.egress.sync;

import lombok.Value;

@Value
public class ChangeAction {
    public enum ChangeType {
        COPY,
        DELETE
    }

    private final ChangeType type;
    private final String path;

    public static ChangeAction copy(String path) {
        return new ChangeAction(ChangeType.COPY, path);
    }

    public static ChangeAction delete(String path) {
        return new ChangeAction(ChangeType.DELETE, path);
    }
}