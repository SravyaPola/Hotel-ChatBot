package com.synex.domain;

import java.util.Map;

/**
 * Never exposed directly to the user. Just a carrier for structured data from
 * your action lambdas.
 */
public record ActionResult(String actionName, Map<String, Object> data) {
}
