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

import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;

import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectSelectStep;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.TableImpl;

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
    List<Individual> users = getUsersLeaderboard();
    List<Team> teams = getTeamLeaderboard();

    return new BlockLeaderboardResponse(teams, users);
  }

  /**
   * Builds the main parts of the subquery. Specifically the
   * <pre>
   * SELECT <strong>id</strong>, <strong>name</strong>, ? AS completed, ? AS reserved
   * FROM <strong>table</strong>
   * (optional) JOIN user_team ON user_team.team_id = <strong>id</strong>
   * JOIN block ON block.id = <strong>table</strong>.assigned_to AND block.status = <strong>blockStatus</strong>
   * </pre>
   * where ? AS [completed, reserved] matches <strong>blockStatus</strong>.
   *
   * @param table the table to select from (should be USERS or TEAM)
   * @param id the ID field of the table record
   * @param name the name field of the table record
   * @param blockStatus the BlockStatus to search for
   * @param isTeam if the record being selected is for a TEAM or USERS table
   * @return the part of the query as represented above
   */
  SelectJoinStep<Record4<Integer, String, String, String>> buildSubQueryParts(
      TableImpl<?> table, TableField<?, Integer> id, TableField<?, String> name,
      BlockStatus blockStatus, boolean isTeam) {
    org.jooq.generated.tables.Block reserved = BLOCK.as("reserved");
    org.jooq.generated.tables.Block completed = BLOCK.as("completed");
    org.jooq.generated.tables.Block selectedBlock;

    SelectSelectStep<Record4<Integer, String, String, String>> selectBase;

    if (blockStatus == BlockStatus.DONE) {
      selectBase = select(id, name, completed.FID.as("isCompleted"),
          inline(null, completed.FID).as("isReserved"));
      selectedBlock = completed;
    }
    else {
      selectBase = select(id, name, inline(null, reserved.FID)
          .as("isCompleted"), reserved.FID.as("isReserved"));
      selectedBlock = reserved;
    }

    SelectJoinStep<Record4<Integer, String, String, String>> joinStep =
        selectBase.from(table);

    if (isTeam) {
      joinStep = joinStep.join(USER_TEAM).on(USER_TEAM.TEAM_ID.eq(id))
          .join(selectedBlock).on(USER_TEAM.USER_ID.eq(selectedBlock.ASSIGNED_TO)
              .and(selectedBlock.STATUS.eq(blockStatus)));
    }
    else {
      joinStep = joinStep.join(selectedBlock).on(id.eq(selectedBlock.ASSIGNED_TO)
          .and(selectedBlock.STATUS.eq(blockStatus)));
    }

    return joinStep;
  }

  /**
   * Builds the subquery. Specifically the
   * <pre>
   *   SELECT <strong>id</strong>, <strong>name</strong>, fid as completed_raw, null as reserved_raw
   *   FROM <strong>table</strong>
   *   (optional) JOIN user_team ON user_team.team_id = <strong>id</strong>
   *   JOIN block ON block.id = <strong>table</strong>.assigned_to AND block.status = 2
   *   UNION
   *   SELECT <strong>id</strong>, <strong>name</strong>, null as completed_raw, fid as reserved_raw
   *   FROM <strong>table</strong>
   *   (optional) JOIN user_team ON user_team.team_id = <strong>id</strong>
   *   JOIN block ON block.id = <strong>table</strong>.assigned_to AND block.status = 1
   * </pre>
   *
   * @param table the table to select from (should be USERS or TEAM)
   * @param id the ID field of the table record
   * @param name the name field of the table record
   * @param isTeam if the record being selected is for a TEAM or USERS table
   * @return the part of the query as represented above
   */
  Select<Record4<Integer, String, String, String>> buildSubQuery(TableImpl<?> table,
      TableField<?, Integer> id, TableField<?, String> name, boolean isTeam) {

    SelectJoinStep<Record4<Integer, String, String, String>> done =
        buildSubQueryParts(table, id, name, BlockStatus.DONE, isTeam);

    SelectJoinStep<Record4<Integer, String, String, String>> reserved =
        buildSubQueryParts(table, id, name, BlockStatus.RESERVED, isTeam);

    return done.union(reserved);
  }

  /**
   * Compose the entire query after building the subquery. Specifically performs
   * <pre>
   *   SELECT <strong>id</strong>, <strong>name</strong>,
   *     COUNT(completed_raw) AS completed, COUNT(reserved_raw) AS reserved
   *   FROM subquery
   *   GROUP BY <strong>id</strong>, <strong>name</strong>
   *   ORDER BY completed DESC, reserved DESC
   *   LIMIT 10;
   * </pre>
   * where subquery is what is returned from {@code buildSubQuery}. To run, just call
   * {@code fetch} on the returned object.
   * @param table the table to select from (should be USERS or TEAM)
   * @param id the ID field of the table record
   * @param name the name field of the table record
   * @param isTeam if the record being selected is for a TEAM or USERS table
   * @return a List of the what is requested and returned from the query represented above
   */
  SelectLimitPercentStep<? extends Record4<?, ?, ?, ?>> composeFullQuery(
      TableImpl<?> table, TableField<?, Integer> id, TableField<?, String> name, boolean isTeam) {
    Table<Record4<Integer, String, String, String>> subQuery =
        buildSubQuery(table, id, name, isTeam).asTable("subquery");

    Field<?> subQueryId = subQuery.field("id").as("id");
    Field<?> subQueryName = subQuery.field(1).as(name.getName());
    Field<?> subQueryCompleted = count(subQuery.field("isCompleted")).as("blocksCompleted");
    Field<?> subQueryReserved = count(subQuery.field("isReserved")).as("blocksReserved");

    return db.select(subQueryId, subQueryName, subQueryCompleted, subQueryReserved)
        .from(subQuery)
        .groupBy(subQueryId, subQueryName)
        .orderBy(subQueryCompleted.desc(), subQueryReserved.desc())
        .limit(10);
  }

  /**
   * Performs the following query:
   * <pre>
   * SELECT id, username, COUNT(completed_raw) AS completed, COUNT(reserved_raw) AS reserved
   * FROM (SELECT uc.id, uc.username, c.fid AS completed_raw, null AS reserved_raw
   * FROM team uc
   * JOIN block c ON uc.id = c.assigned_to AND c.status = 2
   * UNION
   * SELECT ur.id, ur.username, null AS completed_raw, r.fid AS reserved_raw
   * FROM users ur
   * JOIN block r ON ur.id = r.assigned_to AND r.status = 1) s
   * GROUP BY id, username
   * ORDER BY completed DESC, reserved DESC
   * LIMIT 10;
   * </pre>
   * @return a List of {@link Individual} as represented by the query above
   */
  public List<Individual> getUsersLeaderboard() {
    return composeFullQuery(USERS, USERS.ID, USERS.USERNAME, false)
        .fetchInto(Individual.class);
  }

  /**
   * Performs the following query:
   * <pre>
   * SELECT id, name, COUNT(completed_raw) AS completed, COUNT(reserved_raw) AS reserved
   * FROM (SELECT tc.id, tc.name, c.fid AS completed_raw, null AS reserved_raw
   * FROM team tc
   * JOIN block c ON uc.id = c.assigned_to AND c.status = 2
   * UNION
   * SELECT tr.id, tr.name, null AS completed_raw, r.fid AS reserved_raw
   * FROM users tr
   * JOIN block r ON tr.id = r.assigned_to AND r.status = 1) s
   * GROUP BY id, name
   * ORDER BY completed DESC, reserved DESC
   * LIMIT 10;
   * </pre>
   * @return a List of {@link Team} as represented by the above query
   */
  public List<Team> getTeamLeaderboard() {
    return composeFullQuery(TEAM, TEAM.ID, TEAM.NAME, true)
        .fetchInto(Team.class);
  }
}
