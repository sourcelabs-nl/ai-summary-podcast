# Capability: User Management

## Purpose

User registration and lifecycle management, providing identity for podcast ownership and API key storage.

## Requirements

### Requirement: User entity with UUID identity
The system SHALL store users in a `users` database table with columns: `id` (TEXT, primary key, UUID), `name` (TEXT, NOT NULL), and `version` (BIGINT, NOT NULL, DEFAULT 0). The `id` SHALL be generated as a UUID v4 upon creation. Entities with pre-populated String `@Id` fields SHALL use a `@Version` field so Spring Data JDBC can distinguish new entities (version=null) from existing ones, ensuring `save()` performs an INSERT rather than an UPDATE.

#### Scenario: Create a new user
- **WHEN** a `POST /users` request is received with a JSON body containing `name`
- **THEN** the system creates a user record with a generated UUID, stores it in the database, and returns the created user with HTTP 201

#### Scenario: Create user with missing name
- **WHEN** a `POST /users` request is received without `name`
- **THEN** the system returns HTTP 400 with a validation error message

### Requirement: List all users
The system SHALL provide an endpoint to retrieve all registered users.

#### Scenario: List users
- **WHEN** a `GET /users` request is received
- **THEN** the system returns HTTP 200 with a JSON array of all users (id, name)

#### Scenario: List users when none exist
- **WHEN** a `GET /users` request is received and no users are registered
- **THEN** the system returns HTTP 200 with an empty JSON array

### Requirement: Get single user
The system SHALL provide an endpoint to retrieve a specific user by ID.

#### Scenario: Get existing user
- **WHEN** a `GET /users/{userId}` request is received with a valid user ID
- **THEN** the system returns HTTP 200 with the user's details (id, name)

#### Scenario: Get non-existing user
- **WHEN** a `GET /users/{userId}` request is received with an ID that does not exist
- **THEN** the system returns HTTP 404

### Requirement: Update user
The system SHALL allow updating a user's `name` field.

#### Scenario: Update user name
- **WHEN** a `PUT /users/{userId}` request is received with a JSON body containing `name`
- **THEN** the system updates the user record and returns HTTP 200 with the updated user

#### Scenario: Update non-existing user
- **WHEN** a `PUT /users/{userId}` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

### Requirement: Delete user with cascade
The system SHALL allow deleting a user, which MUST cascade-delete all of the user's podcasts, their sources, associated articles, episodes, and audio files on disk.

#### Scenario: Delete existing user
- **WHEN** a `DELETE /users/{userId}` request is received for an existing user
- **THEN** the system deletes the user, all their podcasts, all sources within those podcasts, all articles belonging to those sources, all episodes within those podcasts, removes associated audio files from disk, and returns HTTP 204

#### Scenario: Delete non-existing user
- **WHEN** a `DELETE /users/{userId}` request is received for a user that does not exist
- **THEN** the system returns HTTP 404
