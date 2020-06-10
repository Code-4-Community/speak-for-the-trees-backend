package com.codeforcommunity.processor;

import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.USERS;

import com.codeforcommunity.api.IBlockProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.AssignedBlock;
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
        .map(BlockRecord::getFid)
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
        db.select(BLOCK.FID, USERS.USERNAME, BLOCK.UPDATED_TIMESTAMP)
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

    List<String> blockFids = db.selectFrom(BLOCK).fetch(BLOCK.FID);
    for (int i = 0; i < blockFids.size(); i += 3000) {
      List<String> sublist = blockFids.subList(i, Math.min(blockFids.size(), i + 3000));
      mapRequester.updateStreets(sublist, BlockStatus.OPEN);
    }
    db.update(BLOCK).set(BLOCK.STATUS, BlockStatus.OPEN).execute();
  }

  /**
   * Given a list of block ids, return the corresponding block records separated by block status.
   */
  private Map<BlockStatus, Result<BlockRecord>> getBlocksByStatus(List<String> blockIds) {
    return db.selectFrom(BLOCK).where(BLOCK.FID.in(blockIds)).fetchGroups(BLOCK.STATUS);
  }

  /** Get the list of block ids from the given Map that are not in the correct state. */
  private List<String> getInvalidBlockStatusIds(
      Map<BlockStatus, Result<BlockRecord>> blocks, BlockStatus validBlockStatus) {
    List<String> failures = new ArrayList<>();
    blocks.forEach(
        (blockStatus, blockRecordResult) -> {
          if (!blockStatus.equals(validBlockStatus)) {
            failures.addAll(blockRecordResult.map(BlockRecord::getFid));
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
            failures.add(blockRecord.getFid());
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
              break;
            case DONE:
              setToId = br.getAssignedTo();
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
