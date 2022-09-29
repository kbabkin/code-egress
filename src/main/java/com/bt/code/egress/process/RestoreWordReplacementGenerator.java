package com.bt.code.egress.process;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
public class RestoreWordReplacementGenerator extends WordReplacementGenerator {
    private final Map<String, Set<String>> replacementsByWord;

    public RestoreWordReplacementGenerator(Map<String, Set<String>> replacementsByWord) {
        super("NOT USED");
        this.replacementsByWord = replacementsByWord;
    }

    @Override
    protected String generate(String word, String template) {
        //todo template - from word-guard?
        Set<String> set = replacementsByWord.get(word);
        if (set != null && set.size() == 1) {
            return set.iterator().next();
        }
        return set == null || set.isEmpty()
                ? "<<NO_RESTORE:" + word + ">>"
                : "<<AMBIGUOUS:" + word + ":" + String.join("|", set) + ">>";
    }

}
