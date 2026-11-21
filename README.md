# Coworking-Hub

## Objective

Coworking-Hub is designed to streamline the process of finding, reserving, and utilizing coworking spaces. It caters to freelancers, entrepreneurs, and businesses seeking flexible and productive work environments. By centralizing various coworking options, users can effortlessly compare spaces, book amenities, and connect with like-minded professionals.

## Tools and Technologies

To build a robust and scalable application, we employed a combination of modern and efficient technologies:

- **Flutter**: Used for creating a seamless and responsive user interface. Flutter enables a cross-platform solution, ensuring our app delivers a consistent experience across both iOS and Android devices.

- **Spring Boot**: Powers the backend services, offering a straightforward approach to setting up and running web applications. It is the backbone that handles business logic, user authentication, and data processing.

- **PostgreSQL**: Our chosen database for secure and reliable storage of all application data, including user profiles, reservations, and space details. It provides comprehensive features and is well-suited for handling complex queries and transactions.

## Running the backend

The Spring Boot API lives in `coworkinghub/` (Java 21, Spring Boot 3.5). Configuration comes from environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | *(empty)* | Database password |
| `JWT_SECRET` | *(random per start)* | HS256 signing key, at least 32 bytes. Set it in production, or tokens stop working after a restart |
| `JWT_EXPIRATION` | `PT24H` | Token lifetime (ISO-8601 duration) |
| `APP_SECURITY_REQUIRE_AUTH` | `false` | `true` requires a token on every endpoint except `/api/auth/**` |
| `SERVER_PORT` | `8081` | HTTP port |

```bash
cd coworkinghub
DB_PASSWORD=your-password JWT_SECRET=$(openssl rand -hex 32) ./mvnw spring-boot:run
```

Tests use an in-memory H2 database and need no PostgreSQL server: `./mvnw test`.

### Authentication

`POST /api/auth/register` and `POST /api/auth/login` return `{ "token": "...", "user": {...} }`. Send the token as `Authorization: Bearer <token>`. With `APP_SECURITY_REQUIRE_AUTH=false` (the default, so the released mobile app keeps working) the token is optional: when present it identifies the caller, who becomes the owner of spaces they create and the user of reservations they make. An invalid or expired token is always rejected with `401`. Passwords are stored as BCrypt hashes and never returned.

### Pricing

A reservation is placed in the cheapest room of the space that can hold `numberOfPeople`:
`totalPrice = (room.pricePerPersonPerHour × numberOfPeople + sum of equipment prices) × hours`.
If no room is large enough the request is rejected with `400`, so a space needs at least one room before it can be booked.

### API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Create an account (USER or OWNER) and get a token |
| `POST` | `/api/auth/login` | Log in and get a token |
| `POST` | `/api/users/` | Create an account (used by the current mobile app) |
| `GET` | `/api/users/me` | The authenticated account |
| `GET` / `PUT` | `/api/users/{id}` | Read / update a profile |
| `GET` / `POST` | `/api/coworking-spaces/` | List / create coworking spaces |
| `GET` / `PUT` / `DELETE` | `/api/coworking-spaces/{id}` | Read / update / delete a space (`409` while it still has rooms, equipment or reservations) |
| `GET` / `POST` | `/api/coworking-spaces/{id}/rooms` | List / add rooms (`capacity`, `pricePerPersonPerHour`) |
| `GET` | `/api/coworking-spaces/{id}/equipment` | List a space's equipment |
| `POST` | `/api/coworking-spaces/{id}/add-random-equipment` | Add one sample item of each equipment type |
| `GET` / `POST` | `/api/reservations/` | List (filter with `?userId=` / `?coworkingSpaceId=`) / create a reservation |
| `GET` | `/api/reservations/me` | The authenticated account's reservations |
| `GET` / `DELETE` | `/api/reservations/{id}` | Read / delete a reservation |
| `PUT` | `/api/reservations/{id}/status` | Accept or decline a reservation |

Errors are RFC 7807 problem details: `400` invalid input, `401` missing/invalid token, `404` unknown ID, `409` conflict.

## Key Features

- **Space Discovery**: Users can explore a variety of coworking spaces with detailed profiles, including amenities, location, pricing, and availability.

- **Real-Time Reservations**: The app offers a real-time booking system, allowing users to reserve spaces and equipment instantly.

- **Interactive Maps**: Integrated mapping and location services enable users to find coworking spaces in their vicinity or search by address.

- **User Profiles**: Professionals can create profiles, showcasing their skills and interests to foster community connections within coworking spaces.

- **Ratings and Reviews**: After using a space, users can leave ratings and reviews, assisting others in making informed decisions.

- **Admin Panel**: For space owners and administrators, the app includes a backend panel to manage listings, view analytics, and communicate with users.

## Additional Information

Coworking-Hub also emphasizes community building and networking by including features such as event listings, discussion forums, and direct messaging. These elements are designed to promote collaboration and innovation among users.

In summary, Coworking-Hub leverages cutting-edge technology to offer an all-in-one platform that simplifies the process of finding and managing coworking spaces, fostering a vibrant and connected community of professionals.
