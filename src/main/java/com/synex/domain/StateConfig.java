// src/main/java/com/synex/config/StateConfig.java
package com.synex.domain;

import com.synex.domain.ConversationState;
import com.synex.domain.ActionResult;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class StateConfig {
    private final List<String> slotsNeeded;
    private final BiFunction<ConversationState,Map<String,String>,ActionResult> action;
    private final Function<ConversationState,ConversationState.Stage> nextState;
    private final Function<ConversationState,List<String>> suggestions;

    public StateConfig(
      List<String> slotsNeeded,
      BiFunction<ConversationState,Map<String,String>,ActionResult> action,
      Function<ConversationState,ConversationState.Stage> nextState,
      Function<ConversationState,List<String>> suggestions
    ) {
        this.slotsNeeded = slotsNeeded;
        this.action      = action;
        this.nextState   = nextState;
        this.suggestions = suggestions;
    }

    public List<String> slotsNeeded() { return slotsNeeded; }
    public BiFunction<ConversationState,Map<String,String>,ActionResult> action() { return action; }
    public Function<ConversationState,ConversationState.Stage> nextState() { return nextState; }
    public Function<ConversationState,List<String>> suggestions() { return suggestions; }
}
