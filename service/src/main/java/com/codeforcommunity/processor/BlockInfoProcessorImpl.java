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
import org.jooq.impl.DSL;

import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;
import static org.jooq.impl.DSL.all;
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
    org.jooq.generated.tables.Users cUsers = USERS.as("cUsers");
    org.jooq.generated.tables.Users rUsers = USERS.as("rUsers");

    /* The query below is trying to do this:
     * select id, username, count(completed_raw) as completed, count(reserved_raw) as reserved
     * from (select u.id, u.username, c.fid as completed_raw, null as reserved_raw
     * from users u
     * left join block c on u.id = c.assigned_to and c.status = 2
     * union
     * select uo.id, uo.username, null as completed_raw, f.fid as reserved_raw
     * from users uo
     * left join block f on uo.id = f.assigned_to and f.status = 1) s
     * group by id, username
     * order by completed desc, reserved desc
     * limit 10;
     */


    Select<Record4<Integer, String, String, String>> userSub =
        select(cUsers.ID, cUsers.USERNAME, completed.FID.as("isCompleted"),
            inline(null, completed.FID).as("isReserved"))
            .from(cUsers)
            .join(completed).on(cUsers.ID.eq(completed.ASSIGNED_TO)
            .and(completed.STATUS.eq(BlockStatus.DONE)))
            .union(
                select(rUsers.ID, rUsers.USERNAME, inline(null, reserved.FID)
                    .as("blocksCompleted"), reserved.FID.as("blocksReserved"))
                    .from(rUsers)
                    .join(reserved).on(rUsers.ID.eq(reserved.ASSIGNED_TO)
                    .and(reserved.STATUS.eq(BlockStatus.RESERVED))));

    Field<?> userId = userSub.field("id").as("id");
    Field<?> username = userSub.field("username").as("username");
    Field<?> userCompleted = count(userSub.field("isCompleted")).as("blocksCompleted");
    Field<?> userReserved = count(userSub.field("isReserved")).as("blocksReserved");

    List<Individual> users =
        db.select(userId, username, userCompleted, userReserved)
            .from(userSub)
            .groupBy(userId, username)
            .orderBy(userCompleted.desc(), userReserved.desc())
            .limit(10)
            .fetchInto(Individual.class);

    Select<Record4<Integer, String, String, String>> teamSub =
        select(TEAM.ID, TEAM.NAME, completed.FID.as("isCompleted"),
            inline(null, reserved.FID).as("isReserved"))
            .from(TEAM)
            .join(USER_TEAM).on(USER_TEAM.TEAM_ID.eq(TEAM.ID))
            .leftJoin(completed).on(USER_TEAM.USER_ID.eq(completed.ASSIGNED_TO)
            .and(completed.STATUS.eq(BlockStatus.DONE)))
            .union(
                select(TEAM.ID, TEAM.NAME, inline(null, completed.FID).as("blocksCompleted"),
                    reserved.FID.as("blocksReserved"))
                    .from(TEAM)
                    .join(USER_TEAM).on(USER_TEAM.TEAM_ID.eq(TEAM.ID))
                    .leftJoin(reserved).on(USER_TEAM.USER_ID.eq(TEAM.ID)
                    .and(reserved.STATUS.eq(BlockStatus.RESERVED))));

    Field<?> teamId = teamSub.field("id").as("id");
    Field<?> teamName = teamSub.field("name").as("name");
    Field<?> teamCompleted = count(teamSub.field("isCompleted")).as("blocksCompleted");
    Field<?> teamReserved = count(teamSub.field("isReserved")).as("blocksReserved");

    List<Team> teams =
        db.select(teamId, teamName, teamCompleted, teamReserved)
        .from(teamSub)
        .groupBy(teamId, teamName)
        .orderBy(teamCompleted.desc(), teamReserved.desc())
        .limit(10)
        .fetchInto(Team.class);

    return new BlockLeaderboardResponse(teams, users);
  }
}
