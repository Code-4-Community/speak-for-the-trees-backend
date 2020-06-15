package com.codeforcommunity.dto.team;

import java.util.List;

public class TeamApplicantsResponse {

  private List<TeamApplicant> applicants;

  public TeamApplicantsResponse(List<TeamApplicant> applicants) {
    this.applicants = applicants;
  }

  public List<TeamApplicant> getApplicants() {
    return applicants;
  }
}
