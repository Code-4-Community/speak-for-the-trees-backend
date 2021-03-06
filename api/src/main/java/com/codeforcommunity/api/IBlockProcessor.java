package com.codeforcommunity.api;

import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.BlockResponse;
import com.codeforcommunity.dto.blocks.BlockSeedingInfo;
import com.codeforcommunity.dto.blocks.GetAssignedBlocksResponse;
import java.util.List;

public interface IBlockProcessor {
  BlockResponse reserveBlocks(JWTData jwtData, List<String> blockIds);

  BlockResponse finishBlocks(JWTData jwtData, List<String> blockIds);

  BlockResponse releaseBlocks(JWTData jwtData, List<String> blockIds);

  BlockResponse resetBlocks(JWTData jwtData, List<String> blockIds);

  List<String> getUserReservedBlocks(JWTData jwtData, boolean includeDone);

  GetAssignedBlocksResponse getAllReservedBlocks(JWTData jwtData);

  GetAssignedBlocksResponse getAllDoneBlocks(JWTData jwtData);

  void resetAllBlocks(JWTData jwtData);

  void setMapToDatabase(JWTData jwtData);

  void seedBlockCompletions(JWTData jwtData, List<BlockSeedingInfo> blockSeedingInfos);

  String getBlockExportCSV(JWTData jwtData);
}
