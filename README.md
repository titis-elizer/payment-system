# Payment System (3 Microservices)

This project consist of 3 microservices:

- `order-service` (`8081`)
- `payment-service` (`8082`)
- `notification-service` (`8083`)

Infrastructure:

- PostgreSQL (`5432`)
- ZooKeeper (`2181`)
- Kafka (`9092` host, `29092` internal docker network)

## Usage

## 1) Requirement

- Docker Desktop aktif
- Docker Compose v2

## 2) Run Application

In root project:

```bash
docker compose up -d --build
```

Check all containers:

```bash
docker ps
```

Container that needed to appear:

- `payment-system-order-service-1`
- `payment-system-payment-service-1`
- `payment-system-notification-service-1`
- `payment-system-kafka-1`
- `payment-system-postgres-1`
- `payment-system-zookeeper-1`

## 3) Alur penggunaan normal
1. Buat order ke `order-service`.
2. `order-service` publish event `order-created`.
3. `payment-service` consume event dan **membuat payment status `PENDING`** (belum charge).
4. Saat user siap bayar, panggil `POST /payments/confirm?txId=...`.
5. `payment-service` melakukan charge (dengan retry timeout), lalu update `SUCCESS`.
6. `payment-service` publish `payment-success`.
7. `notification-service` consume event, **buat notifikasi**, simpan ke tabel `notifications`, dan publish event `notification-sent`.

Contoh buat order:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/orders" `
  -ContentType "application/json" `
  -Body (([pscustomobject]@{customerId="C1";amount=100}) | ConvertTo-Json)
```

Pantau log:

```bash
docker compose logs -f payment-service notification-service
```

Cek notifikasi yang tersimpan:

```bash
docker exec -i payment-system-postgres-1 psql -U postgres -d paymentdb -c "select id, transaction_id, amount, sender_customer_id, local_date_time, channel, status from notifications order by id desc limit 10;"
```

Cek payment pending terbaru:

```bash
docker exec -i payment-system-postgres-1 psql -U postgres -d paymentdb -c "select id, transaction_id, order_id, customer_id, amount, status from payments order by id desc limit 5;"
```

Lakukan konfirmasi pembayaran (contoh order id = 1 -> txId = TX-1):

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/payments/confirm?txId=TX-1"
```

---

## Pengetesan Kriteria

## Kriteria 1: Payment gateway callback bisa lebih dari satu kali

Buat payment valid dulu (contoh txId dari order yang sudah ada):

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/payments/confirm?txId=TX-1"
```

Gunakan txId yang sama berkali-kali:

```powershell
1..5 | ForEach-Object {
  Invoke-RestMethod -Method Post -Uri "http://localhost:8082/payments/callback?txId=TX-CB-1"
}
```

Verifikasi data payment (tidak duplicate):

```bash
docker exec -i payment-system-postgres-1 psql -U postgres -d paymentdb -c "select transaction_id, status, count(*) from payments where transaction_id='TX-CB-1' group by transaction_id, status;"
```

Expected:

- status tetap konsisten (umumnya `SUCCESS`)
- jumlah row untuk txId yang sama tetap 1

## Kriteria 2: Network timeout sering terjadi

Di `payment-service`, timeout simulation default aktif:

- `PAYMENT_GATEWAY_TIMEOUT_SIMULATION_ENABLED=true`
- `PAYMENT_GATEWAY_TIMEOUT_SIMULATION_FAIL_ATTEMPTS=2`

Artinya untuk txId baru, 2 attempt pertama timeout, lalu retry berhasil.

Test command:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/payments/confirm?txId=TX-2"
```

Lihat bukti timeout + retry di log:

```bash
docker logs payment-system-payment-service-1 | findstr /i "Simulated gateway timeout Gateway charge success"
```

Expected:

- ada log `Simulated gateway timeout ... attempt=1`
- ada log `Simulated gateway timeout ... attempt=2`
- lalu `Gateway charge success ...`

## Kriteria 3: Sistem tidak boleh double charge

Panggil endpoint confirm berkali-kali dengan txId sama:

```powershell
1..5 | ForEach-Object {
  Invoke-RestMethod -Method Post -Uri "http://localhost:8082/payments/confirm?txId=TX-3"
}
```

Cek row count di DB (tetap 1 row untuk txId itu):

```bash
docker exec -i payment-system-postgres-1 psql -U postgres -d paymentdb -c "select count(*) from payments where transaction_id='TX-3';"
```

Expected:

- hasil `count = 1`

---

## Cleanup

Stop:

```bash
docker compose down
```

Stop + hapus volume (reset data):

```bash
docker compose down -v
```

