package com.codeforcommunity.dto.blocks;

import java.util.List;

public class BlockResponse {
  private List<String> successes;
  private List<String> failures;

  public BlockResponse(List<String> successes, List<String> failures) {
    this.successes = successes;
    this.failures = failures;
  }

  public List<String> getSuccesses() {
    return successes;
  }

  public List<String> getFailures() {
    return failures;
  }
}
