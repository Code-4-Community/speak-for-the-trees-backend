package com.codeforcommunity.dto.team;

import java.util.List;

public class TeamResponse {
  private int id;
  private String name;
  private int blocksCompleted;
  private int blocksReserved;
  List<TeamMember> members;
}
