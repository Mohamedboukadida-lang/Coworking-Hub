# Coworking-Hub

## Objective

Coworking-Hub is designed to streamline the process of finding, reserving, and utilizing coworking spaces. It caters to freelancers, entrepreneurs, and businesses seeking flexible and productive work environments. By centralizing various coworking options, users can effortlessly compare spaces, book amenities, and connect with like-minded professionals.

## Tools and Technologies

To build a robust and scalable application, we employed a combination of modern and efficient technologies:

- **Flutter**: Used for creating a seamless and responsive user interface. Flutter enables a cross-platform solution, ensuring our app delivers a consistent experience across both iOS and Android devices.

- **Spring Boot**: Powers the backend services, offering a straightforward approach to setting up and running web applications. It is the backbone that handles business logic, user authentication, and data processing.

- **PostgreSQL**: Our chosen database for secure and reliable storage of all application data, including user profiles, reservations, and space details. It provides comprehensive features and is well-suited for handling complex queries and transactions.

## Running the backend

The Spring Boot API lives in `coworkinghub/` (Java 21, Spring Boot 3.5). Database credentials are read from environment variables:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | *(empty)* |
| `SERVER_PORT` | `8081` |

```bash
cd coworkinghub
DB_PASSWORD=your-password ./mvnw spring-boot:run
```

Tests use an in-memory H2 database and need no PostgreSQL server: `./mvnw test`.

### API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/users/` | Create an account |
| `POST` | `/api/coworking-spaces/` | Create a coworking space |
| `POST` | `/api/coworking-spaces/{id}/add-random-equipment` | Add one sample item of each equipment type |
| `POST` | `/api/reservations/` | Create a reservation (status `PENDING`) |
| `PUT` | `/api/reservations/{id}/status` | Accept or decline a reservation |

Unknown IDs return `404` and invalid bodies return `400`, both as RFC 7807 problem details.

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
