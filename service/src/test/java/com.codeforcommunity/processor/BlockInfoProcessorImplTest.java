package com.codeforcommunity.processor;

import static com.sun.javaws.JnlpxArgs.verify;
import static org.junit.jupiter.api.Assertions.*;

import com.codeforcommunity.JooqMock;
import com.codeforcommunity.dto.blockInfo.BlockInfoResponse;
import com.codeforcommunity.dto.blockInfo.BlockLeaderboardResponse;
import com.codeforcommunity.enums.BlockStatus;
import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.jooq.generated.Tables.BLOCK;
import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;

class BlockInfoProcessorImplTest {
  private JooqMock mock;
  private BlockInfoProcessorImpl processor;

  @BeforeEach
  void setUp() {
    mock = new JooqMock();
    processor = new BlockInfoProcessorImpl(mock.getContext());
  }

  @Test
  void testGetBlocks() {
    Record1<Integer> open = mock.getContext().newRecord(DSL.field("COUNT", Integer.class));
    open.values(1);
    Record1<Integer> done = mock.getContext().newRecord(DSL.field("COUNT", Integer.class));
    done.values(2);
    Record1<Integer> assigned = mock.getContext().newRecord(DSL.field("COUNT", Integer.class));
    assigned.values(3);
    mock.addReturn("SELECT", open);
    mock.addReturn("SELECT", done);
    mock.addReturn("SELECT", assigned);

    BlockInfoResponse res = processor.getBlocks();

    // check that select was called 3 times
    assertEquals(3, mock.timesCalled("SELECT"));

    // check sql bindings
    List<Object[]> bindings = mock.getSqlBindings().get("SELECT");
    assertEquals(1, bindings.get(0).length);
    assertEquals(0, bindings.get(0)[0]);
    assertEquals(1, bindings.get(1).length);
    assertEquals(2, bindings.get(1)[0]);
    assertEquals(1, bindings.get(2).length);
    assertEquals(1, bindings.get(2)[0]);

    // check returned values
    assertEquals(1, res.getBlocksToDo());
    assertEquals(2, res.getBlocksCompleted());
    assertEquals(3, res.getBlocksReserved());
  }

  @Test
  void testGetBlockLeaderboards() {
    // create aggregate fields for use
    Field<Integer> completed = DSL.field("blocksCompleted", Integer.class);
    Field<Integer> reserved = DSL.field("blocksReserved", Integer.class);

    // create teams
    Record4<Integer, String, Integer, Integer> team1 =
        mock.getContext().newRecord(TEAM.ID, TEAM.NAME, completed, reserved);
    team1.values(1, "a", 2, 3);
    Record4<Integer, String, Integer, Integer> team2 =
        mock.getContext().newRecord(TEAM.ID, TEAM.NAME, completed, reserved);
    team2.values(3, "b", 5, 1);
    Record4<Integer, String, Integer, Integer> team3 =
        mock.getContext().newRecord(TEAM.ID, TEAM.NAME, completed, reserved);
    team3.values(2, "c", 0, 0);
    Record4<Integer, String, Integer, Integer> team4 =
        mock.getContext().newRecord(TEAM.ID, TEAM.NAME, completed, reserved);
    team4.values(4, "d", 4, 5);
    Result<Record> teamRes = mock.getContext().newResult(team1.fields());
    teamRes.addAll(Arrays.asList(team1, team2, team3, team4));

    // create users
    Record4<Integer, String, Integer, Integer> user1 =
        mock.getContext().newRecord(USERS.ID, USERS.USERNAME, completed, reserved);
    user1.values(1, "u1", 2, 3);
    Record4<Integer, String, Integer, Integer> user2 =
        mock.getContext().newRecord(USERS.ID, USERS.USERNAME, completed, reserved);
    user2.values(2, "u2", 0, 0);
    Record4<Integer, String, Integer, Integer> user3 =
        mock.getContext().newRecord(USERS.ID, USERS.USERNAME, completed, reserved);
    user3.values(3, "u1", 4, 5);
    Result<Record> userRes = mock.getContext().newResult(user1.fields());
    userRes.addAll(Arrays.asList(user1, user2, user3));

    mock.addReturn("SELECT", teamRes);
    mock.addReturn("SELECT", userRes);

    BlockLeaderboardResponse res = processor.getBlockLeaderboards();

    assertEquals(2, mock.timesCalled("SELECT"));
    assertEquals(4, res.getTeams().size());
    assertEquals(3, res.getIndividuals().size());
    assertEquals(1, res.getTeams().get(0).getId());
    assertEquals(1, res.getIndividuals().get(0).getId());
  }
}