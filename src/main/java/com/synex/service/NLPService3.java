// src/main/java/com/synex/service/NLPService2.java
package com.synex.service;

import com.synex.domain.ActionResult;
import com.synex.domain.ConversationState;

import java.util.List;
import java.util.Map;

/**
 * Extracts exactly the named slots from free-form text,
 * generates follow-up questions, and renders action replies.
 */
public interface NLPService3 {
  /**
   * Pull out exactly the given slot names from userMessage.
   * Returns a map of slotName→stringValue.
   */
  Map<String,String> fillSlots(List<String> slots, String userMessage);

  /**
   * Given which slots are still missing, and the conversation context summary,
   * ask the model to generate one concise follow-up question.
   */
  String generateQuestion(List<String> missingSlots, ConversationState ctx);

  /**
   * Given the structured result of an action, have the model render
   * a 1–2 sentence human-friendly reply incorporating those values.
   */
  String renderActionReply(ActionResult result,
                           ConversationState ctx);
}
