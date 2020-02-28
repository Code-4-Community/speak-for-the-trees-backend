package com.codeforcommunity.processor;

import com.codeforcommunity.api.IBlockProcessor;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.BlockResponse;
import com.codeforcommunity.enums.BlockStatus;
import com.codeforcommunity.requester.Requester;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.generated.Tables;
import org.jooq.generated.tables.records.BlockRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.generated.Tables.BLOCK;

public class BlocksProcessorImpl implements IBlockProcessor {

  private Requester requester;
  private DSLContext db;

  // TODO: Add constructor
  // TODO: Refactor methods to remove code duplication
  // TODO: Add checks for admin privilege

  @Override
  public BlockResponse reserveBlocks(JWTData jwtData, List<String> blockIds) {
    int userId = jwtData.getUserId();

    Map<BlockStatus, Result<BlockRecord>> brs = db.selectFrom(BLOCK)
        .where(BLOCK.FID.in(blockIds))
        .fetchGroups(BLOCK.STATUS);

    List<String> failures = new ArrayList<>();
    brs.forEach((bs, rbrs) -> {
      if (!bs.equals(BlockStatus.OPEN)) {
        failures.addAll(rbrs.map(BlockRecord::getFid));
      }
    });

    brs.get(BlockStatus.OPEN).forEach(br -> {
      br.setAssignedTo(userId);
      br.setStatus(BlockStatus.RESERVED);
      br.store();
    });

    List<String> successfulBlockIds = brs.get(BlockStatus.OPEN).map(BlockRecord::getFid);
    requester.updateStreets(successfulBlockIds, BlockStatus.RESERVED);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public BlockResponse finishBlocks(JWTData jwtData, List<String> blockIds) {
    int userId = jwtData.getUserId();

    Map<BlockStatus, Result<BlockRecord>> brs = db.selectFrom(BLOCK)
        .where(BLOCK.FID.in(blockIds))
        .fetchGroups(BLOCK.STATUS);

    List<String> failures = new ArrayList<>();
    brs.forEach((bs, rbrs) -> {
      if (!bs.equals(BlockStatus.RESERVED)) {
        failures.addAll(rbrs.map(BlockRecord::getFid));
      }
    });

    List<BlockRecord> eligibleBlocks = new ArrayList<>();
    brs.get(BlockStatus.RESERVED).forEach(br -> {
      if (br.getAssignedTo() != userId) {
        failures.add(br.getFid());
      } else {
        eligibleBlocks.add(br);
      }
    });

    eligibleBlocks.forEach(br -> {
      br.setStatus(BlockStatus.DONE);
      br.store();
    });

    List<String> successfulBlockIds = eligibleBlocks.stream().map(BlockRecord::getFid).collect(Collectors.toList());
    requester.updateStreets(successfulBlockIds, BlockStatus.DONE);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public BlockResponse releaseBlocks(JWTData jwtData, List<String> blockIds) {
    int userId = jwtData.getUserId();

    Map<BlockStatus, Result<BlockRecord>> brs = db.selectFrom(BLOCK)
        .where(BLOCK.FID.in(blockIds))
        .fetchGroups(BLOCK.STATUS);

    List<String> failures = new ArrayList<>();
    brs.forEach((bs, rbrs) -> {
      if (!bs.equals(BlockStatus.RESERVED)) {
        failures.addAll(rbrs.map(BlockRecord::getFid));
      }
    });

    List<BlockRecord> eligibleBlocks = new ArrayList<>();
    brs.get(BlockStatus.RESERVED).forEach(br -> {
      if (br.getAssignedTo() != userId) {
        failures.add(br.getFid());
      } else {
        eligibleBlocks.add(br);
      }
    });

    eligibleBlocks.forEach(br -> {
      br.setStatus(BlockStatus.OPEN);
      br.store();
    });

    List<String> successfulBlockIds = eligibleBlocks.stream().map(BlockRecord::getFid).collect(Collectors.toList());
    requester.updateStreets(successfulBlockIds, BlockStatus.OPEN);

    return new BlockResponse(successfulBlockIds, failures);
  }

  @Override
  public BlockResponse resetBlocks(JWTData jwtData, List<String> blockIds) {
    int userId = jwtData.getUserId();

    Map<BlockStatus, Result<BlockRecord>> brs = db.selectFrom(BLOCK)
        .where(BLOCK.FID.in(blockIds))
        .fetchGroups(BLOCK.STATUS);

    List<String> failures = new ArrayList<>();
    brs.forEach((bs, rbrs) -> {
      if (!bs.equals(BlockStatus.DONE)) {
        failures.addAll(rbrs.map(BlockRecord::getFid));
      }
    });

    List<BlockRecord> eligibleBlocks = new ArrayList<>();
    brs.get(BlockStatus.RESERVED).forEach(br -> {
      if (br.getAssignedTo() != userId) {
        failures.add(br.getFid());
      } else {
        eligibleBlocks.add(br);
      }
    });

    eligibleBlocks.forEach(br -> {
      br.setStatus(BlockStatus.OPEN);
      br.store();
    });

    List<String> successfulBlockIds = eligibleBlocks.stream().map(BlockRecord::getFid).collect(Collectors.toList());
    requester.updateStreets(successfulBlockIds, BlockStatus.OPEN);

    return new BlockResponse(successfulBlockIds, failures);
  }
}
