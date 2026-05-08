# Payment System (3 Microservices)

Project ini terdiri dari 3 microservices Spring Boot:

- `order-service` (`8081`)
- `payment-service` (`8082`)
- `notification-service` (`8083`)

Infrastruktur:

- PostgreSQL (`5432`)
- ZooKeeper (`2181`)
- Kafka (`9092` host, `29092` internal docker network)

## Cara Pakai Microservices

## 1) Prasyarat

- Docker Desktop aktif
- Docker Compose v2

## 2) Jalankan aplikasi

Di root project:

```bash
docker compose up -d --build
```

Cek semua container:

```bash
docker ps
```

Container yang harus muncul:

- `payment-system-order-service-1`
- `payment-system-payment-service-1`
- `payment-system-notification-service-1`
- `payment-system-kafka-1`
- `payment-system-postgres-1`
- `payment-system-zookeeper-1`

## 3) Alur penggunaan normal

1. Buat order ke `order-service`.
2. `order-service` publish event `order-created`.
3. `payment-service` consume event, proses pembayaran, simpan payment.
4. Jika sukses, `payment-service` publish `payment-success`.
5. `notification-service` consume event dan kirim notifikasi (log).

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

---

## Pengetesan Kriteria

## Kriteria 1: Payment gateway callback bisa lebih dari satu kali

buat order misal :
 
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/payments?txId=TX-CB-1&amount=100"
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
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/payments?txId=TX-TIMEOUT-1&amount=500"
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

Panggil endpoint payment berkali-kali dengan txId sama:

```powershell
1..5 | ForEach-Object {
  Invoke-RestMethod -Method Post -Uri "http://localhost:8082/payments?txId=TX-IDEMP-1&amount=100"
}
```

Cek row count di DB:

```bash
docker exec -i payment-system-postgres-1 psql -U postgres -d paymentdb -c "select count(*) from payments where transaction_id='TX-IDEMP-1';"
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

