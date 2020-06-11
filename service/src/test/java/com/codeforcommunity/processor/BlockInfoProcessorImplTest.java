package com.codeforcommunity.processor;

import static org.jooq.generated.Tables.TEAM;
import static org.jooq.generated.Tables.USERS;
import static org.jooq.generated.Tables.USER_TEAM;
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
import org.jooq.Select;
import org.jooq.SelectFinalStep;
import org.jooq.SelectJoinStep;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class BlockInfoProcessorImplTest {
    private JooqMock mock;
    private BlockInfoProcessorImpl processor;

    @BeforeEach
    void setUp() {
        mock = new JooqMock();
        processor = new BlockInfoProcessorImpl(mock.getContext());
    }

    // this test is basically useless at this point since it only tests that expected db calls are
    // made
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

    // this test is basically useless at this point since it only tests that expected db calls are
    // made
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

        mock.addReturn("SELECT", userRes);
        mock.addReturn("SELECT", teamRes);

        BlockLeaderboardResponse res = processor.getBlockLeaderboards();

        assertEquals(2, mock.timesCalled("SELECT"));
        assertEquals(4, res.getTeams().size());
        assertEquals(3, res.getIndividuals().size());
        assertEquals(1, res.getTeams().get(0).getId());
        assertEquals(1, res.getIndividuals().get(0).getId());
    }

    // gets the name field of the given table
    private String getNameName(Table<? extends Record> table) {
        if (table.equals(TEAM)) {
            return TEAM.NAME.getName();
        } else if (table.equals(USERS)) {
            return USERS.USERNAME.getName();
        } else {
            throw new IllegalArgumentException("Expected TEAM or USERS table, got: " + table.getName());
        }
    }

    // programmatically builds the sql string that results from the buildSubQueryParts method
    private StringBuilder buildSubQueryPartsString(
            Table<? extends Record> table, BlockStatus status, boolean isTeam, boolean mainQuery) {
        String isCompleted;
        String isReserved;
        String block;
        // if this is being built as part of the composeFullQuery method, inset extra statements
        String asInsert = mainQuery ? " as" : "";

        // get block status sql inserts based off of BlockStatus
        if (status == BlockStatus.DONE) {
            isCompleted = "\"completed\".\"fid\"";
            isReserved = "null";
            block = "\"completed\"";
        } else {
            isCompleted = "null";
            isReserved = "\"reserved\".\"fid\"";
            block = "\"reserved\"";
        }

        // build query up to the first join
        StringBuilder verificationQuery =
                new StringBuilder(
                        "select \""
                                + table.getName()
                                + "\".\"id\", \""
                                + table.getName()
                                + "\".\""
                                + getNameName(table)
                                + "\", "
                                + isCompleted
                                + asInsert
                                + " \"isCompleted\", "
                                + isReserved
                                + asInsert
                                + " \"isReserved\" from \""
                                + table.getName()
                                + "\" ");

        // if isTeam, then add user_team join
        if (isTeam) {
            verificationQuery.append(
                    "join \"user_team\" on " + "\"user_team\".\"team_id\" = \"team\".\"id\" ");
        }

        // add block join up to on statement
        verificationQuery.append("join \"block\"" + asInsert + " " + block + " on (\"");

        // if isTeam, join on user_team user_id, otherwise join on table's id column
        if (isTeam) {
            verificationQuery.append("user_team\".\"user_id\" ");
        } else {
            verificationQuery.append(table.getName() + "\".\"id\" ");
        }

        // build query to end
        verificationQuery.append("= " + block + ".\"assigned_to\" and " + block + ".\"status\" = ?)");
        return verificationQuery;
    }

    // test that buildSubQueryParts returns the correct sql query for USERS
    @ParameterizedTest
    @EnumSource(BlockStatus.class)
    public void testBuildSubQueryPartsUsers(BlockStatus status) {
        SelectJoinStep<Record4<Integer, String, String, String>> rawResult =
                processor.buildSubQueryParts(USERS, status, false);
        String verificationQuery =
                this.buildSubQueryPartsString(USERS, status, false, false).toString();
        assertEquals(verificationQuery, rawResult.getSQL());
        assertEquals(1, rawResult.getBindValues().size());
        assertEquals(status, rawResult.getBindValues().get(0));
    }

    // test that buildSubQueryParts returns the correct sql query for TEAM
    @ParameterizedTest
    @EnumSource(BlockStatus.class)
    public void testBuildSubQueryPartsTeam(BlockStatus status) {
        SelectJoinStep<Record4<Integer, String, String, String>> rawResult =
                processor.buildSubQueryParts(TEAM, status, true);
        String verificationQuery = this.buildSubQueryPartsString(TEAM, status, true, false).toString();
        assertEquals(verificationQuery, rawResult.getSQL());
        assertEquals(1, rawResult.getBindValues().size());
        assertEquals(status, rawResult.getBindValues().get(0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testBuildSubQueryPartsInvalidTable(boolean isTeam) {
        String errorMessage = "Table must be TEAM or USERS, was: user_team";
        try {
            processor.buildSubQueryParts(USER_TEAM, BlockStatus.DONE, isTeam);
        } catch (IllegalArgumentException e) {
            assertEquals(errorMessage, e.getMessage());
        }
    }

    // programmatically builds the sql string that results from the buildSubQuery method
    private StringBuilder buildSubQueryString(
            Table<? extends Record> table, boolean isTeam, boolean mainQuery) {
        StringBuilder result = new StringBuilder("(");
        result.append(buildSubQueryPartsString(table, BlockStatus.DONE, isTeam, mainQuery));
        result.append(") union (");
        result.append(buildSubQueryPartsString(table, BlockStatus.RESERVED, isTeam, mainQuery));
        result.append(")");

        return result;
    }

    // test that buildSubQuery returns the correct sql query for TEAM and USERS
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testBuildSubQuery(boolean isTeam) {
        Table<? extends Record> table = isTeam ? TEAM : USERS;
        Select<Record4<Integer, String, String, String>> subQuery =
                processor.buildSubQuery(table, isTeam);
        String verificationQuery = buildSubQueryString(table, isTeam, false).toString();

        assertEquals(verificationQuery, subQuery.getSQL());
        assertEquals(2, subQuery.getBindValues().size());
        assertEquals(BlockStatus.DONE, subQuery.getBindValues().get(0));
        assertEquals(BlockStatus.RESERVED, subQuery.getBindValues().get(1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testBuildSubQueryInvalidTable(boolean isTeam) {
        String errorMessage = "Table must be TEAM or USERS, was: user_team";
        try {
            processor.buildSubQuery(USER_TEAM, isTeam);
        } catch (IllegalArgumentException e) {
            assertEquals(errorMessage, e.getMessage());
        }
    }

    // programmatically builds the sql string that results from the composeFullQuery method
    private StringBuilder buildFullQueryString(Table<? extends Record> table, boolean isTeam) {
        // create wrapper select statement up to from statement
        StringBuilder result =
                new StringBuilder(
                        "select \"id\", \""
                                + getNameName(table)
                                + "\", count(\"subQuery\".\"isCompleted\") as \"blocksCompleted\", "
                                + "count(\"subQuery\".\"isReserved\") as \"blocksReserved\" from (");
        // select FROM result of buildSubQueryString
        result.append(buildSubQueryString(table, isTeam, true));
        // finish the rest of the sql query
        result.append(
                ") as \"subQuery\" group by \"id\", \""
                        + getNameName(table)
                        + "\" order by \"blocksCompleted\" desc, \"blocksReserved\" desc limit ?");
        return result;
    }

    // test that buildQuery returns the correct sql query for TEAM and USERS
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testComposeFullQuery(boolean isTeam) {
        Table<? extends Record> table = isTeam ? TEAM : USERS;
        SelectFinalStep<Record4<Integer, String, Integer, Integer>> query =
                processor.composeFullQuery(table, isTeam);
        String verificationQuery = buildFullQueryString(table, isTeam).toString();

        assertEquals(verificationQuery, query.getSQL());
        assertEquals(3, query.getBindValues().size());
        assertEquals(BlockStatus.DONE, query.getBindValues().get(0));
        assertEquals(BlockStatus.RESERVED, query.getBindValues().get(1));
        assertEquals("10", query.getBindValues().get(2).toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testBuildQueryInvalidTable(boolean isTeam) {
        String errorMessage = "Table must be TEAM or USERS, was: user_team";
        try {
            processor.composeFullQuery(USER_TEAM, isTeam);
        } catch (IllegalArgumentException e) {
            assertEquals(errorMessage, e.getMessage());
        }
    }
}