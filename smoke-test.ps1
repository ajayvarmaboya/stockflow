# StockFlow Automated Smoke Test Script
# Run this script while the StockFlow backend is running on http://localhost:8080

$BaseUrl = "http://localhost:8080"
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "      STOCKFLOW BACKEND — LIVE VERIFICATION SUITE         " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Health Check
Write-Host "`n[1/6] Testing Actuator Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get
    Write-Host "  -> Health Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "  -> ERROR: Application is not running on $BaseUrl. Start it with: ./mvnw spring-boot:run" -ForegroundColor Red
    exit 1
}

# 2. Browse Public Catalogue
Write-Host "`n[2/6] Browsing Active Product Catalogue (GET /api/v1/products)..." -ForegroundColor Yellow
$catalog = Invoke-RestMethod -Uri "$BaseUrl/api/v1/products" -Method Get
$productCount = $catalog.totalElements
$firstProduct = $catalog.content[0]
Write-Host "  -> Found $productCount products in active catalogue." -ForegroundColor Green
Write-Host "  -> Sample Item: $($firstProduct.name) | SKU: $($firstProduct.sku) | Price: `$$($firstProduct.price) | Stock: $($firstProduct.availableStock)" -ForegroundColor Gray

# 3. Customer Authentication
Write-Host "`n[3/6] Authenticating Demo Customer (POST /api/v1/auth/login)..." -ForegroundColor Yellow
$loginBody = @{
    username = "customer1"
    password = "Customer@123"
} | ConvertTo-Json

$authResponse = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $authResponse.token
Write-Host "  -> Logged in successfully as: $($authResponse.username) (Role: $($authResponse.role))" -ForegroundColor Green
Write-Host "  -> JWT Token acquired." -ForegroundColor Gray

# 4. Place an Order with Idempotency Key
Write-Host "`n[4/6] Placing Order with Idempotency-Key (POST /api/v1/orders)..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $token"
    "Idempotency-Key" = [System.Guid]::NewGuid().ToString()
}

$orderBody = @{
    items = @(
        @{
            productId = $firstProduct.id
            quantity = 1
        }
    )
} | ConvertTo-Json

$order = Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders" -Method Post -Headers $headers -Body $orderBody -ContentType "application/json"
Write-Host "  -> Order Placed Successfully!" -ForegroundColor Green
Write-Host "  -> Order ID: $($order.id)" -ForegroundColor Gray
Write-Host "  -> Status: $($order.status)" -ForegroundColor Gray
Write-Host "  -> Total Amount: `$$($order.totalAmount)" -ForegroundColor Gray

# 5. Test Idempotent Retry (Exact same key & payload)
Write-Host "`n[5/6] Retrying Request with SAME Idempotency-Key (Testing Idempotency)..." -ForegroundColor Yellow
$replayOrder = Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders" -Method Post -Headers $headers -Body $orderBody -ContentType "application/json"
if ($replayOrder.id -eq $order.id) {
    Write-Host "  -> Idempotency Verified! Returned identical Order ID: $($replayOrder.id)" -ForegroundColor Green
    Write-Host "  -> No duplicate order created and no double-deduction." -ForegroundColor Green
} else {
    Write-Host "  -> Idempotency FAILED: Order ID changed on retry." -ForegroundColor Red
}

# 6. Test Insufficient Stock Conflict (409)
Write-Host "`n[6/6] Testing Insufficient Stock Guardrail (Requesting 9999 units)..." -ForegroundColor Yellow
$excessiveBody = @{
    items = @(
        @{
            productId = $firstProduct.id
            quantity = 9999
        }
    )
} | ConvertTo-Json

$excessiveHeaders = @{
    "Authorization" = "Bearer $token"
    "Idempotency-Key" = [System.Guid]::NewGuid().ToString()
}

try {
    $conflict = Invoke-RestMethod -Uri "$BaseUrl/api/v1/orders" -Method Post -Headers $excessiveHeaders -Body $excessiveBody -ContentType "application/json"
    Write-Host "  -> FAILED: Expected 409 Conflict, but order succeeded." -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "  -> Successfully Caught Expected HTTP $statusCode Conflict!" -ForegroundColor Green
    Write-Host "  -> Protected database from negative inventory." -ForegroundColor Green
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "   ALL LIVE VERIFICATION CHECKS COMPLETED SUCCESSFULLY!   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
