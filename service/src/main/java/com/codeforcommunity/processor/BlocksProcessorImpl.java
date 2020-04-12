package com.codeforcommunity.processor;

import com.codeforcommunity.api.IBlockProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.BlockResponse;
import com.codeforcommunity.enums.BlockStatus;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.requester.MapRequester;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.generated.tables.records.BlockRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.generated.Tables.BLOCK;

public class BlocksProcessorImpl implements IBlockProcessor {

  private MapRequester mapRequester;
  private DSLContext db;

  public BlocksProcessorImpl(MapRequester mapRequester, DSLContext db) {
    this.mapRequester = mapRequester;
    this.db = db;
  }

  @Override
  public BlockResponse reserveBlocks(JWTData jwtData, List<String> blockIds) {
    int userId = jwtData.getUserId();

    Map<BlockStatus, Result<BlockRecord>> brs = getBlocksByStatus(blockIds);

    List<String> failures = getInvalidBlockStatusIds(brs, BlockStatus.OPEN);

    List<BlockRecord> eligibleBlocks = brs.get(BlockStatus.OPEN).into(BlockRecord.class);

    updateDatabaseBlocks(eligibleBlocks, BlockStatus.RESERVED, jwtData.getUserId());

    List<String> successfulBlockIds = brs.get(BlockStatus.OPEN).map(BlockRecord::getFid);
    mapRequester.updateStreets(successfulBlockIds, BlockStatus.RESERVED);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public BlockResponse finishBlocks(JWTData jwtData, List<String> blockIds) {
    Map<BlockStatus, Result<BlockRecord>> brs = getBlocksByStatus(blockIds);

    List<String> failures = getInvalidBlockStatusIds(brs, BlockStatus.RESERVED);

    List<BlockRecord> eligibleBlocks = getBlocksEditableByUser(brs.get(BlockStatus.RESERVED), jwtData, failures);

    updateDatabaseBlocks(eligibleBlocks, BlockStatus.DONE, jwtData.getUserId());

    List<String> successfulBlockIds = eligibleBlocks.stream().map(BlockRecord::getFid).collect(Collectors.toList());
    mapRequester.updateStreets(successfulBlockIds, BlockStatus.DONE);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public BlockResponse releaseBlocks(JWTData jwtData, List<String> blockIds) {
    Map<BlockStatus, Result<BlockRecord>> brs = getBlocksByStatus(blockIds);

    List<String> failures = getInvalidBlockStatusIds(brs, BlockStatus.RESERVED);

    List<BlockRecord> eligibleBlocks = getBlocksEditableByUser(brs.get(BlockStatus.RESERVED), jwtData, failures);

    updateDatabaseBlocks(eligibleBlocks, BlockStatus.OPEN, jwtData.getUserId());

    List<String> successfulBlockIds = eligibleBlocks.stream().map(BlockRecord::getFid).collect(Collectors.toList());
    mapRequester.updateStreets(successfulBlockIds, BlockStatus.OPEN);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public BlockResponse resetBlocks(JWTData jwtData, List<String> blockIds) {
    Map<BlockStatus, Result<BlockRecord>> brs = getBlocksByStatus(blockIds);

    List<String> failures = getInvalidBlockStatusIds(brs, BlockStatus.DONE);

    List<BlockRecord> eligibleBlocks = getBlocksEditableByUser(brs.get(BlockStatus.DONE), jwtData, failures);

    updateDatabaseBlocks(eligibleBlocks, BlockStatus.OPEN, jwtData.getUserId());

    List<String> successfulBlockIds = eligibleBlocks.stream().map(BlockRecord::getFid).collect(Collectors.toList());
    mapRequester.updateStreets(successfulBlockIds, BlockStatus.OPEN);

    return new BlockResponse(successfulBlockIds, failures);
  }


  /**
   * Given a list of block ids, return the corresponding block records separated by block status.
   */
  private Map<BlockStatus, Result<BlockRecord>> getBlocksByStatus(List<String> blockIds) {
    return db.selectFrom(BLOCK)
        .where(BLOCK.FID.in(blockIds))
        .fetchGroups(BLOCK.STATUS);
  }

  /**
   * Get the list of block ids from the given Map that are not in the correct state.
   */
  private List<String> getInvalidBlockStatusIds(Map<BlockStatus, Result<BlockRecord>> blocks, BlockStatus validBlockStatus) {
    List<String> failures = new ArrayList<>();
    blocks.forEach((blockStatus, blockRecordResult) -> {
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
  private List<BlockRecord> getBlocksEditableByUser(Result<BlockRecord> blocks, JWTData userData, List<String> failures) {
    if (userData.getPrivilegeLevel() == PrivilegeLevel.ADMIN) {
      return blocks.into(BlockRecord.class);
    }

    List<BlockRecord> eligibleBlocks = new ArrayList<>();
    blocks.forEach(blockRecord -> {
      if (blockRecord.getAssignedTo().equals(userData.getUserId())) {
        eligibleBlocks.add(blockRecord);
      } else {
        failures.add(blockRecord.getFid());
      }
    });
    return eligibleBlocks;
  }

  /**
   * Update the database records of the given blocks to the new status with the appropriate assignedTo
   * value.
   */
  private void updateDatabaseBlocks(List<BlockRecord> eligibleBlocks, BlockStatus newStatus, int userId) {
    eligibleBlocks.forEach(br -> {
      Integer setToId;
      switch (newStatus) {
        case OPEN: setToId = null; break;
        case RESERVED: setToId = userId; break;
        case DONE: setToId = br.getAssignedTo(); break;
        default: throw new UnsupportedOperationException("BlockStatus " + newStatus + " is unhandled");
      }

      br.setAssignedTo(setToId);
      br.setStatus(newStatus);
      br.store();
    });
  }
}
