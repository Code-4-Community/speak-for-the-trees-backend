package com.codeforcommunity.processor;

import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.select;

import com.codeforcommunity.api.IBlockInfoProcessor;
import com.codeforcommunity.dto.blockInfo.BlockInfoResponse;
import com.codeforcommunity.dto.blockInfo.BlockLeaderboardResponse;
import com.codeforcommunity.dto.blockInfo.Individual;
import com.codeforcommunity.dto.blockInfo.Team;
import com.codeforcommunity.enums.BlockStatus;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record4;
import org.jooq.Select;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitStep;
import org.jooq.SelectSelectStep;
import org.jooq.Table;

public class BlockInfoProcessorImpl implements IBlockInfoProcessor {
  private final DSLContext db;

  public BlockInfoProcessorImpl(DSLContext db) {
    this.db = db;
  }

  @Override
  public BlockInfoResponse getBlocks() {
    Integer openBlocks =
        db.select(count(BLOCK.STATUS))
            .from(BLOCK)
            .where(BLOCK.STATUS.eq(BlockStatus.OPEN))
            .fetchOneInto(Integer.class);
    Integer doneBlocks =
        db.select(count(BLOCK.STATUS))
            .from(BLOCK)
            .where(BLOCK.STATUS.eq(BlockStatus.DONE))
            .fetchOneInto(Integer.class);
    Integer assignedBlocks =
        db.select(count(BLOCK.STATUS))
            .from(BLOCK)
            .where(BLOCK.STATUS.eq(BlockStatus.RESERVED))
            .fetchOneInto(Integer.class);
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
   *
   * <pre>
   * SELECT <strong>id</strong>, <strong>name</strong>, ? AS completed, ? AS reserved
   * FROM <strong>table</strong>
   * (optional) JOIN user_team ON user_team.team_id = <strong>id</strong>
   * JOIN block ON block.id = <strong>table</strong>.assigned_to AND block.status = <strong>blockStatus</strong>
   * (optional) WHERE <strong>id</strong> = <strong>itemId</strong>
   * </pre>
   *
   * where ? AS [completed, reserved] matches <strong>blockStatus</strong>.
   *
   * @param table the table to select from (should be USERS or TEAM)
   * @param blockStatus the BlockStatus to search for
   * @param isTeam if the record being selected is for a TEAM or USERS table
   * @param itemId the optional item id to select items by
   * @return the part of the query as represented above
   */
  SelectConnectByStep<Record4<Integer, String, String, String>> buildSubQueryParts(
      Table<? extends Record> table, BlockStatus blockStatus, boolean isTeam, Integer itemId) {
    if (!table.equals(TEAM) && !table.equals(USERS)) {
      throw new IllegalArgumentException("Table must be TEAM or USERS, was: " + table.getName());
    }

    org.jooq.generated.tables.Block reserved = BLOCK.as("reserved");
    org.jooq.generated.tables.Block completed = BLOCK.as("completed");
    org.jooq.generated.tables.Block selectedBlock;
    String nameName = isTeam ? "name" : "username";

    Field<Integer> id = table.field(0, Integer.class);
    Field<String> name = table.field(nameName, String.class);

    SelectSelectStep<Record4<Integer, String, String, String>> selectBase;

    // create select fields depending on desired block status
    if (blockStatus == BlockStatus.DONE) {
      selectBase =
          select(
              id,
              name,
              completed.FID.as("isCompleted"),
              inline(null, completed.FID).as("isReserved"));
      selectedBlock = completed;
    } else {
      selectBase =
          select(
              id,
              name,
              inline(null, reserved.FID).as("isCompleted"),
              reserved.FID.as("isReserved"));
      selectedBlock = reserved;
    }

    SelectJoinStep<Record4<Integer, String, String, String>> joinStep = selectBase.from(table);

    // join on USER_TEAM table and optionally the block table if querying for a team
    if (isTeam) {
      joinStep =
          joinStep
              .join(USER_TEAM)
              .on(USER_TEAM.TEAM_ID.eq(id))
              .join(selectedBlock)
              .on(
                  USER_TEAM
                      .USER_ID
                      .eq(selectedBlock.ASSIGNED_TO)
                      .and(selectedBlock.STATUS.eq(blockStatus)));
    } else {
      joinStep =
          joinStep
              .join(selectedBlock)
              .on(id.eq(selectedBlock.ASSIGNED_TO).and(selectedBlock.STATUS.eq(blockStatus)));
    }

    // add where step for selecting where the ID = itemId
    SelectConnectByStep<Record4<Integer, String, String, String>> whereStep;
    if (itemId != null) {
      whereStep = joinStep.where(id.eq(itemId));
    } else {
      whereStep = joinStep;
    }

    return whereStep;
  }

  /**
   * Builds the subquery. Specifically the
   *
   * <pre>
   *   SELECT <strong>id</strong>, <strong>name</strong>, fid as completed_raw, null as reserved_raw
   *   FROM <strong>table</strong>
   *   (optional) JOIN user_team ON user_team.team_id = <strong>id</strong>
   *   JOIN block ON block.id = <strong>table</strong>.assigned_to AND block.status = 2
   *   (optional) WHERE <strong>id</strong> = <strong>itemId</strong>
   *   UNION
   *   SELECT <strong>id</strong>, <strong>name</strong>, null as completed_raw, fid as reserved_raw
   *   FROM <strong>table</strong>
   *   (optional) JOIN user_team ON user_team.team_id = <strong>id</strong>
   *   JOIN block ON block.id = <strong>table</strong>.assigned_to AND block.status = 1
   *   (optional) WHERE <strong>id</strong> = <strong>itemId</strong>
   * </pre>
   *
   * @param table the table to select from (should be USERS or TEAM)
   * @param isTeam if the record being selected is for a TEAM or USERS table
   * @param itemId the optional item id to select items by
   * @return the part of the query as represented above
   */
  Select<Record4<Integer, String, String, String>> buildSubQuery(
      Table<? extends Record> table, boolean isTeam, Integer itemId) {
    if (!table.equals(TEAM) && !table.equals(USERS)) {
      throw new IllegalArgumentException("Table must be TEAM or USERS, was: " + table.getName());
    }

    // Gets each part of the union, and unions them together
    SelectConnectByStep<Record4<Integer, String, String, String>> done =
        buildSubQueryParts(table, BlockStatus.DONE, isTeam, itemId);

    SelectConnectByStep<Record4<Integer, String, String, String>> reserved =
        buildSubQueryParts(table, BlockStatus.RESERVED, isTeam, itemId);

    return done.union(reserved);
  }

  /**
   * Compose the entire query after building the subquery. Specifically performs
   *
   * <pre>
   *   SELECT <strong>id</strong>, <strong>name</strong>,
   *     COUNT(completed_raw) AS completed, COUNT(reserved_raw) AS reserved
   *   FROM subquery
   *   GROUP BY <strong>id</strong>, <strong>name</strong>
   *   ORDER BY completed DESC, reserved DESC;
   * </pre>
   *
   * where subquery is what is returned from {@code buildSubQuery}. To run, just call {@code fetch}
   * on the returned object.
   *
   * @param table the table to select from (should be USERS or TEAM)
   * @param isTeam if the record being selected is for a TEAM or USERS table
   * @param itemId the optional item id to select items by
   * @return a List of the what is requested and returned from the query represented above
   */
  SelectLimitStep<Record4<Integer, String, Integer, Integer>> composeFullQuery(
      Table<? extends Record> table, boolean isTeam, Integer itemId) {
    if (!table.equals(TEAM) && !table.equals(USERS)) {
      throw new IllegalArgumentException("Table must be TEAM or USERS, was: " + table.getName());
    }
    Table<Record4<Integer, String, String, String>> subQuery =
        buildSubQuery(table, isTeam, itemId).asTable("subQuery");

    String nameName = isTeam ? "name" : "username";
    Field<Integer> subQueryId = subQuery.field("id").as("id").coerce(Integer.class);
    Field<String> subQueryName =
        subQuery.field(1).as(table.field(nameName).getName()).coerce(String.class);
    Field<Integer> subQueryCompleted = count(subQuery.field("isCompleted")).as("blocksCompleted");
    Field<Integer> subQueryReserved = count(subQuery.field("isReserved")).as("blocksReserved");

    return db.select(subQueryId, subQueryName, subQueryCompleted, subQueryReserved)
        .from(subQuery)
        .groupBy(subQueryId, subQueryName)
        .orderBy(subQueryCompleted.desc(), subQueryReserved.desc());
  }

  /**
   * Performs the following query:
   *
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
   *
   * @return a List of {@link Individual} as represented by the query above
   */
  public List<Individual> getUsersLeaderboard() {
    return composeFullQuery(USERS, false, null).limit(10).fetchInto(Individual.class);
  }

  /**
   * Performs the following query:
   *
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
   *
   * @return a List of {@link Team} as represented by the above query
   */
  public List<Team> getTeamLeaderboard() {
    return composeFullQuery(TEAM, true, null).limit(10).fetchInto(Team.class);
  }

  /**
   * Performs the following query:
   *
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
   * ORDER BY completed DESC, reserved DESC;
   * </pre>
   *
   * @return a List of {@link Team} as represented by the above query
   */
  public List<Team> getTeamStats() {
    return composeFullQuery(TEAM, true, null).fetchInto(Team.class);
  }

  /**
   * Performs the following query:
   *
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
   * ORDER BY completed DESC, reserved DESC;
   * </pre>
   *
   * @param teamId the id of the team to select info for
   * @return a List of {@link Team} as represented by the above query
   */
  public Team getTeamStats(int teamId) {
    return composeFullQuery(TEAM, true, teamId).fetchOneInto(Team.class);
  }
}
