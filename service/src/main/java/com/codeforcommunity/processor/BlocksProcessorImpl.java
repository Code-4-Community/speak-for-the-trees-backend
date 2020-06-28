package com.codeforcommunity.processor;

import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;
import static org.jooq.generated.tables.Team.TEAM;

import com.codeforcommunity.api.IBlockProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.AssignedBlock;
import com.codeforcommunity.dto.blocks.BlockExport;
import com.codeforcommunity.dto.blocks.BlockResponse;
import com.codeforcommunity.dto.blocks.GetAssignedBlocksResponse;
import com.codeforcommunity.enums.BlockStatus;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.exceptions.AdminOnlyRouteException;
import com.codeforcommunity.requester.MapRequester;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.generated.tables.records.BlockRecord;

public class BlocksProcessorImpl implements IBlockProcessor {

  private DSLContext db;
  private MapRequester mapRequester;

  public BlocksProcessorImpl(DSLContext db, MapRequester mapRequester) {
    this.db = db;
    this.mapRequester = mapRequester;
  }

  @Override
  public BlockResponse reserveBlocks(JWTData jwtData, List<String> blockIds) {
    Map<BlockStatus, Result<BlockRecord>> brs = getBlocksByStatus(blockIds);

    List<String> failures = getInvalidBlockStatusIds(brs, BlockStatus.OPEN);

    List<BlockRecord> eligibleBlocks =
        brs.getOrDefault(BlockStatus.OPEN, db.newResult(BLOCK)).into(BlockRecord.class);

    updateDatabaseBlocks(eligibleBlocks, BlockStatus.RESERVED, jwtData.getUserId());

    List<String> successfulBlockIds =
        brs.getOrDefault(BlockStatus.OPEN, db.newResult(BLOCK)).map(BlockRecord::getFid);
    mapRequester.updateStreets(successfulBlockIds, BlockStatus.RESERVED);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public BlockResponse finishBlocks(JWTData jwtData, List<String> blockIds) {
    Map<BlockStatus, Result<BlockRecord>> brs = getBlocksByStatus(blockIds);

    List<String> failures = getInvalidBlockStatusIds(brs, BlockStatus.RESERVED);

    List<BlockRecord> eligibleBlocks =
        getBlocksEditableByUser(
            brs.getOrDefault(BlockStatus.RESERVED, db.newResult(BLOCK)), jwtData, failures);

    updateDatabaseBlocks(eligibleBlocks, BlockStatus.DONE, jwtData.getUserId());

    List<String> successfulBlockIds =
        eligibleBlocks.stream().map(BlockRecord::getFid).collect(Collectors.toList());
    mapRequester.updateStreets(successfulBlockIds, BlockStatus.DONE);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public BlockResponse releaseBlocks(JWTData jwtData, List<String> blockIds) {
    Map<BlockStatus, Result<BlockRecord>> brs = getBlocksByStatus(blockIds);

    List<String> failures = getInvalidBlockStatusIds(brs, BlockStatus.RESERVED);

    List<BlockRecord> eligibleBlocks =
        getBlocksEditableByUser(
            brs.getOrDefault(BlockStatus.RESERVED, db.newResult(BLOCK)), jwtData, failures);

    updateDatabaseBlocks(eligibleBlocks, BlockStatus.OPEN, jwtData.getUserId());

    List<String> successfulBlockIds =
        eligibleBlocks.stream().map(BlockRecord::getFid).collect(Collectors.toList());
    mapRequester.updateStreets(successfulBlockIds, BlockStatus.OPEN);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public BlockResponse resetBlocks(JWTData jwtData, List<String> blockIds) {
    Map<BlockStatus, Result<BlockRecord>> brs = getBlocksByStatus(blockIds);

    List<String> failures = getInvalidBlockStatusIds(brs, BlockStatus.DONE);

    List<BlockRecord> eligibleBlocks =
        getBlocksEditableByUser(
            brs.getOrDefault(BlockStatus.DONE, db.newResult(BLOCK)), jwtData, failures);

    updateDatabaseBlocks(eligibleBlocks, BlockStatus.OPEN, jwtData.getUserId());

    List<String> successfulBlockIds =
        eligibleBlocks.stream().map(BlockRecord::getFid).collect(Collectors.toList());
    mapRequester.updateStreets(successfulBlockIds, BlockStatus.OPEN);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public List<String> getUserReservedBlocks(JWTData jwtData, boolean includeDone) {
    return getUserReservedBlocks(jwtData.getUserId(), includeDone).stream()
        .map(BlockRecord::getId)
        .collect(Collectors.toList());
  }

  @Override
  public GetAssignedBlocksResponse getAllReservedBlocks(JWTData jwtData) {
    if (jwtData.getPrivilegeLevel() != PrivilegeLevel.ADMIN) {
      throw new AdminOnlyRouteException();
    }
    return getAssignedBlocksWithStatus(BlockStatus.RESERVED);
  }

  @Override
  public GetAssignedBlocksResponse getAllDoneBlocks(JWTData jwtData) {
    if (jwtData.getPrivilegeLevel() != PrivilegeLevel.ADMIN) {
      throw new AdminOnlyRouteException();
    }
    return getAssignedBlocksWithStatus(BlockStatus.DONE);
  }

  private GetAssignedBlocksResponse getAssignedBlocksWithStatus(BlockStatus status) {
    return new GetAssignedBlocksResponse(
        db.select(BLOCK.ID, USERS.USERNAME, BLOCK.UPDATED_TIMESTAMP)
            .from(BLOCK)
            .innerJoin(USERS)
            .on(BLOCK.ASSIGNED_TO.eq(USERS.ID))
            .where(BLOCK.STATUS.equal(status))
            .orderBy(BLOCK.UPDATED_TIMESTAMP.desc())
            .fetchInto(AssignedBlock.class));
  }

  @Override
  public void resetAllBlocks(JWTData jwtData) {
    if (jwtData.getPrivilegeLevel() != PrivilegeLevel.ADMIN) {
      throw new AdminOnlyRouteException();
    }

    List<String> blockIds = db.selectFrom(BLOCK).fetch(BLOCK.FID);
    for (int i = 0; i < blockIds.size(); i += 3000) {
      List<String> sublist = blockIds.subList(i, Math.min(blockIds.size(), i + 3000));
      mapRequester.updateStreets(sublist, BlockStatus.OPEN);
    }
    db.update(BLOCK).set(BLOCK.STATUS, BlockStatus.OPEN).execute();
  }

  @Override
  public String getBlockExportCSV(JWTData jwtData) {
    List<BlockExport> blockExports =
        db.select(
                BLOCK.ID,
                BLOCK.STATUS,
                BLOCK.UPDATED_TIMESTAMP,
                BLOCK.LAST_RESERVED,
                BLOCK.LAST_COMPLETED,
                USERS.FIRST_NAME,
                USERS.LAST_NAME,
                USERS.EMAIL,
                USERS.USERNAME,
                USERS.ID)
            .from(BLOCK)
            .leftJoin(USERS)
            .on(USERS.ID.eq(BLOCK.ASSIGNED_TO))
            .orderBy(BLOCK.STATUS.desc(), USERS.ID)
            .fetchInto(BlockExport.class);

    Map<Integer, List<String>> userTeams =
        db.select(USERS.ID, TEAM.NAME)
            .from(USERS)
            .leftJoin(USER_TEAM)
            .on(USERS.ID.eq(USER_TEAM.USER_ID))
            .leftJoin(TEAM)
            .on(USER_TEAM.TEAM_ID.eq(TEAM.ID))
            .fetchGroups(USERS.ID, TEAM.NAME);

    StringBuilder builder = new StringBuilder();
    builder.append(BlockExport.getHeaderCSV());
    for (BlockExport export : blockExports) {
      List<String> teamList = userTeams.get(export.getUserId());
      export.setTeamNames(teamList);
      builder.append(export.getRowCSV());
    }

    return builder.toString();
  }

  /**
   * Given a list of block ids, return the corresponding block records separated by block status.
   */
  private Map<BlockStatus, Result<BlockRecord>> getBlocksByStatus(List<String> blockIds) {
    return db.selectFrom(BLOCK).where(BLOCK.ID.in(blockIds)).fetchGroups(BLOCK.STATUS);
  }

  /** Get the list of block ids from the given Map that are not in the correct state. */
  private List<String> getInvalidBlockStatusIds(
      Map<BlockStatus, Result<BlockRecord>> blocks, BlockStatus validBlockStatus) {
    List<String> failures = new ArrayList<>();
    blocks.forEach(
        (blockStatus, blockRecordResult) -> {
          if (!blockStatus.equals(validBlockStatus)) {
            failures.addAll(blockRecordResult.map(BlockRecord::getId));
          }
        });
    return failures;
  }

  /**
   * From the given Collection of blocks, return the list of blocks that the given user has the
   * privilege level to modify.
   */
  private List<BlockRecord> getBlocksEditableByUser(
      Result<BlockRecord> blocks, JWTData userData, List<String> failures) {
    if (userData.getPrivilegeLevel() == PrivilegeLevel.ADMIN) {
      return blocks.into(BlockRecord.class);
    }

    List<BlockRecord> eligibleBlocks = new ArrayList<>();
    blocks.forEach(
        blockRecord -> {
          if (blockRecord.getAssignedTo().equals(userData.getUserId())) {
            eligibleBlocks.add(blockRecord);
          } else {
            failures.add(blockRecord.getId());
          }
        });
    return eligibleBlocks;
  }

  /**
   * Update the database records of the given blocks to the new status with the appropriate
   * assignedTo value.
   */
  private void updateDatabaseBlocks(
      List<BlockRecord> eligibleBlocks, BlockStatus newStatus, int userId) {
    eligibleBlocks.forEach(
        br -> {
          Integer setToId;
          switch (newStatus) {
            case OPEN:
              setToId = null;
              break;
            case RESERVED:
              setToId = userId;
              br.setLastReserved(new Timestamp(System.currentTimeMillis()));
              break;
            case DONE:
              setToId = br.getAssignedTo();
              br.setLastCompleted(new Timestamp(System.currentTimeMillis()));
              break;
            default:
              throw new UnsupportedOperationException("BlockStatus " + newStatus + " is unhandled");
          }

          br.setAssignedTo(setToId);
          br.setStatus(newStatus);
          br.setUpdatedTimestamp(new Timestamp(System.currentTimeMillis()));
          br.store();
        });
  }

  /**
   * Returns all blocks that are reserved by a user.
   *
   * @param userId the ID of the user.
   * @param includeDone if true, returns all blocks that are "RESERVED" or "DONE", else only returns
   *     "RESERVED" blocks
   * @return A list of BlockRecord that are assigned to the given user.
   */
  private List<BlockRecord> getUserReservedBlocks(int userId, boolean includeDone) {
    if (includeDone) {
      return db.selectFrom(BLOCK)
          .where(BLOCK.STATUS.equal(BlockStatus.RESERVED).or(BLOCK.STATUS.equal(BlockStatus.DONE)))
          .and(BLOCK.ASSIGNED_TO.equal(userId))
          .fetch();
    } else {
      return db.selectFrom(BLOCK)
          .where(BLOCK.STATUS.equal(BlockStatus.RESERVED))
          .and(BLOCK.ASSIGNED_TO.equal(userId))
          .fetch();
    }
  }
}
