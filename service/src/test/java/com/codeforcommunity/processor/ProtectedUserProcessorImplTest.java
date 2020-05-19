package com.codeforcommunity.processor;

import com.codeforcommunity.processor.ProtectedUserProcessorImpl;

import com.codeforcommunity.auth.Passwords;
import com.codeforcommunity.enums.PrivilegeLevel;
import com.codeforcommunity.dto.user.ChangePasswordRequest;
import com.codeforcommunity.exceptions.UserDoesNotExistException;
import com.codeforcommunity.exceptions.WrongPasswordException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.generated.tables.records.UsersRecord;
import org.jooq.generated.Tables;
import java.util.ArrayList;
import com.codeforcommunity.JooqMock;
import com.codeforcommunity.auth.JWTData;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.fail;

public class ProtectedUserProcessorImplTest {
    // the JooqMock to use for testing
    JooqMock mockDb;
    // the ProcessorImpl to use for testing
    ProtectedUserProcessorImpl processor;

    /**
     * Method to setup mockDb and processor.
     */
    void setup() {
        this.mockDb = new JooqMock();
        this.processor = new ProtectedUserProcessorImpl(mockDb.getContext());
    }

    // successfully deletes user
    @Test
    public void testDeleteUser1() {
        setup();
        UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
        myUser.setUsername("kiminusername");
        myUser.setEmail("kimin@example.com");
        myUser.setPassHash(Passwords.createHash("password"));
        myUser.setId(1);
        myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
        mockDb.addReturn("SELECT", myUser);
        mockDb.addReturn("DELETE", myUser);

        JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

        this.processor.deleteUser(jwtData);
        assertEquals(1, mockDb.timesCalled("SELECT"));
        assertEquals(3, mockDb.timesCalled("DELETE"));
    }

    // attempts to delete user when there are no users
    @Test
    public void testDeleteUser2() {
        setup();

        UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
        myUser.setUsername("kiminusername");
        myUser.setEmail("kimin@example.com");
        myUser.setPassHash(Passwords.createHash("password"));
        myUser.setId(1);
        myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
        mockDb.addReturn("SELECT", myUser);
        mockDb.addReturn("DELETE", myUser);
        mockDb.addReturn("DELETE", myUser);

        JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);
        this.processor.deleteUser(jwtData);
        assertEquals(1, mockDb.timesCalled("SELECT"));
        assertEquals(2, mockDb.timesCalled("DELETE"));
    }

    // UserDoesNotExistException because user does not exist
    @Test
    public void testChangePassword1() {
        setup();
        mockDb.addReturn("SELECT", new ArrayList<UpdatableRecordImpl>());
        JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

        // ChangePasswordRequest
        ChangePasswordRequest changePwRequest = new ChangePasswordRequest("password", "newPassword");
        try {
            this.processor.changePassword(jwtData, changePwRequest);
            fail();
        } catch (UserDoesNotExistException e) {
            assertEquals(e.getIdentifierMessage(), "id = 1");
        }
        assertEquals(1, mockDb.timesCalled("SELECT"));
    }

    // WrongPasswordException because user inputted wrong current password
    @Test
    public void testChangePassword2() {
        setup();
        String myPw = "password";
        String wrongPw = "wrongPassword";

        UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
        myUser.setUsername("kiminusername");
        myUser.setEmail("kimin@example.com");
        myUser.setPassHash(Passwords.createHash(myPw));
        myUser.setId(1);
        myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
        mockDb.addReturn("SELECT", myUser);

        JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

        // ChangePasswordRequest
        ChangePasswordRequest changePwRequest = new ChangePasswordRequest(wrongPw, "newPassword");
        try {
            this.processor.changePassword(jwtData, changePwRequest);
            fail();
        } catch (WrongPasswordException e) {
            assertNotEquals(wrongPw, myPw);
        }
    }

    // successfully changes password
    @Test
    public void testChangePassword3() {
        setup();
        String myPw = "password";
        String newPw = "newPassword";

        UsersRecord myUser = mockDb.getContext().newRecord(Tables.USERS);
        myUser.setUsername("kiminusername");
        myUser.setEmail("kimin@example.com");
        myUser.setPassHash(Passwords.createHash(myPw));
        myUser.setId(1);
        myUser.setPrivilegeLevel(PrivilegeLevel.STANDARD);
        mockDb.addReturn("SELECT", myUser);
        byte[] currentPw = myUser.getPassHash();
        JWTData jwtData = new JWTData(1, PrivilegeLevel.STANDARD);

        // ChangePasswordRequest
        ChangePasswordRequest changePwRequest = new ChangePasswordRequest(myPw, newPw);
        mockDb.addReturn("UPDATE", myUser);
        this.processor.changePassword(jwtData, changePwRequest);
        myUser.setPassHash(Passwords.createHash(newPw));
        assertNotEquals(myUser.getPassHash(), currentPw);
    }
}