package com.codeforcommunity.processor;

import com.codeforcommunity.api.IBlockInfoProcessor;
import com.codeforcommunity.dto.blockInfo.BlockInfoResponse;
import com.codeforcommunity.dto.blockInfo.BlockLeaderboardResponse;
import com.codeforcommunity.dto.blockInfo.Individual;
import com.codeforcommunity.dto.blockInfo.Team;
import com.codeforcommunity.enums.BlockStatus;
import java.util.List;
import org.jooq.DSLContext;
import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;
import static org.jooq.impl.DSL.count;

public class BlockInfoProcessorImpl implements IBlockInfoProcessor {
  private final DSLContext db;

  public BlockInfoProcessorImpl(DSLContext db) {
    this.db = db;
  }

  @Override
  public BlockInfoResponse getBlocks() {
    Integer openBlocks = db.select(count(BLOCK.STATUS)).from(BLOCK)
        .where(BLOCK.STATUS.eq(BlockStatus.OPEN)).fetchOneInto(Integer.class);
    Integer doneBlocks = db.select(count(BLOCK.STATUS)).from(BLOCK)
        .where(BLOCK.STATUS.eq(BlockStatus.DONE)).fetchOneInto(Integer.class);
    Integer assignedBlocks = db.select(count(BLOCK.STATUS)).from(BLOCK)
        .where(BLOCK.STATUS.eq(BlockStatus.RESERVED)).fetchOneInto(Integer.class);
    return new BlockInfoResponse(doneBlocks, assignedBlocks, openBlocks);
  }

  @Override
  public BlockLeaderboardResponse getBlockLeaderboards() {
    org.jooq.generated.tables.Block reserved = BLOCK.as("reserved");
    org.jooq.generated.tables.Block completed = BLOCK.as("completed");

    List<Team> teams =
        db.select(TEAM.ID, TEAM.NAME, count(completed.FID).as("blocksCompleted"), count(reserved.FID).as("blocksReserved"))
            .from(TEAM)
            .join(USER_TEAM).on(USER_TEAM.TEAM_ID.eq(TEAM.ID))
            .leftJoin(reserved).on(USER_TEAM.USER_ID.eq(reserved.ASSIGNED_TO).and(reserved.STATUS.eq(BlockStatus.RESERVED)))
            .leftJoin(completed).on(USER_TEAM.USER_ID.eq(completed.ASSIGNED_TO).and(completed.STATUS.eq(BlockStatus.DONE)))
            .groupBy(TEAM.ID)
            .fetchInto(Team.class);

    List<Individual> individuals =
        db.select(USERS.ID, USERS.USERNAME, count(completed.FID).as("blocksCompleted"), count(reserved.FID).as("blocksReserved"))
            .from(USERS)
            .leftJoin(reserved).on(USERS.ID.eq(reserved.ASSIGNED_TO).and(reserved.STATUS.eq(BlockStatus.RESERVED)))
            .leftJoin(completed).on(USERS.ID.eq(completed.ASSIGNED_TO).and(completed.STATUS.eq(BlockStatus.DONE)))
            .groupBy(USERS.ID)
            .fetchInto(Individual.class);

    return new BlockLeaderboardResponse(teams, individuals);
  }
}
