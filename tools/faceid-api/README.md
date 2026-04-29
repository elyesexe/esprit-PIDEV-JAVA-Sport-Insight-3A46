# Face ID API (FastAPI)

## 1) Install

```bash
cd tools/faceid-api
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

## 2) Configure (optional)

Environment variables:

- `FACE_DB_HOST` (default `127.0.0.1`)
- `FACE_DB_PORT` (default `3306`)
- `FACE_DB_NAME` (default `sport_insight`)
- `FACE_DB_USER` (default `root`)
- `FACE_DB_PASSWORD` (default empty)
- `FACE_ID_THRESHOLD` (default `0.48`)

## 3) Run

```bash
uvicorn main:app --host 127.0.0.1 --port 8000 --reload
```

## 4) Endpoints

- `GET /health`
- `POST /face/enroll` (`user_id`, `image`)
- `POST /face/verify` (`user_id`, `image`)
- `DELETE /face/profile/{user_id}`

The Java app calls `http://127.0.0.1:8000` by default.
Override with:

- JVM property: `-Dface.id.api.base=http://...`
- or env var: `FACE_ID_API_BASE`
