# SFTT API Specification


In every request header is a JWT. The JWT will contain the user's id value and the user's privilege level.

## Privilege Levels
- `0` = untrusted user
- `1` = a regular user
- `2` = an admin user

It is assumed that the user making the request (as specified in the JWT header) is the person the block is being reserved / completed by.

## `POST /blocks/reserve`

Open -> Reserved


1. Update DB, add blocks to a user's assigned blocks
2. Call map API and update status of blocks to reserved


## `POST /blocks/finish`

Reserved -> Done

Whoever had the block in it's current reserve is the one who is credited with completion.

Must be called by an admin or by the person holding the block.

## `POST /blocks/release`

Reserved -> Open

Must be called by an admin or by the person holding the block.

## `POST /blocks/reset`

Done -> Open

Admin Only


### Request Body
```json
{
  "blocks": [
    "block_id STRING",
    ...
  ]
}
```


### Responses
Partial successes possible
- Everything is okay
- The block was not in the state that it should've been
- The user is not allowed to modify that block's state
- The fid doesn't match any block

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

##### `401 Unauthorized`



## `POST /blocks/reserve/admin`

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
