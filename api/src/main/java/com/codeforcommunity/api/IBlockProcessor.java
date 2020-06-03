package com.codeforcommunity.api;

import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dto.blocks.BlockResponse;
import java.util.List;

public interface IBlockProcessor {
  BlockResponse reserveBlocks(JWTData jwtData, List<String> blockIds);

  BlockResponse finishBlocks(JWTData jwtData, List<String> blockIds);

  BlockResponse releaseBlocks(JWTData jwtData, List<String> blockIds);

  BlockResponse resetBlocks(JWTData jwtData, List<String> blockIds);

  List<String> getUserReservedBlocks(JWTData jwtData, boolean includeDone);

  void resetAllBlocks(JWTData jwtData);
}
