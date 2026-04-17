# Client Registration API - Frontend Documentation

## Overview
This API allows users to register, view, update, and delete client accounts.

**Base URL (Local):** `hostedon Render`

---

## API Endpoints

| Method | Endpoint                        | Description                    | Request Body |
|--------|---------------------------------|--------------------------------|--------------|
| POST   | `/api/Client/add`               | Register a new client          | Yes          |
| GET    | `/api/Client/all`               | Get all clients                | No           |
| GET    | `/api/Client/email/{email}`     | Get client by email            | No           |
| PUT    | `/api/Client/update/{id}`       | Update client details          | Yes          |
| DELETE | `/api/Client/delete/{id}`       | Delete client                  | No           |

---

## 1. Register a New Client (Most Important)

**POST** `http://localhost:8080/api/Client/add`

### Request Body (JSON)

```json
{
  "username": "lunga_mthethwa",
  "fullName": "Lunga Mthethwa",
  "email": "lunga@example.com",
  "passwordHash": "MyStrongPassword123!",   // ← Send plain password (backend will hash it)
  "role": "user"                            // Optional, default is "user"
}
