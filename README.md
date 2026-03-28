# ILoveMusic Backend

A Spring Boot microservice for music playlist management with Spotify/YouTube OAuth2 integration, JWT authentication, and PostgreSQL persistence.

## Project Structure

```
ilovemusic-backend/
├── src/main/java/com/ilovemusic/
│   ├── IlovemusicApplication.java          # Main Spring Boot Application
│   ├── config/
│   │   ├── SecurityConfig.java             # Spring Security & JWT configuration
│   │   ├── AppConfig.java                  # Application beans configuration
│   │   └── JwtAuthenticationFilter.java    # JWT token validation filter
│   ├── controller/
│   │   ├── AuthController.java             # Authentication endpoints
│   │   └── PlaylistController.java         # Playlist management endpoints
│   ├── entity/
│   │   ├── User.java                       # User JPA entity
│   │   ├── OAuthToken.java                 # OAuth token storage entity
│   │   ├── Playlist.java                   # Playlist JPA entity
│   │   └── Track.java                      # Track JPA entity
│   ├── repository/
│   │   ├── UserRepository.java             # User data access layer
│   │   ├── OAuthTokenRepository.java       # OAuth token data access layer
│   │   ├── PlaylistRepository.java         # Playlist data access layer
│   │   └── TrackRepository.java            # Track data access layer
│   ├── service/
│   │   ├── SpotifyService.java             # Spotify API integration
│   │   ├── YouTubeService.java             # YouTube API integration
│   │   └── PlaylistService.java            # Playlist business logic
│   ├── dto/
│   │   ├── PlaylistDTO.java                # Playlist data transfer object
│   │   └── TrackDTO.java                   # Track data transfer object
│   └── util/
│       └── JwtUtil.java                    # JWT token utilities
├── src/main/resources/
│   └── application.yml                     # Application configuration
├── build.gradle                            # Gradle build configuration
└── README.md                               # This file
```

## Technologies & Dependencies

- **Framework**: Spring Boot 3.3.5
- **Security**: Spring Security with OAuth2 & JWT (jjwt 0.12.3)
- **Database**: PostgreSQL with Spring Data JPA
- **ORM**: Hibernate
- **HTTP Client**: Spring WebFlux & RestTemplate
- **Utilities**: Lombok for boilerplate reduction
- **Java Version**: Java 17+

## Setup & Installation

### Prerequisites

- Java 17 or higher
- PostgreSQL 12+
- Gradle 8.0+

### Environment Variables

Create a `.env` file or set system environment variables:

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/ilovemusic
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your-super-secret-key-that-should-be-at-least-256-bits-long-for-HS256

# Spotify OAuth Configuration
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret

# YouTube/Google OAuth Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
YOUTUBE_API_KEY=your_youtube_api_key

# Application URLs
APP_BASE_URL=http://localhost:8080
FRONTEND_URL=http://localhost:3000
```

### Build & Run

```bash
# Build the project
gradle build

# Run the application
gradle bootRun

# Or using the JAR
java -jar build/libs/ilovemusic-backend-0.0.1-SNAPSHOT.jar
```

The server will start on `http://localhost:8080`

## API Endpoints

### Authentication

- **POST** `/api/auth/register` - Register a new user with username/email/password
- **POST** `/api/auth/login` - Login with username/password
- **POST** `/api/auth/refresh-token` - Refresh JWT token
- **POST** `/api/auth/logout` - Logout
- **GET** `/api/auth/connections` - Check Spotify and YouTube connections
- **GET** `/api/auth/spotify` - Get Spotify OAuth redirect URL
- **GET** `/api/auth/spotify/callback` - Spotify OAuth callback
- **GET** `/api/auth/youtube` - Get YouTube OAuth redirect URL
- **GET** `/api/auth/youtube/callback` - YouTube OAuth callback

### Playlist Management

- **POST** `/api/v1/playlists` - Create a new playlist
- **GET** `/api/v1/playlists` - Get all playlists for current user (supports optional `platform` query parameter)
- **GET** `/api/v1/playlists/{playlistId}` - Get specific playlist
- **PUT** `/api/v1/playlists/{playlistId}` - Update playlist
- **DELETE** `/api/v1/playlists/{playlistId}` - Delete playlist
- **POST** `/api/v1/playlists/{playlistId}/tracks` - Add track to playlist
- **GET** `/api/v1/playlists/{playlistId}/tracks` - Get all tracks in playlist
- **DELETE** `/api/v1/playlists/{playlistId}/tracks/{trackId}` - Remove track from playlist
- **POST** `/api/v1/playlists/export` - Export playlist between platforms (requires `playlistId`, `fromPlatform`, `toPlatform` query parameters)

## Core Components

### Controllers

#### AuthController
Handles user authentication and OAuth flows:
- User login/logout
- JWT token generation and refresh
- Spotify OAuth integration
- YouTube OAuth integration

#### PlaylistController
Manages playlist CRUD operations and track management:
- Create, read, update, delete playlists
- Add/remove tracks from playlists
- Retrieve playlist contents

### Services

#### PlaylistService
Core business logic for playlist management:
- Playlist creation and management
- Track management within playlists
- Data transformation (Entity ↔ DTO)

#### SpotifyService
Spotify API integration:
- Fetch track information
- Search tracks on Spotify
- Retrieve user's Spotify playlists

#### YouTubeService
YouTube API integration:
- Fetch video information
- Search videos on YouTube
- Retrieve user's YouTube playlists

### Entities

#### User
Represents a user in the system with OAuth integration:
- Email and username
- Spotify and YouTube IDs
- OAuth tokens and playlists relationships

#### OAuthToken
Stores OAuth access/refresh tokens:
- Provider (Spotify or YouTube)
- Access and refresh tokens
- Token expiration information

#### Playlist
Represents a music playlist:
- Owner (User relationship)
- Name, description, cover image
- Collection of tracks

#### Track
Represents a music track:
- Title, artist, album information
- Platform IDs (Spotify/YouTube)
- Duration and source information

### DTOs

#### PlaylistDTO
Transfer object for playlist data with nested tracks

#### TrackDTO
Transfer object for track data

### Security

#### SecurityConfig
Spring Security configuration:
- JWT authentication filter registration
- CORS configuration
- OAuth2 client setup
- Authorization rules

#### JwtAuthenticationFilter
Custom filter for JWT validation:
- Extracts token from Authorization header
- Validates token signature and expiration
- Sets Spring Security context

#### JwtUtil
JWT token utilities:
- Token generation with claims
- Token validation
- Claims extraction

## Database Schema

### users
- `id`: Primary key
- `email`: Unique email
- `username`: Unique username
- `spotify_id`: Spotify user ID
- `youtube_id`: YouTube user ID
- `created_at`: Timestamp
- `updated_at`: Timestamp

### oauth_tokens
- `id`: Primary key
- `user_id`: Foreign key to users
- `provider`: "spotify" or "youtube"
- `access_token`: OAuth access token
- `refresh_token`: OAuth refresh token
- `expires_in`: Token expiration time
- `created_at`: Timestamp
- `updated_at`: Timestamp

### playlists
- `id`: Primary key
- `user_id`: Foreign key to users
- `name`: Playlist name
- `description`: Playlist description
- `cover_image_url`: Cover image URL
- `created_at`: Timestamp
- `updated_at`: Timestamp

### tracks
- `id`: Primary key
- `playlist_id`: Foreign key to playlists
- `title`: Track title
- `artist`: Artist name
- `album`: Album name
- `spotify_id`: Spotify track ID
- `youtube_id`: YouTube video ID
- `cover_image_url`: Cover image URL
- `duration`: Duration in seconds
- `source`: "spotify" or "youtube"
- `created_at`: Timestamp

## Development

### IDE Setup

This project works with IntelliJ IDEA, Eclipse, and VS Code. Ensure:
- Lombok annotation processor is enabled
- JDK 17+ is configured

### Building & Testing

```bash
# Build only
gradle build --no-test

# Run tests
gradle test

# Run specific test
gradle test --tests TestClassName
```

## TODO / Future Enhancements

1. Implement full Spotify API integration for playlist sync
2. Implement full YouTube API integration for video search
3. Add comprehensive exception handling
4. Implement password hashing and validation
5. Add email verification for new users
6. Implement playlist sharing functionality
7. Add unit and integration tests
8. Add API documentation with Swagger/OpenAPI
9. Implement caching strategy
10. Add rate limiting and throttling

## Contributing

1. Clone the repository
2. Create a feature branch
3. Commit changes with clear messages
4. Push to the branch
5. Submit a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues or questions, please open an issue on the project repository.

