package com.codeforcommunity.api;

import com.codeforcommunity.dto.blockInfo.BlockInfoResponse;
import com.codeforcommunity.dto.blockInfo.BlockLeaderboardResponse;

public interface IBlockInfoProcessor {
  BlockInfoResponse getBlocks();
  BlockLeaderboardResponse getBlockLeaderboards();
}
