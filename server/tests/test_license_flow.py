import os
from pathlib import Path


def test_license_activation_and_check(tmp_path: Path) -> None:
    os.environ["DINGPAY_DATABASE_URL"] = f"sqlite:///{tmp_path / 'test.db'}"
    os.environ["DINGPAY_SECRET_KEY"] = "test-secret"
    os.environ["DINGPAY_ADMIN_USERNAME"] = "admin"
    os.environ["DINGPAY_ADMIN_PASSWORD"] = "admin123"

    from fastapi.testclient import TestClient
    from server.app.main import app

    with TestClient(app) as client:
        login = client.post("/api/admin/login", json={"username": "admin", "password": "admin123"})
        assert login.status_code == 200
        token = login.json()["token"]
        headers = {"Authorization": f"Bearer {token}"}

        generated = client.post(
            "/api/admin/cards/generate",
            json={"count": 1, "durationDays": 7},
            headers=headers,
        )
        assert generated.status_code == 200
        license_key = generated.json()["licenseKeys"][0]

        activated = client.post(
            "/api/license/activate",
            json={"licenseKey": license_key, "deviceId": "device-1", "appVersion": "1.0"},
        )
        assert activated.status_code == 200
        payload = activated.json()
        assert payload["status"] == "active"
        assert payload["signedToken"]

        checked = client.post(
            "/api/license/check",
            json={
                "licenseKey": license_key,
                "deviceId": "device-1",
                "signedToken": payload["signedToken"],
                "appVersion": "1.0",
            },
        )
        assert checked.status_code == 200
        assert checked.json()["status"] == "active"

        blocked = client.post(
            "/api/license/activate",
            json={"licenseKey": license_key, "deviceId": "device-2", "appVersion": "1.0"},
        )
        assert blocked.status_code == 409

        cards = client.get("/api/admin/cards", headers=headers)
        assert cards.status_code == 200
        card_id = cards.json()[0]["id"]

        deleted = client.delete(f"/api/admin/cards/{card_id}", headers=headers)
        assert deleted.status_code == 200
        assert deleted.json()["ok"] is True

        after_delete = client.get("/api/admin/cards", headers=headers)
        assert after_delete.status_code == 200
        assert after_delete.json() == []

        deleted_activation = client.post(
            "/api/license/activate",
            json={"licenseKey": license_key, "deviceId": "device-1", "appVersion": "1.0"},
        )
        assert deleted_activation.status_code == 404

    from server.app.database import engine

    engine.dispose()


def test_permanent_license_activation_and_check(tmp_path: Path) -> None:
    os.environ["DINGPAY_DATABASE_URL"] = f"sqlite:///{tmp_path / 'test-permanent.db'}"
    os.environ["DINGPAY_SECRET_KEY"] = "test-secret"
    os.environ["DINGPAY_ADMIN_USERNAME"] = "admin"
    os.environ["DINGPAY_ADMIN_PASSWORD"] = "admin123"

    from fastapi.testclient import TestClient
    from server.app.main import app

    with TestClient(app) as client:
        login = client.post("/api/admin/login", json={"username": "admin", "password": "admin123"})
        assert login.status_code == 200
        token = login.json()["token"]
        headers = {"Authorization": f"Bearer {token}"}

        generated = client.post(
            "/api/admin/cards/generate",
            json={"count": 1, "durationDays": 0},
            headers=headers,
        )
        assert generated.status_code == 200
        license_key = generated.json()["licenseKeys"][0]

        activated = client.post(
            "/api/license/activate",
            json={"licenseKey": license_key, "deviceId": "device-1", "appVersion": "1.0"},
        )
        assert activated.status_code == 200
        payload = activated.json()
        assert payload["status"] == "active"
        assert payload["expiresAtMillis"] == 0
        assert payload["signedToken"]

        checked = client.post(
            "/api/license/check",
            json={
                "licenseKey": license_key,
                "deviceId": "device-1",
                "signedToken": payload["signedToken"],
                "appVersion": "1.0",
            },
        )
        assert checked.status_code == 200
        assert checked.json()["status"] == "active"
        assert checked.json()["expiresAtMillis"] == 0

        cards = client.get("/api/admin/cards", headers=headers)
        assert cards.status_code == 200
        [card] = cards.json()
        assert card["durationDays"] == 0
        assert card["status"] == "active"
        assert card["expiresAt"] is None

    from server.app.database import engine

    engine.dispose()


def test_admin_page_is_served(tmp_path: Path) -> None:
    os.environ["DINGPAY_DATABASE_URL"] = f"sqlite:///{tmp_path / 'test-admin.db'}"
    os.environ["DINGPAY_SECRET_KEY"] = "test-secret"
    os.environ["DINGPAY_ADMIN_USERNAME"] = "admin"
    os.environ["DINGPAY_ADMIN_PASSWORD"] = "admin123"

    from fastapi.testclient import TestClient
    from server.app.main import app

    with TestClient(app) as client:
        response = client.get("/admin")
        assert response.status_code == 200
        assert "DingPay 卡密后台" in response.text

    from server.app.database import engine

    engine.dispose()
