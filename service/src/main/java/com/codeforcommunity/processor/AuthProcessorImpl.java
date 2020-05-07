package com.codeforcommunity.processor;

import com.codeforcommunity.api.IAuthProcessor;
import com.codeforcommunity.auth.JWTCreator;
import com.codeforcommunity.auth.JWTData;
import com.codeforcommunity.dataaccess.AuthDatabaseOperations;
import com.codeforcommunity.dto.auth.SessionResponse;
import com.codeforcommunity.dto.auth.LoginRequest;
import com.codeforcommunity.dto.auth.NewUserRequest;
import com.codeforcommunity.dto.auth.RefreshSessionRequest;
import com.codeforcommunity.dto.auth.RefreshSessionResponse;
import com.codeforcommunity.exceptions.AuthException;
import com.codeforcommunity.exceptions.EmailAlreadyInUseException;
import com.codeforcommunity.exceptions.TokenInvalidException;
import org.jooq.DSLContext;

import java.util.Optional;

public class AuthProcessorImpl implements IAuthProcessor {

    private final AuthDatabaseOperations authDatabaseOperations;
    private final JWTCreator jwtCreator;

    public AuthProcessorImpl(DSLContext db, JWTCreator jwtCreator) {
        this.authDatabaseOperations = new AuthDatabaseOperations(db);
        this.jwtCreator = jwtCreator;
    }

    /**
     * Check that inputs are valid with the database
     * Creates a new refresh jwt
     * Creates a new access jwt
     * Creates a new user database row
     * Return the new jwts
     *
     * @throws EmailAlreadyInUseException if the given email is already used.
     */
    @Override
    public SessionResponse signUp(NewUserRequest request) {
        authDatabaseOperations.createNewUser(request.getUsername(), request.getEmail(), request.getPassword(),
            request.getFirstName(), request.getLastName());

        return setupSessionResponse(request.getEmail());
    }

    /**
     * Checks if username password combination is valid with database
     * Creates a new refresh jwt
     * Creates a new access jwt
     * Return the access and refresh jwts
     *
     * @throws AuthException if the given username password combination is invalid.
     */
    @Override
    public SessionResponse login(LoginRequest loginRequest) throws AuthException {
        if (authDatabaseOperations.isValidLogin(loginRequest.getEmail(), loginRequest.getPassword())) {
            return setupSessionResponse(loginRequest.getEmail());
        } else {
            throw new AuthException("Could not validate username password combination");
        }
    }

    /**
     * Add refresh jwt to the blacklist token database table
     */
    @Override
    public void logout(String refreshToken) {
        authDatabaseOperations.addToBlackList(getSignature(refreshToken));
    }

    /**
     * Checks if refresh jwt is valid
     * Checks if refresh jwt is blacklisted *uses database
     * Creates a new access jwt
     * Returns the access jwt
     *
     * @throws AuthException if the given refresh token is invalid.
     */
    @Override
    public RefreshSessionResponse refreshSession(RefreshSessionRequest request) throws AuthException {
        if(authDatabaseOperations.isOnBlackList(getSignature(request.getRefreshToken()))) {
            throw new AuthException("The refresh token has been invalidated by a previous logout");
        }

        Optional<String> accessToken = jwtCreator.getNewAccessToken(request.getRefreshToken());

        if (accessToken.isPresent()) {
            return new RefreshSessionResponse() {{
                setFreshAccessToken(accessToken.get());
            }};
        } else {
            throw new TokenInvalidException("refresh");
        }
    }

    @Override
    public void validateSecretKey(String secretKey) {
        authDatabaseOperations.validateSecretKey(secretKey);
    }

    /**
     * Given a valid user's email, get a corresponding refresh and access token
     * and return them as a SessionResponse object.
     */
    private SessionResponse setupSessionResponse(String email) {
        JWTData userData = authDatabaseOperations.getUserJWTData(email);
        String refreshToken = jwtCreator.createNewRefreshToken(userData);
        Optional<String> accessToken = jwtCreator.getNewAccessToken(refreshToken);

        if (accessToken.isPresent()) {
            return new SessionResponse() {{
                setAccessToken(accessToken.get());
                setRefreshToken(refreshToken);
            }};
        } else {
            // If this is thrown there is probably an error in our JWT creation / validation logic
            throw new IllegalStateException("Newly created refresh token was deemed invalid");
        }
    }

    /**
     * Gets the signature of a given JWT string. Will be the third segment of a JWT that is
     * partitioned by "." characters.
     */
    private String getSignature(String token) {
        return token.split("\\.")[2];
    }
}
