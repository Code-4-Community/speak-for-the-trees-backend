# SFTT API Specification

In every request header is a JWT. The JWT will contain the user's id value and the user's privilege level.

## Privilege Levels
- `0` = untrusted user
- `1` = a regular user
- `2` = an admin user

It is assumed that the user making the request (as specified in the JWT header) is the person the block is being reserved / completed by.

# Block Status Updates

## `POST api/v1/protected/blocks/reserve`

Open -> Reserved

Must be called on open blocks.

1. Update DB, add blocks to a user's assigned blocks
2. Call map API and update status of blocks to reserved


## `POST api/v1/protected/blocks/finish`

Reserved -> Done

Whoever had the block in it's current reserve is the one who is credited with completion.

Must be called by an admin or by the person holding the block.

## `POST api/v1/protected/blocks/release`

Reserved -> Open

Must be called by an admin or by the person holding the block.


## `POST api/v1/protected/blocks/reset`

Done -> Open

Must be called by an admin or the person that completed the block.

### reserve, finish, release, and reset share the same request bodies and possible responses.

### Request Body
```json
{
  "blocks": [
    "block_id STRING",
    ...
  ]
}
```

The block id will correspond to the id of a block stored in the ArcGIS map.

### Responses
Partial successes are possible.

Reasons a block may fail:
- The block was not in the state that it should've been
- The user is not allowed to modify that block's state
- The id doesn't match any block

##### `200 OK`
```json
{
  "successes": [
    "block_id STRING",
    ...
  ],
  "failures": [
    "block_id STRING",
    ...
  ]
}
```



## `POST api/v1/protected/blocks/reserve/admin`

Open -> Reserved

Admin Only

Can specify a specific user that a block is being assigned to.

### Request Body
```json
{
  "assigned_to": "user_id STRING",
  "blocks": [
    "block_id STRING",
    ...
  ]
}
```

### Responses

##### `200 OK`
```json
{
  "successes": [
    "block_id STRING",
    ...
  ],
  "failures": [
    "block_id STRING",
    ...
  ]
}
```

##### `400 BAD REQUEST`
The body was malformed or the specified user doesn't exist.

##### `401 Unauthorized`
The calling user was not an admin.





# Team Management

Members of a team have roles specified in the following table:

| Role Name      | teamRole |
|----------------|----------|
| General Member | 1        |
| Team Leader    | 2        |


## `POST /teams`

Create a team. The team will only contain the member that created it who is now specified as the team leader.

### Request

```json
{
  "name": STRING
}
```

## `POST /teams/:team_id/invite`

Invite someone to join a team. Will send an email to all specified people that includes a link. Link will direct them to the team page where they can join once they are authenticated.

### Request

```json
{
  "emails": [
    STRING,
    ...
  ]
}
```

## `POST /teams/:team_id/join`

Join this team. Any member that is not currently a part of a team can join any team.

### Request

No request body.

## `POST /teams/:team_id/leave`

Leave this team that you are a part of. Cannot be called by the leader of the team.

### Request

No request body.

## `POST /teams/:team_id/disband`

Disband this team. Leader only. The team will be removed from the public team list and all other members will now be free to join or create a different team.

### Request

No request body.

## `POST /teams/:team_id/members/:member_id/kick`

Kicks a member off this team. Leader only. That member is then allowed to join or create any team now.

### Request

No request body.


# Team and Leaderboard Data

## `GET /teams`

Gets a list of teams, names, member count.

### Responses

##### `200 OK`

```json
{
  "teams": [
    {
      "id": INT,
      "name": STRING,
      "memberCount": INT
    },
    ...
  ],
  "rowCount": INT
}
```

## `GET /teams/admin`

Gets a list of teams with their goal data.

### Responses

##### `200 OK`

```json
{
  "teams": [
    {
      "id": INT,
      "name": STRING,
      "goalCompletionDate": TIMESTAMP,
      "blocksCompleted": INT,
      "blocksReserved": INT,
      "goal": INT
    },
    ...
  ],
  "rowCount": INT
}
```

## `GET /teams/:team_id`

Gets the information for this specific team. Including the members with how many blocks each one has completed and reserved.

### Responses

##### `200 OK`

```json
{
  "id": INT,
  "name": STRING,
  "blocksCompleted": INT,
  "blocksReserved": INT,
  "members": [
    {
      "id": INT,
      "username": STRING,
      "blocksCompleted": INT,
      "blocksReserved": INT,
      "teamRole": INT
    },
    ...
  ],
}
```

The `members` list is sorted in descending order of number of blocks completed.

`teamRole` is an indicator of the member's role on the team. Currently there are only two roles: general member and team leader.


## `GET /blocks`

Gets blocks done, in progress, todo for all of Boston

### Responses

##### `200 OK`

```json
{
  "blocksCompleted": INT,
  "blocksReserved": INT,
  "blocksOpen": INT
}
```

## `GET /blocks/leaderboard`

Gets blocks completed leaderboard for both teams and individuals. Will include up-to the top 10 teams and the top 10 individuals

### Responses

##### `200 OK`

```json
{
  "teams": [
    {
      "id": INT,
      "name": STRING,
      "blocksCompleted": INT,
      "blocksReserved": INT
    },
    ...
  ],
  "individuals": [
    {
      "id": INT,
      "username": STRING,
      "blocksCompleted": INT,
      "blocksReserved": INT
    },
    ...
  ]
}
```

