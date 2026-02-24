

param(
    [string]$Ticker,
    [string]$Interval = "OneMinute",
    [int]$Count = 300
)


$instrumentId = http GET "https://public-api.etoro.com/api/v1/market-data/search?internalSymbolFull=$Ticker" "X-Api-Key:$env:ETORO_PUBLIC_KEY" "X-User-Key:$env:ETORO_API_KEY" "X-Request-Id:$(New-Guid)" | jq '.items[0].internalInstrumentId'



http GET "https://public-api.etoro.com/api/v1/market-data/instruments/$instrumentId/history/candles/ASC/$Interval/$COUNT"  "X-Api-Key:$env:ETORO_PUBLIC_KEY" "X-User-Key:$env:ETORO_API_KEY" "X-Request-Id:$(New-Guid)"