# Capability: Podcast Image

## Purpose

Upload, serve, and manage podcast artwork images for use in RSS feeds and external publishing.

## Requirements

### Requirement: Podcast image upload endpoint
The system SHALL provide a `POST /users/{userId}/podcasts/{podcastId}/image` endpoint that accepts a multipart file upload for podcast artwork. The endpoint SHALL validate:
1. The file content type is an image (`image/jpeg`, `image/png`, or `image/webp`)
2. The file size does not exceed 1MB (1,048,576 bytes)
3. The file's magic bytes match the declared content type

On success, the image SHALL be stored at `data/episodes/{podcastId}/podcast-image.{ext}` (where `ext` is derived from the content type: `jpg`, `png`, or `webp`). Any previously stored podcast image SHALL be replaced. The endpoint SHALL return HTTP 200 with the stored image path.

#### Scenario: Upload valid JPEG image
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/image` request is received with a valid 500KB JPEG file
- **THEN** the file is stored at `data/episodes/{podcastId}/podcast-image.jpg` and HTTP 200 is returned

#### Scenario: Upload valid PNG image
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/image` request is received with a valid PNG file
- **THEN** the file is stored at `data/episodes/{podcastId}/podcast-image.png` and HTTP 200 is returned

#### Scenario: Upload replaces existing image
- **WHEN** a podcast already has a `podcast-image.jpg` and a new PNG is uploaded
- **THEN** the old `podcast-image.jpg` is deleted and the new `podcast-image.png` is stored

#### Scenario: File too large
- **WHEN** a `POST .../image` request is received with a 2MB file
- **THEN** HTTP 400 is returned with a message indicating the maximum file size is 1MB

#### Scenario: Not an image
- **WHEN** a `POST .../image` request is received with a PDF file
- **THEN** HTTP 400 is returned with a message indicating only JPEG, PNG, and WebP images are accepted

#### Scenario: Podcast not found
- **WHEN** a `POST .../image` request is received for a non-existing podcast
- **THEN** HTTP 404 is returned

### Requirement: Get podcast image endpoint
The system SHALL provide a `GET /users/{userId}/podcasts/{podcastId}/image` endpoint that returns the podcast's stored artwork. If no image exists, it SHALL return HTTP 404.

#### Scenario: Image exists
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/image` request is received and a podcast image is stored
- **THEN** HTTP 200 is returned with the image file and the appropriate content type header

#### Scenario: No image
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/image` request is received and no podcast image exists
- **THEN** HTTP 404 is returned

### Requirement: Delete podcast image endpoint
The system SHALL provide a `DELETE /users/{userId}/podcasts/{podcastId}/image` endpoint that removes the podcast's stored artwork.

#### Scenario: Delete existing image
- **WHEN** a `DELETE /users/{userId}/podcasts/{podcastId}/image` request is received and an image exists
- **THEN** the image file is deleted and HTTP 204 is returned

#### Scenario: No image to delete
- **WHEN** a `DELETE /users/{userId}/podcasts/{podcastId}/image` request is received and no image exists
- **THEN** HTTP 404 is returned
