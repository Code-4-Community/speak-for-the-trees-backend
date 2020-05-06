package com.codeforcommunity.processor;

import com.codeforcommunity.api.IBlockInfoProcessor;
import com.codeforcommunity.dto.blockInfo.BlockInfoResponse;
import com.codeforcommunity.dto.blockInfo.BlockLeaderboardResponse;
import com.codeforcommunity.dto.blockInfo.Individual;
import com.codeforcommunity.dto.blockInfo.Team;
import com.codeforcommunity.enums.BlockStatus;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.Select;
import org.jooq.Table;

import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.select;

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

    Select<Record4<Integer, String, Integer, Integer>> userSub =
        db.select(USERS.ID, USERS.USERNAME, completed.ASSIGNED_TO.as("blocksCompleted"),
            inline(null, reserved.ASSIGNED_TO).as("blocksReserved"))
            .from(USERS)
            .leftJoin(completed).on(USERS.ID.eq(completed.ASSIGNED_TO)
            .and(completed.STATUS.eq(BlockStatus.DONE)))
            .union(
                select(USERS.ID, USERS.USERNAME, inline(null, completed.ASSIGNED_TO)
                    .as("blocksCompleted"), reserved.ASSIGNED_TO.as("blocksReserved"))
                    .from(USERS)
                    .leftJoin(reserved).on(USERS.ID.eq(reserved.ASSIGNED_TO)
                    .and(reserved.STATUS.eq(BlockStatus.RESERVED))));

    Field<?> userId = userSub.field(0).as("id");
    Field<?> username = userSub.field(1).as("username");
    Field<?> userCompleted = count(userSub.field(2)).as("blocksCompleted");
    Field<?> userReserved = count(userSub.field(3)).as("blocksReserved");

    List<Individual> users =
        db.select(userId, username, userCompleted,userReserved)
            .from(userSub)
            .groupBy(userId, username)
            .orderBy(count(userCompleted).desc(), count(userReserved).desc())
            .limit(10)
            .fetchInto(Individual.class);

    Select<Record4<Integer, String, Integer, Integer>> teamSub =
        db.select(TEAM.ID, TEAM.NAME, completed.ASSIGNED_TO.as("blocksCompleted"),
            inline(null, reserved.ASSIGNED_TO).as("blocksReserved"))
            .from(TEAM)
            .join(USER_TEAM).on(USER_TEAM.TEAM_ID.eq(TEAM.ID))
            .leftJoin(completed).on(USER_TEAM.USER_ID.eq(completed.ASSIGNED_TO)
            .and(completed.STATUS.eq(BlockStatus.DONE)))
            .union(
                select(TEAM.ID, TEAM.NAME, inline(null, completed.ASSIGNED_TO).as("blocksCompleted"),
                    reserved.ASSIGNED_TO.as("blocksReserved"))
                    .from(TEAM)
                    .join(USER_TEAM).on(USER_TEAM.TEAM_ID.eq(TEAM.ID))
                    .leftJoin(reserved).on(USER_TEAM.USER_ID.eq(TEAM.ID)
                    .and(reserved.STATUS.eq(BlockStatus.RESERVED))));

    Field<?> teamId = teamSub.field(0).as("id");
    Field<?> teamName = teamSub.field(1).as("name");
    Field<?> teamCompleted = count(teamSub.field(2)).as("blocksCompleted");
    Field<?> teamReserved = count(teamSub.field(3)).as("blocksReserved");

    List<Team> teams =
        db.select(teamId, teamName, teamCompleted, teamReserved)
        .from(teamSub)
        .groupBy(teamId, teamName)
        .orderBy(teamCompleted.desc(), teamReserved.desc())
        .limit(10)
        .fetchInto(Team.class);

//    List<Team> teams =
//        db.select(TEAM.ID, TEAM.NAME, count(completed.FID).as("blocksCompleted"), count(reserved.FID).as("blocksReserved"))
//            .from(TEAM)
//            .join(USER_TEAM).on(USER_TEAM.TEAM_ID.eq(TEAM.ID))
//            .leftJoin(reserved).on(USER_TEAM.USER_ID.eq(reserved.ASSIGNED_TO).and(reserved.STATUS.eq(BlockStatus.RESERVED)))
//            .leftJoin(completed).on(USER_TEAM.USER_ID.eq(completed.ASSIGNED_TO).and(completed.STATUS.eq(BlockStatus.DONE)))
//            .groupBy(TEAM.ID)
//            .orderBy(inline(3).desc(), inline(4).desc())
//            .limit(10)
//            .fetchInto(Team.class);

//    List<Individual> individuals =
//        db.select(USERS.ID, USERS.USERNAME, count(completed.FID).as("blocksCompleted"), count(reserved.FID).as("blocksReserved"))
//            .from(USERS)
//            .leftJoin(reserved).on(USERS.ID.eq(reserved.ASSIGNED_TO).and(reserved.STATUS.eq(BlockStatus.RESERVED)))
//            .leftJoin(completed).on(USERS.ID.eq(completed.ASSIGNED_TO).and(completed.STATUS.eq(BlockStatus.DONE)))
//            .groupBy(USERS.ID)
//            .orderBy(inline(3).desc(), inline(4).desc())
//            .limit(10)
//            .fetchInto(Individual.class);

    return new BlockLeaderboardResponse(teams, users);
  }
}
