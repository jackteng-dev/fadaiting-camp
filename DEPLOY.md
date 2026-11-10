# 發呆亭露營區 Docker 部署

## Local dev

Infrastructure only:

```powershell
docker compose up -d
```

Frontend:

```powershell
cd D:\Project\fadaiting-camp\frontend
npm run dev
```

Backend:

```powershell
cd D:\Project\fadaiting-camp\backend
mvn -DskipTests package
java -jar target\fadaiting-camp-api-1.0-SNAPSHOT.jar
```

Default dev login:

```text
username: admin001
password: <seed 預設密碼：見 db/init，首次登入後立即更改>
```

## Full Docker stack

Build images:

```powershell
cd D:\Project\fadaiting-camp
docker compose -f docker-compose.prod.yml build
```

Start the full stack:

```powershell
docker compose -f docker-compose.prod.yml up -d
```

Open:

```text
http://localhost:8080/admin/login
```

Stop:

```powershell
docker compose -f docker-compose.prod.yml down
```

Remove production volumes and reset data:

```powershell
docker compose -f docker-compose.prod.yml down -v
```
