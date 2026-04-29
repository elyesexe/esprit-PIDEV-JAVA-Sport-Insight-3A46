import io
import json
import os
from contextlib import contextmanager
from typing import Any

import face_recognition
import mysql.connector
import numpy as np
from fastapi import FastAPI, File, Form, HTTPException, UploadFile


APP_NAME = "Sport Insight Face ID API"
MODEL_NAME = "face_recognition"
# Smaller is stricter. Typical face_recognition threshold around 0.6.
VERIFY_THRESHOLD = float(os.getenv("FACE_ID_THRESHOLD", "0.48"))

DB_HOST = os.getenv("FACE_DB_HOST", "127.0.0.1")
DB_PORT = int(os.getenv("FACE_DB_PORT", "3306"))
DB_NAME = os.getenv("FACE_DB_NAME", "sport_insight")
DB_USER = os.getenv("FACE_DB_USER", "root")
DB_PASSWORD = os.getenv("FACE_DB_PASSWORD", "")

app = FastAPI(title=APP_NAME, version="1.0.0")


@contextmanager
def db_connection():
    connection = mysql.connector.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        autocommit=False,
    )
    try:
        yield connection
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


def ensure_schema() -> None:
    with db_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS face_profile (
                    user_id INT NOT NULL,
                    embedding_json LONGTEXT NOT NULL,
                    model_name VARCHAR(120) NOT NULL DEFAULT 'face_recognition',
                    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id),
                    CONSTRAINT fk_face_profile_user
                        FOREIGN KEY (user_id) REFERENCES `user`(id)
                        ON DELETE CASCADE ON UPDATE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """
            )


@app.on_event("startup")
def startup() -> None:
    ensure_schema()


@app.get("/health")
def health() -> dict[str, Any]:
    return {
        "ok": True,
        "service": APP_NAME,
        "threshold": VERIFY_THRESHOLD,
        "db_host": DB_HOST,
        "db_name": DB_NAME,
    }


@app.post("/face/enroll")
async def enroll_face(
    user_id: int = Form(...),
    image: UploadFile = File(...),
) -> dict[str, Any]:
    encoding = await extract_face_encoding(image)
    with db_connection() as connection:
        with connection.cursor() as cursor:
            ensure_user_exists(cursor, user_id)
            cursor.execute(
                """
                INSERT INTO face_profile (user_id, embedding_json, model_name)
                VALUES (%s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    embedding_json = VALUES(embedding_json),
                    model_name = VALUES(model_name)
                """,
                (user_id, json.dumps(encoding.tolist()), MODEL_NAME),
            )

    return {"success": True, "enrolled": True, "message": "Face profile enrolled."}


@app.post("/face/verify")
async def verify_face(
    user_id: int = Form(...),
    image: UploadFile = File(...),
) -> dict[str, Any]:
    probe_encoding = await extract_face_encoding(image)
    profile = fetch_profile(user_id)
    if profile is None:
        raise HTTPException(status_code=404, detail="No face profile found for this user.")

    reference_encoding = np.array(profile["embedding"], dtype=np.float64)
    distance = float(face_recognition.face_distance([reference_encoding], probe_encoding)[0])
    verified = distance <= VERIFY_THRESHOLD

    return {
        "success": True,
        "verified": verified,
        "distance": round(distance, 6),
        "threshold": VERIFY_THRESHOLD,
        "message": "Face verification passed." if verified else "Face verification failed.",
    }


@app.delete("/face/profile/{user_id}")
def delete_profile(user_id: int) -> dict[str, Any]:
    with db_connection() as connection:
        with connection.cursor() as cursor:
            cursor.execute("DELETE FROM face_profile WHERE user_id = %s", (user_id,))
            deleted = cursor.rowcount > 0
    return {"success": True, "deleted": deleted}


def fetch_profile(user_id: int) -> dict[str, Any] | None:
    with db_connection() as connection:
        with connection.cursor(dictionary=True) as cursor:
            cursor.execute(
                "SELECT user_id, embedding_json, model_name FROM face_profile WHERE user_id = %s LIMIT 1",
                (user_id,),
            )
            row = cursor.fetchone()

    if row is None:
        return None

    try:
        embedding = json.loads(row["embedding_json"])
        if not isinstance(embedding, list) or len(embedding) == 0:
            raise ValueError("Invalid embedding format.")
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Corrupted face profile data: {exc}") from exc

    return {"user_id": row["user_id"], "embedding": embedding, "model_name": row["model_name"]}


def ensure_user_exists(cursor: mysql.connector.cursor.MySQLCursor, user_id: int) -> None:
    cursor.execute("SELECT id FROM user WHERE id = %s LIMIT 1", (user_id,))
    if cursor.fetchone() is None:
        raise HTTPException(status_code=404, detail="User not found.")


async def extract_face_encoding(upload: UploadFile) -> np.ndarray:
    raw = await upload.read()
    if not raw:
        raise HTTPException(status_code=400, detail="Empty image upload.")

    try:
        image_array = face_recognition.load_image_file(io.BytesIO(raw))
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Invalid image file: {exc}") from exc

    locations = face_recognition.face_locations(image_array, model="hog")
    if len(locations) == 0:
        raise HTTPException(status_code=422, detail="No face detected.")
    if len(locations) > 1:
        raise HTTPException(status_code=422, detail="Multiple faces detected. Keep one face only.")

    encodings = face_recognition.face_encodings(image_array, known_face_locations=locations, model="small")
    if len(encodings) == 0:
        raise HTTPException(status_code=422, detail="Face detected but embedding extraction failed.")
    return encodings[0]
